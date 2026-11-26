package com.networkmonitor.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.networkmonitor.entity.DeviceStatus;
import com.networkmonitor.entity.DeviceType;
import com.networkmonitor.entity.NetworkDevice;
import com.networkmonitor.entity.SnmpStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 返回给页面或 REST API 的设备对象。
 *
 * <p>不直接把 JPA 实体返回给外部，避免实体中的持久化细节暴露出去。
 */
public class DeviceResponse {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Long id;
    private final String name;
    private final String ipAddress;
    private final DeviceType deviceType;
    private final String area;
    private final String description;
    private final DeviceStatus status;
    private final LocalDateTime lastCheckedAt;
    private final Integer lastResponseTimeMs;
    private final boolean snmpEnabled;
    private final String snmpVersion;
    private final int snmpPort;
    private final boolean snmpCommunityConfigured;
    private final SnmpStatus snmpStatus;
    private final String snmpSysName;
    private final String snmpSysDescr;
    private final Long snmpSysUptimeTicks;
    private final LocalDateTime snmpLastCheckedAt;
    private final String snmpMessage;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private DeviceResponse(NetworkDevice device) {
        this.id = device.getId();
        this.name = device.getName();
        this.ipAddress = device.getIpAddress();
        this.deviceType = device.getDeviceType();
        this.area = device.getArea();
        this.description = device.getDescription();
        this.status = device.getStatus();
        this.lastCheckedAt = device.getLastCheckedAt();
        this.lastResponseTimeMs = device.getLastResponseTimeMs();
        this.snmpEnabled = device.isSnmpEnabled();
        this.snmpVersion = device.getSnmpVersion();
        this.snmpPort = device.getSnmpPort();
        this.snmpCommunityConfigured = device.getSnmpCommunity() != null
                && !device.getSnmpCommunity().isBlank();
        this.snmpStatus = device.getSnmpStatus();
        this.snmpSysName = device.getSnmpSysName();
        this.snmpSysDescr = device.getSnmpSysDescr();
        this.snmpSysUptimeTicks = device.getSnmpSysUptimeTicks();
        this.snmpLastCheckedAt = device.getSnmpLastCheckedAt();
        this.snmpMessage = device.getSnmpMessage();
        this.createdAt = device.getCreatedAt();
        this.updatedAt = device.getUpdatedAt();
    }

    /**
     * 把实体转换成 DTO。
     */
    public static DeviceResponse from(NetworkDevice device) {
        return new DeviceResponse(device);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public String getArea() {
        return area;
    }

    public String getDescription() {
        return description;
    }

    public DeviceStatus getStatus() {
        return status;
    }

    public LocalDateTime getLastCheckedAt() {
        return lastCheckedAt;
    }

    public Integer getLastResponseTimeMs() {
        return lastResponseTimeMs;
    }

    public boolean isSnmpEnabled() {
        return snmpEnabled;
    }

    public String getSnmpVersion() {
        return snmpVersion;
    }

    public int getSnmpPort() {
        return snmpPort;
    }

    public boolean isSnmpCommunityConfigured() {
        return snmpCommunityConfigured;
    }

    public SnmpStatus getSnmpStatus() {
        return snmpStatus;
    }

    public String getSnmpSysName() {
        return snmpSysName;
    }

    public String getSnmpSysDescr() {
        return snmpSysDescr;
    }

    public Long getSnmpSysUptimeTicks() {
        return snmpSysUptimeTicks;
    }

    public LocalDateTime getSnmpLastCheckedAt() {
        return snmpLastCheckedAt;
    }

    public String getSnmpMessage() {
        return snmpMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 页面展示用的创建时间。REST API 不返回这个格式化字段。
     */
    @JsonIgnore
    public String getCreatedAtText() {
        return createdAt == null ? "" : DATE_TIME_FORMATTER.format(createdAt);
    }

    /**
     * 页面展示用的更新时间。REST API 不返回这个格式化字段。
     */
    @JsonIgnore
    public String getUpdatedAtText() {
        return updatedAt == null ? "" : DATE_TIME_FORMATTER.format(updatedAt);
    }

    /**
     * 页面展示用的最近检测时间。REST API 返回原始 lastCheckedAt 字段。
     */
    @JsonIgnore
    public String getLastCheckedAtText() {
        return lastCheckedAt == null ? "—" : DATE_TIME_FORMATTER.format(lastCheckedAt);
    }

    /**
     * 页面展示用的响应时间，例如 "12 ms" 或 "<1 ms"。
     */
    @JsonIgnore
    public String getLastResponseTimeText() {
        if (lastResponseTimeMs == null) {
            return "—";
        }
        if (lastResponseTimeMs == 0) {
            return "<1 ms";
        }
        return lastResponseTimeMs + " ms";
    }

    @JsonIgnore
    public String getSnmpStatusText() {
        if (!snmpEnabled) {
            return "未启用";
        }
        if (snmpLastCheckedAt == null) {
            return "未检测";
        }
        return snmpStatus == null ? "未检测" : snmpStatus.getDisplayName();
    }

    @JsonIgnore
    public String getSnmpStatusCssClass() {
        if (!snmpEnabled || snmpLastCheckedAt == null || snmpStatus == null) {
            return "text-bg-secondary";
        }
        return snmpStatus.getCssClass();
    }

    @JsonIgnore
    public String getSnmpUptimeText() {
        if (snmpSysUptimeTicks == null) {
            return "—";
        }
        long totalSeconds = snmpSysUptimeTicks / 100;
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        String clock = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        return days > 0 ? days + " 天 " + clock : clock;
    }

    @JsonIgnore
    public String getSnmpLastCheckedAtText() {
        return snmpLastCheckedAt == null ? "—" : DATE_TIME_FORMATTER.format(snmpLastCheckedAt);
    }
}
