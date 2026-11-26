package com.networkmonitor.entity;

/**
 * SNMP 采集状态。
 */
public enum SnmpStatus {

    DISABLED("未启用", "text-bg-secondary"),
    OK("正常", "text-bg-success"),
    UNREACHABLE("不可达", "text-bg-danger"),
    ERROR("采集异常", "text-bg-warning");

    private final String displayName;
    private final String cssClass;

    SnmpStatus(String displayName, String cssClass) {
        this.displayName = displayName;
        this.cssClass = cssClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCssClass() {
        return cssClass;
    }
}
