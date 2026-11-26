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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 看板聚合服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private NetworkDeviceRepository deviceRepository;

    @Mock
    private MonitorRecordRepository monitorRecordRepository;

    @Mock
    private AlertRecordRepository alertRepository;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(
                deviceRepository,
                monitorRecordRepository,
                alertRepository
        );
    }

    @Test
    @DisplayName("汇总接口返回设备、告警和记录数量")
    void shouldBuildSummary() {
        when(deviceRepository.count()).thenReturn(4L);
        when(deviceRepository.countByStatus(DeviceStatus.ONLINE)).thenReturn(3L);
        when(deviceRepository.countByStatus(DeviceStatus.OFFLINE)).thenReturn(1L);
        when(deviceRepository.countByStatus(DeviceStatus.UNKNOWN)).thenReturn(0L);
        when(deviceRepository.countByStatus(DeviceStatus.ERROR)).thenReturn(0L);
        when(alertRepository.countByStatus(AlertStatus.OPEN)).thenReturn(0L);
        when(alertRepository.countByStatus(AlertStatus.ACKNOWLEDGED)).thenReturn(1L);
        when(alertRepository.count()).thenReturn(3L);
        when(monitorRecordRepository.count()).thenReturn(1329L);

        DashboardSummaryResponse summary = dashboardService.summary();

        assertEquals(4, summary.deviceTotal());
        assertEquals(3, summary.online());
        assertEquals(1, summary.offline());
        assertEquals(1, summary.activeAlerts());
        assertEquals(3, summary.totalAlerts());
        assertEquals(1329, summary.monitorRecords());
    }

    @Test
    @DisplayName("告警分布按状态、类型和级别统计")
    void shouldBuildAlertDistribution() {
        when(alertRepository.findAll()).thenReturn(List.of(
                alert(AlertType.DEVICE_OFFLINE, AlertLevel.CRITICAL, AlertStatus.ACKNOWLEDGED),
                alert(AlertType.HIGH_LATENCY, AlertLevel.WARNING, AlertStatus.RECOVERED),
                alert(AlertType.HIGH_LATENCY, AlertLevel.WARNING, AlertStatus.RECOVERED)
        ));

        AlertDistributionResponse distribution = dashboardService.alertDistribution();

        assertEquals(0, distribution.statusCounts().get("OPEN"));
        assertEquals(1, distribution.statusCounts().get("ACKNOWLEDGED"));
        assertEquals(2, distribution.statusCounts().get("RECOVERED"));
        assertEquals(1, distribution.typeCounts().get("DEVICE_OFFLINE"));
        assertEquals(2, distribution.typeCounts().get("HIGH_LATENCY"));
        assertEquals(1, distribution.levelCounts().get("CRITICAL"));
        assertEquals(2, distribution.levelCounts().get("WARNING"));
    }

    @Test
    @DisplayName("响应时间趋势包含在线记录平均值")
    void shouldBuildResponseTimeTrend() {
        MonitorRecord record = new MonitorRecord();
        record.setDeviceId(1L);
        record.setCheckTime(LocalDateTime.now().minusMinutes(5));
        record.setReachable(true);
        record.setResponseTimeMs(10);
        record.setStatus(DeviceStatus.ONLINE);
        record.setResultMessage("收到 ICMP 回复");
        when(monitorRecordRepository.findByCheckTimeGreaterThanEqualOrderByCheckTimeAsc(any(LocalDateTime.class)))
                .thenReturn(List.of(record));

        ResponseTimeTrendResponse trend = dashboardService.responseTimeTrend(24);

        assertEquals(24, trend.hours());
        assertEquals(30, trend.bucketMinutes());
        assertTrue(trend.labels().size() >= 1);
        assertTrue(trend.values().stream().anyMatch(value -> value != null && value == 10.0));
        assertEquals(1L, trend.counts().stream().mapToLong(Long::longValue).sum());
    }

    private AlertRecord alert(AlertType type, AlertLevel level, AlertStatus status) {
        AlertRecord alert = new AlertRecord();
        alert.setDeviceId(1L);
        alert.setAlertType(type);
        alert.setAlertLevel(level);
        alert.setStatus(status);
        alert.setMessage("测试告警");
        alert.setTriggeredAt(LocalDateTime.now());
        alert.setLastSeenAt(LocalDateTime.now());
        return alert;
    }
}
