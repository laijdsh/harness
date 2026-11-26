package com.networkmonitor.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.networkmonitor.entity.DeviceStatus;
import com.networkmonitor.entity.MonitorRecord;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 一条监测记录的响应对象。
 */
public class MonitorRecordResponse {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Long id;
    private final Long deviceId;
    private final String deviceName;
    private final LocalDateTime checkTime;
    private final boolean reachable;
    private final Integer responseTimeMs;
    private final DeviceStatus status;
    private final String resultMessage;

    private MonitorRecordResponse(MonitorRecord record, String deviceName) {
        this.id = record.getId();
        this.deviceId = record.getDeviceId();
        this.deviceName = deviceName;
        this.checkTime = record.getCheckTime();
        this.reachable = record.isReachable();
        this.responseTimeMs = record.getResponseTimeMs();
        this.status = record.getStatus();
        this.resultMessage = record.getResultMessage();
    }

    public static MonitorRecordResponse from(MonitorRecord record, String deviceName) {
        return new MonitorRecordResponse(record, deviceName);
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

    public LocalDateTime getCheckTime() {
        return checkTime;
    }

    public boolean isReachable() {
        return reachable;
    }

    public Integer getResponseTimeMs() {
        return responseTimeMs;
    }

    public DeviceStatus getStatus() {
        return status;
    }

    public String getResultMessage() {
        return resultMessage;
    }

    /**
     * 页面展示用的检测时间。
     */
    @JsonIgnore
    public String getCheckTimeText() {
        return checkTime == null ? "" : DATE_TIME_FORMATTER.format(checkTime);
    }

    /**
     * 页面展示用的响应时间。
     */
    @JsonIgnore
    public String getResponseTimeText() {
        if (responseTimeMs == null) {
            return "—";
        }
        if (responseTimeMs == 0) {
            return "<1 ms";
        }
        return responseTimeMs + " ms";
    }
}
