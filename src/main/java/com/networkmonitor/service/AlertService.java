package com.networkmonitor.service;

import com.networkmonitor.dto.AlertResponse;
import com.networkmonitor.entity.AlertLevel;
import com.networkmonitor.entity.AlertRecord;
import com.networkmonitor.entity.AlertStatus;
import com.networkmonitor.entity.AlertType;
import com.networkmonitor.entity.DeviceStatus;
import com.networkmonitor.entity.MonitorRecord;
import com.networkmonitor.entity.NetworkDevice;
import com.networkmonitor.exception.AlertNotFoundException;
import com.networkmonitor.exception.InvalidAlertStateException;
import com.networkmonitor.repository.AlertRecordRepository;
import com.networkmonitor.repository.MonitorRecordRepository;
import com.networkmonitor.repository.NetworkDeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 异常判断与告警业务服务。
 *
 * <p>告警规则：
 * <ol>
 *     <li>连续 N 次 OFFLINE，生成 CRITICAL 离线告警；</li>
 *     <li>ONLINE 且响应时间超过阈值，生成 WARNING 延迟告警；</li>
 *     <li>同一设备同一类型只保留一个活动告警，持续异常只增加次数；</li>
 *     <li>恢复时更新原告警状态为 RECOVERED，不重复生成恢复记录；</li>
 *     <li>支持管理员确认活动告警，确认后仍可被恢复。</li>
 * </ol>
 *
 * <p>告警判断在检测结果保存后执行，和 MonitorService 在同一个事务中。
 */
