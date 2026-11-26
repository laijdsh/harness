package com.networkmonitor.monitor;

import com.networkmonitor.entity.NetworkDevice;
import com.networkmonitor.entity.SnmpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SNMP 采集边界测试。
 *
 * <p>这里不触发真实网络请求，只验证未启用和缺少社区字符串的短路逻辑。
 */
class SnmpServiceTest {

    private SnmpService snmpService;

    @BeforeEach
    void setUp() {
        snmpService = new SnmpService(500, 0);
    }

    @Test
    @DisplayName("未启用 SNMP 时直接返回 DISABLED")
    void shouldReturnDisabledWhenSnmpIsOff() {
        NetworkDevice device = new NetworkDevice();
        device.setName("测试设备");
        device.setIpAddress("127.0.0.1");
        device.setSnmpEnabled(false);

        SnmpResult result = snmpService.poll(device);

        assertEquals(SnmpStatus.DISABLED, result.getStatus());
        assertTrue(result.getMessage().contains("未启用"));
    }

    @Test
    @DisplayName("启用 SNMP 但缺少社区字符串时返回 ERROR")
    void shouldReturnErrorWhenCommunityIsMissing() {
        NetworkDevice device = new NetworkDevice();
        device.setName("测试设备");
        device.setIpAddress("127.0.0.1");
        device.setSnmpEnabled(true);
        device.setSnmpPort(1161);
        device.setSnmpCommunity(null);

        SnmpResult result = snmpService.poll(device);

        assertEquals(SnmpStatus.ERROR, result.getStatus());
        assertTrue(result.getMessage().contains("未配置"));
    }
}
