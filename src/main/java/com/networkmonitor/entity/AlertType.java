package com.networkmonitor.entity;

/**
 * 告警类型。
 */
public enum AlertType {

    DEVICE_OFFLINE("设备离线"),
    HIGH_LATENCY("响应延迟");

    private final String displayName;

    AlertType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