@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final List<AlertStatus> ACTIVE_STATUSES =
            List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED);

    private final AlertRecordRepository alertRepository;
    private final MonitorRecordRepository monitorRecordRepository;
    private final NetworkDeviceRepository deviceRepository;
    private final int consecutiveFailureThreshold;
    private final int responseTimeThresholdMs;

    public AlertService(AlertRecordRepository alertRepository,
                        MonitorRecordRepository monitorRecordRepository,
                        NetworkDeviceRepository deviceRepository,
                        @Value("${alert.consecutive-failure-threshold:3}") int consecutiveFailureThreshold,
                        @Value("${alert.response-time-threshold-ms:100}") int responseTimeThresholdMs) {
        this.alertRepository = alertRepository;
        this.monitorRecordRepository = monitorRecordRepository;
        this.deviceRepository = deviceRepository;
        this.consecutiveFailureThreshold = Math.max(1, consecutiveFailureThreshold);
        this.responseTimeThresholdMs = Math.max(0, responseTimeThresholdMs);
    }

    /**
     * 在一条监测记录保存后评估是否需要创建、刷新或恢复告警。
     */
    @Transactional
    public void evaluate(NetworkDevice device, MonitorRecord currentRecord) {
        evaluateOffline(device, currentRecord);
        evaluateLatency(device, currentRecord);
    }

    /**
     * 分页查询告警列表。
     */
    @Transactional(readOnly = true)
    public Page<AlertResponse> search(Long deviceId,
                                      AlertStatus status,
                                      AlertLevel level,
                                      int page,
                                      int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Direction.DESC, "triggeredAt"));

        Page<AlertRecord> alerts = alertRepository.search(deviceId, status, level, pageable);
        Map<Long, String> deviceNames = loadDeviceNames(alerts.getContent());

        return alerts.map(alert -> AlertResponse.from(
                alert,
                deviceNames.getOrDefault(alert.getDeviceId(), "已删除设备")
        ));
    }

    /**
     * 查询单条告警。
     */
    @Transactional(readOnly = true)
    public AlertResponse get(Long alertId) {
        AlertRecord alert = getAlert(alertId);
        String deviceName = deviceRepository.findById(alert.getDeviceId())
                .map(NetworkDevice::getName)
                .orElse("已删除设备");
        return AlertResponse.from(alert, deviceName);
    }

    /**
     * 确认一条未恢复告警。
     */
    @Transactional
    public AlertResponse acknowledge(Long alertId) {
        AlertRecord alert = getAlert(alertId);
        if (alert.getStatus() == AlertStatus.RECOVERED) {
            throw new InvalidAlertStateException("告警已恢复，不能再次确认");
        }
        if (alert.getStatus() == AlertStatus.OPEN) {
            alert.setStatus(AlertStatus.ACKNOWLEDGED);
            alert.setAcknowledgedAt(LocalDateTime.now());
            alertRepository.save(alert);
            log.info("告警已确认：id={}，设备ID={}，类型={}", alertId, alert.getDeviceId(), alert.getAlertType());
        }

        String deviceName = deviceRepository.findById(alert.getDeviceId())
                .map(NetworkDevice::getName)
                .orElse("已删除设备");
        return AlertResponse.from(alert, deviceName);
    }

    /**
     * 当前未恢复（含已确认）的告警数量，供后续首页/看板使用。
     */
    @Transactional(readOnly = true)
    public long countActive() {
        return alertRepository.countByStatus(AlertStatus.OPEN)
                + alertRepository.countByStatus(AlertStatus.ACKNOWLEDGED);
    }

    private void evaluateOffline(NetworkDevice device, MonitorRecord currentRecord) {
        List<MonitorRecord> latestRecords = monitorRecordRepository
                .findByDeviceIdOrderByCheckTimeDesc(
                        device.getId(),
                        PageRequest.of(0, consecutiveFailureThreshold)
                )
                .getContent();

        boolean consecutiveFailure = latestRecords.size() >= consecutiveFailureThreshold
                && latestRecords.stream()
                .allMatch(record -> record.getStatus() == DeviceStatus.OFFLINE);

        Optional<AlertRecord> active = findActive(device.getId(), AlertType.DEVICE_OFFLINE);

        if (consecutiveFailure) {
            String message = "设备连续 " + consecutiveFailureThreshold + " 次 Ping 失败";
            if (active.isPresent()) {
                touch(active.get(), currentRecord, message);
            } else {
                create(device, AlertType.DEVICE_OFFLINE, AlertLevel.CRITICAL, message, currentRecord);
            }
        } else if (currentRecord.getStatus() == DeviceStatus.ONLINE) {
            active.ifPresent(alert -> recover(alert, currentRecord));
        }
        // ERROR 时无法判断是否恢复，保持现有告警状态。
    }

    private void evaluateLatency(NetworkDevice device, MonitorRecord currentRecord) {
        Integer responseTime = currentRecord.getResponseTimeMs();
        boolean highLatency = currentRecord.getStatus() == DeviceStatus.ONLINE
                && responseTime != null
                && responseTime > responseTimeThresholdMs;

        Optional<AlertRecord> active = findActive(device.getId(), AlertType.HIGH_LATENCY);

        if (highLatency) {
            String message = "响应时间 " + responseTime + " ms，超过阈值 "
                    + responseTimeThresholdMs + " ms";
            if (active.isPresent()) {
                touch(active.get(), currentRecord, message);
            } else {
                create(device, AlertType.HIGH_LATENCY, AlertLevel.WARNING, message, currentRecord);
            }
        } else if (currentRecord.getStatus() == DeviceStatus.ONLINE
                || currentRecord.getStatus() == DeviceStatus.OFFLINE) {
            // 在线且不超阈值，或设备已经离线，延迟告警都应该结束。
            active.ifPresent(alert -> recover(alert, currentRecord));
        }
    }

    private Optional<AlertRecord> findActive(Long deviceId, AlertType alertType) {
        return alertRepository.findFirstByDeviceIdAndAlertTypeAndStatusInOrderByTriggeredAtDesc(
                deviceId,
                alertType,
                ACTIVE_STATUSES
        );
    }

    private void create(NetworkDevice device,
                        AlertType type,
                        AlertLevel level,
                        String message,
                        MonitorRecord currentRecord) {
        LocalDateTime now = currentRecord.getCheckTime();
        AlertRecord alert = new AlertRecord();
        alert.setDeviceId(device.getId());
        alert.setAlertType(type);
        alert.setAlertLevel(level);
        alert.setMessage(truncate(message, 255));
        alert.setStatus(AlertStatus.OPEN);
        alert.setTriggeredAt(now);
        alert.setLastSeenAt(now);
        alert.setOccurrenceCount(1);
        alertRepository.save(alert);

        log.info("生成告警：设备=[{}]，类型={}，级别={}，内容={}",
                device.getName(), type, level, message);
    }

    private void touch(AlertRecord alert, MonitorRecord currentRecord, String message) {
        alert.setLastSeenAt(currentRecord.getCheckTime());
        alert.setOccurrenceCount(alert.getOccurrenceCount() + 1);
        alert.setMessage(truncate(message, 255));
        alertRepository.save(alert);
    }

    private void recover(AlertRecord alert, MonitorRecord currentRecord) {
        alert.setStatus(AlertStatus.RECOVERED);
        alert.setRecoveredAt(currentRecord.getCheckTime());
        alert.setLastSeenAt(currentRecord.getCheckTime());
        alertRepository.save(alert);

        log.info("告警恢复：id={}，设备ID={}，类型={}",
                alert.getId(), alert.getDeviceId(), alert.getAlertType());
    }

    private AlertRecord getAlert(Long alertId) {
        return alertRepository.findById(alertId)
                .orElseThrow(() -> new AlertNotFoundException(alertId));
    }

    private Map<Long, String> loadDeviceNames(List<AlertRecord> alerts) {
        Set<Long> deviceIds = alerts.stream()
                .map(AlertRecord::getDeviceId)
                .collect(Collectors.toSet());
        return deviceRepository.findAllById(deviceIds).stream()
                .collect(Collectors.toMap(NetworkDevice::getId, NetworkDevice::getName));
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
