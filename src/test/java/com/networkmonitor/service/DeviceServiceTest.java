package com.networkmonitor.service;

import com.networkmonitor.dto.DeviceRequest;
import com.networkmonitor.entity.DeviceType;
import com.networkmonitor.entity.NetworkDevice;
import com.networkmonitor.repository.NetworkDeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DeviceService 单元测试，重点验证 SNMP 配置校验和编辑语义。
 */
@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock
    private NetworkDeviceRepository deviceRepository;

    private DeviceService deviceService;

    @BeforeEach
    void setUp() {
        deviceService = new DeviceService(deviceRepository);
    }

    @Test
    @DisplayName("新增启用 SNMP 但未填社区字符串时拒绝保存")
    void shouldRejectCreateWithSnmpButNoCommunity() {
        DeviceRequest request = request();
        request.setSnmpEnabled(true);
        request.setSnmpPort(1161);
        request.setSnmpCommunity(null);

        assertThrows(IllegalArgumentException.class, () -> deviceService.create(request));
        verify(deviceRepository, never()).save(any(NetworkDevice.class));
    }

    @Test
    @DisplayName("新增启用 SNMP 且填写社区字符串时保存成功")
    void shouldCreateWithSnmpCommunity() {
        DeviceRequest request = request();
        request.setSnmpEnabled(true);
        request.setSnmpPort(1161);
        request.setSnmpCommunity("sim-public");
        when(deviceRepository.save(any(NetworkDevice.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NetworkDevice saved = deviceService.create(request);

        assertTrue(saved.isSnmpEnabled());
        assertEquals(1161, saved.getSnmpPort());
        assertEquals("sim-public", saved.getSnmpCommunity());
        assertEquals("2c", saved.getSnmpVersion());
    }

    @Test
    @DisplayName("编辑时留空社区字符串应保留原值")
    void shouldKeepCommunityWhenEditLeavesItBlank() {
        NetworkDevice existing = new NetworkDevice();
        existing.setId(3L);
        existing.setName("旧名称");
        existing.setIpAddress("127.0.0.1");
        existing.setDeviceType(DeviceType.SERVER);
        existing.setSnmpEnabled(true);
        existing.setSnmpPort(1161);
        existing.setSnmpCommunity("sim-public");
        when(deviceRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(deviceRepository.existsByIpAddressAndIdNot("127.0.0.1", 3L)).thenReturn(false);
        when(deviceRepository.save(any(NetworkDevice.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DeviceRequest request = request();
        request.setName("新名称");
        request.setSnmpEnabled(true);
        request.setSnmpPort(1161);
        request.setSnmpCommunity("   ");

        NetworkDevice saved = deviceService.update(3L, request);

        assertEquals("新名称", saved.getName());
        assertEquals("sim-public", saved.getSnmpCommunity());
        assertTrue(saved.isSnmpEnabled());
    }

    @Test
    @DisplayName("关闭 SNMP 时状态回到 DISABLED")
    void shouldDisableSnmp() {
        DeviceRequest request = request();
        request.setSnmpEnabled(false);
        request.setSnmpPort(161);
        when(deviceRepository.save(any(NetworkDevice.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NetworkDevice saved = deviceService.create(request);

        assertEquals(com.networkmonitor.entity.SnmpStatus.DISABLED, saved.getSnmpStatus());
    }

    private DeviceRequest request() {
        DeviceRequest request = new DeviceRequest();
        request.setName("测试设备");
        request.setIpAddress("127.0.0.1");
        request.setDeviceType(DeviceType.SERVER);
        request.setArea("测试区");
        request.setDescription("阶段7测试");
        return request;
    }
}
