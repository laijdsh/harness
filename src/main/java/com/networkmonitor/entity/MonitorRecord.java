package com.networkmonitor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 设备监测记录实体，对应 monitor_record 表。
 *
 * <p>每执行一次 Ping 检测就保存一条记录。设备表只保留最近一次摘要，
 * 这里保留完整历史，供设备详情和后续趋势图使用。
 */
@Entity
@Table(
        name = "monitor_record",
        indexes = {
                @Index(name = "idx_monitor_record_device_time", columnList = "device_id,check_time"),
                @Index(name = "idx_monitor_record_check_time", columnList = "check_time"),
                @Index(name = "idx_monitor_record_status", columnList = "status")
        }
)
public class MonitorRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(name = "check_time", nullable = false)
    private LocalDateTime checkTime;

    @Column(name = "reachable", nullable = false)
    private boolean reachable;

    @Column(name = "response_time_ms")
    private Integer responseTimeMs;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DeviceStatus status;

    @Column(name = "result_message", length = 255)
    private String resultMessage;

    public Long getId() {
        return id;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public LocalDateTime getCheckTime() {
        return checkTime;
    }

    public void setCheckTime(LocalDateTime checkTime) {
        this.checkTime = checkTime;
    }

    public boolean isReachable() {
        return reachable;
    }

    public void setReachable(boolean reachable) {
        this.reachable = reachable;
    }

    public Integer getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(Integer responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    public DeviceStatus getStatus() {
        return status;
    }

    public void setStatus(DeviceStatus status) {
        this.status = status;
    }

    public String getResultMessage() {
        return resultMessage;
    }

    public void setResultMessage(String resultMessage) {
        this.resultMessage = resultMessage;
    }
}
