package com.networkmonitor.entity;

/**
 * 设备监测状态。
 *
 * <p>阶段 2 还没有真正的 Ping 检测，新设备默认是 UNKNOWN。
 * 阶段 3 实现网络探测后，会由监测服务更新这些状态。
 */
public enum DeviceStatus {

    ONLINE("在线", "text-bg-success"),
    OFFLINE("离线", "text-bg-danger"),
    UNKNOWN("未知", "text-bg-secondary"),
    ERROR("检测异常", "text-bg-warning");

    private final String displayName;
    private final String cssClass;

    DeviceStatus(String displayName, String cssClass) {
        this.displayName = displayName;
        this.cssClass = cssClass;
    }

    /**
     * 页面展示用的中文名称。
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Bootstrap 徽章样式类，例如 text-bg-success。
     */
    public String getCssClass() {
        return cssClass;
    }
}
