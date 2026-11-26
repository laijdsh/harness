package com.networkmonitor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/**
 * 网络设备实体，对应数据库表 network_device。
 *
 * <p>一个对象代表一台需要被监测的交换机、路由器、服务器或其他网络设备。
 * 设备表保存基础资料和最近一次监测摘要；完整的每次检测历史保存在
 * monitor_record 表中。
 */
@Entity
@Table(
        name = "network_device",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_network_device_ip",
                columnNames = "ip_address"
        )
)
public class NetworkDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_name", nullable = false, length = 100)
    private String name;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 20)
    private DeviceType deviceType;

    @Column(name = "area", length = 100)
    private String area;

    @Column(name = "description", length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DeviceStatus status = DeviceStatus.UNKNOWN;

    @Column(name = "last_checked_at")
    private LocalDateTime lastCheckedAt;

    @Column(name = "last_response_time_ms")
    private Integer lastResponseTimeMs;

    @Column(name = "snmp_enabled", nullable = false)
    private boolean snmpEnabled;

    @Column(name = "snmp_version", nullable = false, length = 10)
    private String snmpVersion = "2c";

    @Column(name = "snmp_port", nullable = false)
    private int snmpPort = 161;

    @Column(name = "snmp_community", length = 100)
    private String snmpCommunity;

    @Enumerated(EnumType.STRING)
    @Column(name = "snmp_status", nullable = false, length = 20)
    private SnmpStatus snmpStatus = SnmpStatus.DISABLED;

    @Column(name = "snmp_sys_name", length = 255)
    private String snmpSysName;

    @Column(name = "snmp_sys_descr", length = 500)
    private String snmpSysDescr;

    @Column(name = "snmp_sys_uptime_ticks")
    private Long snmpSysUptimeTicks;

    @Column(name = "snmp_last_checked_at")
    private LocalDateTime snmpLastCheckedAt;

    @Column(name = "snmp_message", length = 255)
    private String snmpMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 第一次保存前自动填充创建时间和更新时间。
     */
    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (status == null) {
            status = DeviceStatus.UNKNOWN;
        }
        if (snmpVersion == null || snmpVersion.isBlank()) {
            snmpVersion = "2c";
        }
        if (snmpPort <= 0) {
            snmpPort = 161;
        }
        if (snmpStatus == null) {
            snmpStatus = SnmpStatus.DISABLED;
        }
    }

    /**
     * 每次更新前自动刷新更新时间。
     */
    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(DeviceType deviceType) {
        this.deviceType = deviceType;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DeviceStatus getStatus() {
        return status;
    }

    public void setStatus(DeviceStatus status) {
        this.status = status;
    }

    public LocalDateTime getLastCheckedAt() {
        return lastCheckedAt;
    }

    public void setLastCheckedAt(LocalDateTime lastCheckedAt) {
        this.lastCheckedAt = lastCheckedAt;
    }

    public Integer getLastResponseTimeMs() {
        return lastResponseTimeMs;
    }

    public void setLastResponseTimeMs(Integer lastResponseTimeMs) {
        this.lastResponseTimeMs = lastResponseTimeMs;
    }

    public boolean isSnmpEnabled() {
        return snmpEnabled;
    }

    public void setSnmpEnabled(boolean snmpEnabled) {
        this.snmpEnabled = snmpEnabled;
    }

    public String getSnmpVersion() {
        return snmpVersion;
    }

    public void setSnmpVersion(String snmpVersion) {
        this.snmpVersion = snmpVersion;
    }

    public int getSnmpPort() {
        return snmpPort;
    }

    public void setSnmpPort(int snmpPort) {
        this.snmpPort = snmpPort;
    }

    /**
     * 社区字符串只供 SNMP 采集服务使用，不能返回给前端或 API。
     */
    public String getSnmpCommunity() {
        return snmpCommunity;
    }

    public void setSnmpCommunity(String snmpCommunity) {
        this.snmpCommunity = snmpCommunity;
    }

    public SnmpStatus getSnmpStatus() {
        return snmpStatus;
    }

    public void setSnmpStatus(SnmpStatus snmpStatus) {
        this.snmpStatus = snmpStatus;
    }

    public String getSnmpSysName() {
        return snmpSysName;
    }

    public void setSnmpSysName(String snmpSysName) {
        this.snmpSysName = snmpSysName;
    }

    public String getSnmpSysDescr() {
        return snmpSysDescr;
    }

    public void setSnmpSysDescr(String snmpSysDescr) {
        this.snmpSysDescr = snmpSysDescr;
    }

    public Long getSnmpSysUptimeTicks() {
        return snmpSysUptimeTicks;
    }

    public void setSnmpSysUptimeTicks(Long snmpSysUptimeTicks) {
        this.snmpSysUptimeTicks = snmpSysUptimeTicks;
    }

    public LocalDateTime getSnmpLastCheckedAt() {
        return snmpLastCheckedAt;
    }

    public void setSnmpLastCheckedAt(LocalDateTime snmpLastCheckedAt) {
        this.snmpLastCheckedAt = snmpLastCheckedAt;
    }

    public String getSnmpMessage() {
        return snmpMessage;
    }

    public void setSnmpMessage(String snmpMessage) {
        this.snmpMessage = snmpMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
