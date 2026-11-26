package com.networkmonitor.entity;

/**
 * 告警级别。
 */
public enum AlertLevel {

    INFO("提示", "text-bg-info"),
    WARNING("警告", "text-bg-warning"),
    CRITICAL("严重", "text-bg-danger");

    private final String displayName;
    private final String cssClass;

    AlertLevel(String displayName, String cssClass) {
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
