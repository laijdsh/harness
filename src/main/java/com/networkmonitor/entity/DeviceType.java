package com.networkmonitor.entity;

/**
 * 网络设备类型。
 *
 * <p>数据库中以字符串形式保存枚举名，例如 ROUTER、SWITCH，
 * 这样比保存数字更直观，也方便直接在 SQL 中查看。
 */
public enum DeviceType {

    ROUTER("路由器"),
    SWITCH("交换机"),
    SERVER("服务器"),
    OTHER("其他设备");

    private final String displayName;

    DeviceType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 页面展示用的中文名称。
     */
    public String getDisplayName() {
        return displayName;
    }
}
