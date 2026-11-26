package com.networkmonitor.service;

import com.networkmonitor.dto.AlertDistributionResponse;
import com.networkmonitor.dto.DashboardSummaryResponse;
import com.networkmonitor.dto.ResponseTimeTrendResponse;
import com.networkmonitor.entity.AlertLevel;
import com.networkmonitor.entity.AlertRecord;
import com.networkmonitor.entity.AlertStatus;
import com.networkmonitor.entity.AlertType;
import com.networkmonitor.entity.DeviceStatus;
import com.networkmonitor.entity.MonitorRecord;
import com.networkmonitor.repository.AlertRecordRepository;
import com.networkmonitor.repository.MonitorRecordRepository;
import com.networkmonitor.repository.NetworkDeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据看板业务服务。
 *
 * <p>当前阶段不新增数据表，直接聚合已有的设备、监测记录和告警数据。
 */
@Service
public class DashboardService {

    private static final DateTimeFormatter LABEL_FORMATTER =
            DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final NetworkDeviceRepository deviceRepository;
    private final MonitorRecordRepository monitorRecordRepository;
    private final AlertRecordRepository alertRepository;

    public DashboardService(NetworkDeviceRepository deviceRepository,
                            MonitorRecordRepository monitorRecordRepository,
                            AlertRecordRepository alertRepository) {
        this.deviceRepository = deviceRepository;
        this.monitorRecordRepository = monitorRecordRepository;
        this.alertRepository = alertRepository;
    }

    /**
     * 看板顶部汇总。
     */
    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary() {
        long activeAlerts = alertRepository.countByStatus(AlertStatus.OPEN)
                + alertRepository.countByStatus(AlertStatus.ACKNOWLEDGED);

        return new DashboardSummaryResponse(
                deviceRepository.count(),
                deviceRepository.countByStatus(DeviceStatus.ONLINE),
                deviceRepository.countByStatus(DeviceStatus.OFFLINE),
                deviceRepository.countByStatus(DeviceStatus.UNKNOWN),
                deviceRepository.countByStatus(DeviceStatus.ERROR),
                activeAlerts,
                alertRepository.count(),
                monitorRecordRepository.count(),
                LocalDateTime.now()
        );
    }

    /**
     * 最近 N 小时的平均响应时间趋势。
     *
     * <p>按时间桶聚合，只统计状态为 ONLINE 且响应时间非空的记录。
     * 没有数据的桶返回 null，避免图表把“没有检测”误画成 0ms。
     */
    @Transactional(readOnly = true)
    public ResponseTimeTrendResponse responseTimeTrend(int hours) {
        int safeHours = Math.max(1, Math.min(hours, 168));
        int bucketMinutes = safeHours <= 6 ? 10 : (safeHours <= 24 ? 30 : 60);

        LocalDateTime end = LocalDateTime.now().withSecond(0).withNano(0);
        LocalDateTime rawStart = end.minusHours(safeHours);
        LocalDateTime firstBucket = bucketStart(rawStart, bucketMinutes);

        List<MonitorRecord> records = monitorRecordRepository
                .findByCheckTimeGreaterThanEqualOrderByCheckTimeAsc(firstBucket);

        Map<LocalDateTime, List<MonitorRecord>> grouped = records.stream()
                .filter(record -> record.getStatus() == DeviceStatus.ONLINE)
                .filter(record -> record.getResponseTimeMs() != null)
                .collect(Collectors.groupingBy(
                        record -> bucketStart(record.getCheckTime(), bucketMinutes)
                ));

        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        List<Long> counts = new ArrayList<>();

        LocalDateTime cursor = firstBucket;
        while (!cursor.isAfter(end)) {
            List<MonitorRecord> bucketRecords = grouped.get(cursor);
            labels.add(cursor.format(LABEL_FORMATTER));

            if (bucketRecords == null || bucketRecords.isEmpty()) {
                values.add(null);
                counts.add(0L);
            } else {
                double average = bucketRecords.stream()
                        .mapToInt(MonitorRecord::getResponseTimeMs)
                        .average()
                        .orElse(0);
                values.add(Math.round(average * 10.0) / 10.0);
                counts.add((long) bucketRecords.size());
            }

            cursor = cursor.plusMinutes(bucketMinutes);
        }

        return new ResponseTimeTrendResponse(safeHours, bucketMinutes, labels, values, counts);
    }

    /**
     * 告警状态、类型、级别分布。
     */
    @Transactional(readOnly = true)
    public AlertDistributionResponse alertDistribution() {
        List<AlertRecord> alerts = alertRepository.findAll();

        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (AlertStatus status : AlertStatus.values()) {
            statusCounts.put(status.name(), alerts.stream()
                    .filter(alert -> alert.getStatus() == status)
                    .count());
        }

        Map<String, Long> typeCounts = new LinkedHashMap<>();
        for (AlertType type : AlertType.values()) {
            typeCounts.put(type.name(), alerts.stream()
                    .filter(alert -> alert.getAlertType() == type)
                    .count());
        }

        Map<String, Long> levelCounts = new LinkedHashMap<>();
        for (AlertLevel level : AlertLevel.values()) {
            levelCounts.put(level.name(), alerts.stream()
                    .filter(alert -> alert.getAlertLevel() == level)
                    .count());
        }

        return new AlertDistributionResponse(statusCounts, typeCounts, levelCounts);
    }

    private LocalDateTime bucketStart(LocalDateTime time, int bucketMinutes) {
        int minute = (time.getMinute() / bucketMinutes) * bucketMinutes;
        return time.withMinute(minute).withSecond(0).withNano(0);
    }
}
