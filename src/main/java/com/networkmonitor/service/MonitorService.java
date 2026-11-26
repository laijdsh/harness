package com.networkmonitor.service;

import com.networkmonitor.dto.CheckAllResponse;
import com.networkmonitor.dto.MonitorRecordResponse;
import com.networkmonitor.entity.DeviceStatus;
import com.networkmonitor.entity.MonitorRecord;
import com.networkmonitor.entity.NetworkDevice;
import com.networkmonitor.exception.DeviceNotFoundException;
import com.networkmonitor.exception.MonitorRecordNotFoundException;
import com.networkmonitor.monitor.PingMonitor;
import com.networkmonitor.monitor.PingResult;
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
import java.util.ArrayList;
import java.util.List;

/**
 * 设备检测业务服务。
 *
 * <p>调用 PingMonitor 获取网络探测结果，然后：
 * <ol>
 *     <li>更新设备表上的最近检测摘要；</li>
 *     <li>向 monitor_record 表写入一条历史记录；</li>
 *     <li>返回适合页面/API 展示的 DTO。</li>
 * </ol>
 *
 * <p>注意：本阶段只根据单次检测结果设置状态。连续失败阈值、告警和
 * 告警去重属于阶段 4 的 Alert 层。
 */
@Service
public class MonitorService {

    private static final Logger log = LoggerFactory.getLogger(MonitorService.class);
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 10;

    private final NetworkDeviceRepository deviceRepository;
    private final MonitorRecordRepository recordRepository;
    private final PingMonitor pingMonitor;
    private final int timeoutMs;

    public MonitorService(NetworkDeviceRepository deviceRepository,
                          MonitorRecordRepository recordRepository,
                          PingMonitor pingMonitor,
                          @Value("${monitor.timeout-ms:2000}") int timeoutMs) {
        this.deviceRepository = deviceRepository;
        this.recordRepository = recordRepository;
        this.pingMonitor = pingMonitor;
        this.timeoutMs = timeoutMs;
    }

    /**
     * 检测单台设备并保存结果。
     */
    @Transactional
    public MonitorRecordResponse checkDevice(Long deviceId) {
        NetworkDevice device = getDevice(deviceId);
        return checkAndPersist(device);
    }

    /**
     * 检测全部设备并返回汇总。
     *
     * <p>本阶段使用顺序检测，避免同时启动大量 ping 进程。
     * 单个设备检测失败会记录为 ERROR，不会中断其他设备。
     */
    @Transactional
    public CheckAllResponse checkAll() {
        List<NetworkDevice> devices = deviceRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        List<MonitorRecordResponse> results = new ArrayList<>();
        int online = 0;
        int offline = 0;
        int error = 0;

        for (NetworkDevice device : devices) {
            MonitorRecordResponse result = checkAndPersist(device);
            results.add(result);

            if (result.getStatus() == DeviceStatus.ONLINE) {
                online++;
            } else if (result.getStatus() == DeviceStatus.OFFLINE) {
                offline++;
            } else {
                error++;
            }
        }

        CheckAllResponse summary = new CheckAllResponse(
                devices.size(),
                online,
                offline,
                error,
                LocalDateTime.now(),
                results
        );
        log.info("全部设备检测完成：总数={}，在线={}，离线={}，异常={}",
                summary.getTotal(), summary.getOnline(), summary.getOffline(), summary.getError());
        return summary;
    }

    /**
     * 查询最近一条记录，不存在时抛出 404 业务异常。
     */
    @Transactional(readOnly = true)
    public MonitorRecordResponse getLatest(Long deviceId) {
        NetworkDevice device = getDevice(deviceId);
        MonitorRecord record = recordRepository
                .findTopByDeviceIdOrderByCheckTimeDesc(deviceId)
                .orElseThrow(() -> new MonitorRecordNotFoundException(deviceId));
        return MonitorRecordResponse.from(record, device.getName());
    }

    /**
     * 查询最近一条记录，不存在时返回 null，供详情页展示使用。
     */
    @Transactional(readOnly = true)
    public MonitorRecordResponse getLatestOrNull(Long deviceId) {
        NetworkDevice device = getDevice(deviceId);
        return recordRepository.findTopByDeviceIdOrderByCheckTimeDesc(deviceId)
                .map(record -> MonitorRecordResponse.from(record, device.getName()))
                .orElse(null);
    }

    /**
     * 查询最近 N 条记录，供设备详情页显示。
     */
    @Transactional(readOnly = true)
    public List<MonitorRecordResponse> getRecentRecords(Long deviceId, int limit) {
        NetworkDevice device = getDevice(deviceId);
        int safeLimit = Math.max(1, Math.min(limit, MAX_PAGE_SIZE));
        Pageable pageable = PageRequest.of(0, safeLimit,
                Sort.by(Sort.Direction.DESC, "checkTime"));
        return recordRepository.findByDeviceIdOrderByCheckTimeDesc(deviceId, pageable)
                .map(record -> MonitorRecordResponse.from(record, device.getName()))
                .getContent();
    }

    /**
     * 分页查询设备历史记录。
     */
    @Transactional(readOnly = true)
    public Page<MonitorRecordResponse> getRecords(Long deviceId, int page, int size) {
        NetworkDevice device = getDevice(deviceId);
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Direction.DESC, "checkTime"));
        return recordRepository.findByDeviceIdOrderByCheckTimeDesc(deviceId, pageable)
                .map(record -> MonitorRecordResponse.from(record, device.getName()));
    }

    private MonitorRecordResponse checkAndPersist(NetworkDevice device) {
        PingResult pingResult;
        try {
            pingResult = pingMonitor.ping(device.getIpAddress(), timeoutMs);
        } catch (Exception ex) {
            // PingMonitor 已经处理常见异常，这里作为最后兜底。
            log.error("检测设备 {} 时发生未预期异常", device.getName(), ex);
            pingResult = PingResult.error("检测程序异常：" + ex.getMessage());
        }

        LocalDateTime checkTime = LocalDateTime.now();

        device.setStatus(pingResult.getStatus());
        device.setLastCheckedAt(checkTime);
        device.setLastResponseTimeMs(pingResult.getResponseTimeMs());
        deviceRepository.save(device);

        MonitorRecord record = new MonitorRecord();
        record.setDeviceId(device.getId());
        record.setCheckTime(checkTime);
        record.setReachable(pingResult.isReachable());
        record.setResponseTimeMs(pingResult.getResponseTimeMs());
        record.setStatus(pingResult.getStatus());
        record.setResultMessage(truncate(pingResult.getMessage(), 255));
        MonitorRecord saved = recordRepository.save(record);

        log.info("检测设备 [{}] {} -> {}，响应时间={}ms",
                device.getName(), device.getIpAddress(), pingResult.getStatus(),
                pingResult.getResponseTimeMs());

        return MonitorRecordResponse.from(saved, device.getName());
    }

    private NetworkDevice getDevice(Long deviceId) {
        return deviceRepository.findById(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException(deviceId));
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
