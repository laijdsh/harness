package com.networkmonitor.entity;

/**
 * 告警状态。
 *
 * <p>OPEN：未恢复；ACKNOWLEDGED：管理员已确认但异常仍在；RECOVERED：已恢复。
 */
public enum AlertStatus {

    OPEN("未恢复", "text-bg-danger"),
    ACKNOWLEDGED("已确认", "text-bg-warning"),
    RECOVERED("已恢复", "text-bg-success");

    private final String displayName;
    private final String cssClass;

    AlertStatus(String displayName, String cssClass) {
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
