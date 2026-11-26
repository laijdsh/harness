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
 * 设备异常告警实体，对应 alert_record 表。
 *
 * <p>同一设备、同一告警类型在未恢复期间只保留一个活动告警。
 * 异常持续时增加 occurrenceCount 并刷新 lastSeenAt；
 * 恢复时写入 recoveredAt，状态改为 RECOVERED。
 */
@Entity
@Table(
        name = "alert_record",
        indexes = {
                @Index(name = "idx_alert_device_triggered", columnList = "device_id,triggered_at"),
                @Index(name = "idx_alert_triggered_at", columnList = "triggered_at"),
                @Index(name = "idx_alert_status", columnList = "status"),
                @Index(name = "idx_alert_type_status", columnList = "alert_type,status")
        }
)
public class AlertRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 30)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_level", nullable = false, length = 20)
    private AlertLevel alertLevel;

    @Column(name = "message", nullable = false, length = 255)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AlertStatus status = AlertStatus.OPEN;

    @Column(name = "triggered_at", nullable = false)
    private LocalDateTime triggeredAt;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    @Column(name = "recovered_at")
    private LocalDateTime recoveredAt;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "occurrence_count", nullable = false)
    private int occurrenceCount = 1;

    public Long getId() {
        return id;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public AlertType getAlertType() {
        return alertType;
    }

    public void setAlertType(AlertType alertType) {
        this.alertType = alertType;
    }

    public AlertLevel getAlertLevel() {
        return alertLevel;
    }

    public void setAlertLevel(AlertLevel alertLevel) {
        this.alertLevel = alertLevel;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public void setStatus(AlertStatus status) {
        this.status = status;
    }

    public LocalDateTime getTriggeredAt() {
        return triggeredAt;
    }

    public void setTriggeredAt(LocalDateTime triggeredAt) {
        this.triggeredAt = triggeredAt;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(LocalDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public LocalDateTime getRecoveredAt() {
        return recoveredAt;
    }

    public void setRecoveredAt(LocalDateTime recoveredAt) {
        this.recoveredAt = recoveredAt;
    }

    public LocalDateTime getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public int getOccurrenceCount() {
        return occurrenceCount;
    }

    public void setOccurrenceCount(int occurrenceCount) {
        this.occurrenceCount = occurrenceCount;
    }
}
