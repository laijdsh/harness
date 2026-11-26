package com.networkmonitor.dto;

import com.networkmonitor.entity.DeviceStatus;
import com.networkmonitor.entity.DeviceType;
import com.networkmonitor.entity.NetworkDevice;
import com.networkmonitor.entity.SnmpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DeviceResponse 页面格式化字段测试。
 */
class DeviceResponseTest {

    @Test
    @DisplayName("SNMP 采集结果能格式化 sysUpTime 和状态")
    void shouldFormatSnmpFields() {
        NetworkDevice device = new NetworkDevice();
        device.setId(3L);
        device.setName("实验服务器");
        device.setIpAddress("127.0.0.1");
        device.setDeviceType(DeviceType.SERVER);
        device.setStatus(DeviceStatus.ONLINE);
        device.setSnmpEnabled(true);
        device.setSnmpVersion("2c");
        device.setSnmpPort(1161);
        device.setSnmpCommunity("sim-public");
        device.setSnmpStatus(SnmpStatus.OK);
        device.setSnmpSysName("simulated-core-switch");
        device.setSnmpSysDescr("Simulated SNMP v2c Agent for thesis testing");
        device.setSnmpSysUptimeTicks(123456L);
        device.setSnmpLastCheckedAt(LocalDateTime.of(2026, 11, 26, 20, 57, 50));

        DeviceResponse response = DeviceResponse.from(device);

        assertEquals("正常", response.getSnmpStatusText());
        assertEquals("00:20:34", response.getSnmpUptimeText());
        assertEquals("2026-11-26 20:57:50", response.getSnmpLastCheckedAtText());
        assertTrue(response.isSnmpCommunityConfigured());
        assertEquals("2c", response.getSnmpVersion());
        assertEquals(1161, response.getSnmpPort());
    }

    @Test
    @DisplayName("启用 SNMP 但未采集时页面显示未检测")
    void shouldShowNotCheckedYet() {
        NetworkDevice device = new NetworkDevice();
        device.setId(3L);
        device.setName("实验服务器");
        device.setIpAddress("127.0.0.1");
        device.setDeviceType(DeviceType.SERVER);
        device.setStatus(DeviceStatus.UNKNOWN);
        device.setSnmpEnabled(true);
        device.setSnmpStatus(SnmpStatus.DISABLED);

        DeviceResponse response = DeviceResponse.from(device);

        assertEquals("未检测", response.getSnmpStatusText());
        assertEquals("—", response.getSnmpLastCheckedAtText());
        assertFalse(response.isSnmpCommunityConfigured());
    }
}
