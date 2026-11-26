package com.networkmonitor.service;

import com.networkmonitor.entity.AlertLevel;
import com.networkmonitor.entity.AlertRecord;
import com.networkmonitor.entity.AlertStatus;
import com.networkmonitor.entity.AlertType;
import com.networkmonitor.entity.DeviceStatus;
import com.networkmonitor.entity.MonitorRecord;
import com.networkmonitor.entity.NetworkDevice;
import com.networkmonitor.repository.AlertRecordRepository;
import com.networkmonitor.repository.MonitorRecordRepository;
import com.networkmonitor.repository.NetworkDeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 告警规则单元测试。
 *
 * <p>不连接数据库，用 Mockito 模拟监测记录和告警仓库，验证：
 * 连续失败阈值、告警去重、延迟告警和告警恢复。
 */
@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    private static final Long DEVICE_ID = 1L;

    @Mock
    private AlertRecordRepository alertRepository;

    @Mock
    private MonitorRecordRepository monitorRecordRepository;

    @Mock
    private NetworkDeviceRepository deviceRepository;

    private AlertService alertService;

    @BeforeEach
    void setUp() {
        alertService = new AlertService(
                alertRepository,
                monitorRecordRepository,
                deviceRepository,
                3,
                100
        );
    }

    @Test
    @DisplayName("连续 3 次离线生成 CRITICAL 告警")
    void shouldCreateOfflineAlertAfterThreeFailures() {
        NetworkDevice device = device();
        MonitorRecord current = record(DeviceStatus.OFFLINE, null, LocalDateTime.now());
        stubLatest(current, current, current);
        stubActive(null, null);
        when(alertRepository.save(any(AlertRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        alertService.evaluate(device, current);

        ArgumentCaptor<AlertRecord> captor = ArgumentCaptor.forClass(AlertRecord.class);
        verify(alertRepository, times(1)).save(captor.capture());
        AlertRecord saved = captor.getValue();
        assertEquals(AlertType.DEVICE_OFFLINE, saved.getAlertType());
        assertEquals(AlertLevel.CRITICAL, saved.getAlertLevel());
        assertEquals(AlertStatus.OPEN, saved.getStatus());
        assertEquals(1, saved.getOccurrenceCount());
    }

    @Test
    @DisplayName("连续失败不足 3 次不生成告警")
    void shouldNotCreateOfflineAlertBelowThreshold() {
        NetworkDevice device = device();
        MonitorRecord current = record(DeviceStatus.OFFLINE, null, LocalDateTime.now());
        stubLatest(current, current);
        stubActive(null, null);

        alertService.evaluate(device, current);

        verify(alertRepository, never()).save(any(AlertRecord.class));
    }

    @Test
    @DisplayName("持续离线只增加次数，不重复插入")
    void shouldDeduplicateActiveOfflineAlert() {
        NetworkDevice device = device();
        MonitorRecord current = record(DeviceStatus.OFFLINE, null, LocalDateTime.now());
        stubLatest(current, current, current);

        AlertRecord active = alert(AlertType.DEVICE_OFFLINE, AlertStatus.OPEN, 1);
        stubActive(active, null);
        when(alertRepository.save(any(AlertRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        alertService.evaluate(device, current);

        assertEquals(2, active.getOccurrenceCount());
        verify(alertRepository, times(1)).save(active);
    }

    @Test
    @DisplayName("设备恢复在线后离线告警变为 RECOVERED")
    void shouldRecoverOfflineAlertWhenOnline() {
        NetworkDevice device = device();
        MonitorRecord current = record(DeviceStatus.ONLINE, 5, LocalDateTime.now());
        stubLatest(current, current, current);

        AlertRecord active = alert(AlertType.DEVICE_OFFLINE, AlertStatus.OPEN, 3);
        stubActive(active, null);
        when(alertRepository.save(any(AlertRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        alertService.evaluate(device, current);

        assertEquals(AlertStatus.RECOVERED, active.getStatus());
        assertNotNull(active.getRecoveredAt());
    }

    @Test
    @DisplayName("响应时间超过阈值生成 WARNING 延迟告警")
    void shouldCreateHighLatencyAlert() {
        NetworkDevice device = device();
        MonitorRecord current = record(DeviceStatus.ONLINE, 150, LocalDateTime.now());
        stubLatest(current, current, current);
        stubActive(null, null);
        when(alertRepository.save(any(AlertRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        alertService.evaluate(device, current);

        ArgumentCaptor<AlertRecord> captor = ArgumentCaptor.forClass(AlertRecord.class);
        verify(alertRepository, times(1)).save(captor.capture());
        assertEquals(AlertType.HIGH_LATENCY, captor.getValue().getAlertType());
        assertEquals(AlertLevel.WARNING, captor.getValue().getAlertLevel());
    }

    @Test
    @DisplayName("响应时间恢复正常后延迟告警变为 RECOVERED")
    void shouldRecoverHighLatencyAlert() {
        NetworkDevice device = device();
        MonitorRecord current = record(DeviceStatus.ONLINE, 10, LocalDateTime.now());
        stubLatest(current, current, current);

        AlertRecord active = alert(AlertType.HIGH_LATENCY, AlertStatus.OPEN, 2);
        stubActive(null, active);
        when(alertRepository.save(any(AlertRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        alertService.evaluate(device, current);

        assertEquals(AlertStatus.RECOVERED, active.getStatus());
        assertNotNull(active.getRecoveredAt());
    }

    private NetworkDevice device() {
        NetworkDevice device = new NetworkDevice();
        device.setId(DEVICE_ID);
        device.setName("测试设备");
        device.setIpAddress("127.0.0.1");
        return device;
    }

    private MonitorRecord record(DeviceStatus status, Integer responseTime, LocalDateTime time) {
        MonitorRecord record = new MonitorRecord();
        record.setDeviceId(DEVICE_ID);
        record.setCheckTime(time);
        record.setReachable(status == DeviceStatus.ONLINE);
        record.setResponseTimeMs(responseTime);
        record.setStatus(status);
        record.setResultMessage(status == DeviceStatus.ONLINE ? "收到 ICMP 回复" : "请求超时");
        return record;
    }

    private AlertRecord alert(AlertType type, AlertStatus status, int occurrenceCount) {
        AlertRecord alert = new AlertRecord();
        alert.setDeviceId(DEVICE_ID);
        alert.setAlertType(type);
        alert.setAlertLevel(type == AlertType.DEVICE_OFFLINE ? AlertLevel.CRITICAL : AlertLevel.WARNING);
        alert.setMessage("测试告警");
        alert.setStatus(status);
        alert.setTriggeredAt(LocalDateTime.now().minusMinutes(10));
        alert.setLastSeenAt(LocalDateTime.now().minusMinutes(1));
        alert.setOccurrenceCount(occurrenceCount);
        return alert;
    }

    private void stubLatest(MonitorRecord... records) {
        List<MonitorRecord> list = new ArrayList<>(List.of(records));
        when(monitorRecordRepository.findByDeviceIdOrderByCheckTimeDesc(
                eq(DEVICE_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(list));
    }

    private void stubActive(AlertRecord offline, AlertRecord latency) {
        when(alertRepository.findFirstByDeviceIdAndAlertTypeAndStatusInOrderByTriggeredAtDesc(
                anyLong(), any(AlertType.class), anyCollection()))
                .thenAnswer(invocation -> {
                    AlertType type = invocation.getArgument(1);
                    return type == AlertType.DEVICE_OFFLINE
                            ? Optional.ofNullable(offline)
                            : Optional.ofNullable(latency);
                });
    }
}
