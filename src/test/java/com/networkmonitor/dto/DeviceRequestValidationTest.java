package com.networkmonitor.dto;

import com.networkmonitor.entity.DeviceType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DeviceRequest 参数校验测试。
 */
class DeviceRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    @Test
    @DisplayName("合法设备请求没有校验错误")
    void shouldAcceptValidRequest() {
        DeviceRequest request = validRequest();
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    @DisplayName("非法 IPv4 地址应校验失败")
    void shouldRejectInvalidIpv4() {
        DeviceRequest request = validRequest();
        request.setIpAddress("192.168.1.999");

        Set<ConstraintViolation<DeviceRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("ipAddress")));
    }

    @Test
    @DisplayName("SNMP 端口超出 1~65535 应校验失败")
    void shouldRejectInvalidSnmpPort() {
        DeviceRequest request = validRequest();
        request.setSnmpEnabled(true);
        request.setSnmpPort(70000);

        Set<ConstraintViolation<DeviceRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("snmpPort")));
    }

    @Test
    @DisplayName("SNMP 社区字符串过长应校验失败")
    void shouldRejectTooLongCommunity() {
        DeviceRequest request = validRequest();
        request.setSnmpEnabled(true);
        request.setSnmpPort(161);
        request.setSnmpCommunity("x".repeat(101));

        Set<ConstraintViolation<DeviceRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("snmpCommunity")));
    }

    private DeviceRequest validRequest() {
        DeviceRequest request = new DeviceRequest();
        request.setName("测试设备");
        request.setIpAddress("192.168.1.1");
        request.setDeviceType(DeviceType.SWITCH);
        request.setArea("测试区");
        request.setDescription("阶段7校验测试");
        request.setSnmpPort(161);
        return request;
    }
}
