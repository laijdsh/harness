package com.networkmonitor.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.networkmonitor.entity.AlertLevel;
import com.networkmonitor.entity.AlertRecord;
import com.networkmonitor.entity.AlertStatus;
import com.networkmonitor.entity.AlertType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 告警记录响应对象。
 */
public class AlertResponse {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Long id;
    private final Long deviceId;
    private final String deviceName;
    private final AlertType alertType;
    private final AlertLevel alertLevel;
    private final String message;
    private final AlertStatus status;
    private final LocalDateTime triggeredAt;
    private final LocalDateTime lastSeenAt;
    private final LocalDateTime recoveredAt;
    private final LocalDateTime acknowledgedAt;
    private final int occurrenceCount;

    private AlertResponse(AlertRecord alert, String deviceName) {
        this.id = alert.getId();
        this.deviceId = alert.getDeviceId();
        this.deviceName = deviceName;
        this.alertType = alert.getAlertType();
        this.alertLevel = alert.getAlertLevel();
        this.message = alert.getMessage();
        this.status = alert.getStatus();
        this.triggeredAt = alert.getTriggeredAt();
        this.lastSeenAt = alert.getLastSeenAt();
        this.recoveredAt = alert.getRecoveredAt();
        this.acknowledgedAt = alert.getAcknowledgedAt();
        this.occurrenceCount = alert.getOccurrenceCount();
    }

    public static AlertResponse from(AlertRecord alert, String deviceName) {
        return new AlertResponse(alert, deviceName);
    }

    public Long getId() {
        return id;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public AlertType getAlertType() {
        return alertType;
    }

    public AlertLevel getAlertLevel() {
        return alertLevel;
    }

    public String getMessage() {
        return message;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public LocalDateTime getTriggeredAt() {
        return triggeredAt;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public LocalDateTime getRecoveredAt() {
        return recoveredAt;
    }

    public LocalDateTime getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public int getOccurrenceCount() {
        return occurrenceCount;
    }

    /**
     * 只有未恢复告警可以确认；已确认或已恢复的告警不再显示确认按钮。
     */
    @JsonIgnore
    public boolean isAcknowledgeable() {
        return status == AlertStatus.OPEN;
    }

    @JsonIgnore
    public String getTriggeredAtText() {
        return triggeredAt == null ? "—" : DATE_TIME_FORMATTER.format(triggeredAt);
    }

    @JsonIgnore
    public String getLastSeenAtText() {
        return lastSeenAt == null ? "—" : DATE_TIME_FORMATTER.format(lastSeenAt);
    }

    @JsonIgnore
    public String getRecoveredAtText() {
        return recoveredAt == null ? "—" : DATE_TIME_FORMATTER.format(recoveredAt);
    }

    @JsonIgnore
    public String getAcknowledgedAtText() {
        return acknowledgedAt == null ? "—" : DATE_TIME_FORMATTER.format(acknowledgedAt);
    }
}
