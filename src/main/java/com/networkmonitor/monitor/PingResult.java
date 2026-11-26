package com.networkmonitor.monitor;

import com.networkmonitor.entity.DeviceStatus;

/**
 * 一次 Ping 探测的结果。
 *
 * <p>这个对象只描述“网络探测层”看到的原始结果，
 * 是否触发告警、是否连续失败由后续 Alert 层判断。
 */
public class PingResult {

    private final boolean reachable;
    private final Integer responseTimeMs;
    private final DeviceStatus status;
    private final String message;

    private PingResult(boolean reachable,
                       Integer responseTimeMs,
                       DeviceStatus status,
                       String message) {
        this.reachable = reachable;
        this.responseTimeMs = responseTimeMs;
        this.status = status;
        this.message = message;
    }

    public static PingResult online(Integer responseTimeMs, String message) {
        return new PingResult(true, responseTimeMs, DeviceStatus.ONLINE, message);
    }

    public static PingResult offline(String message) {
        return new PingResult(false, null, DeviceStatus.OFFLINE, message);
    }

    public static PingResult error(String message) {
        return new PingResult(false, null, DeviceStatus.ERROR, message);
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

    public String getMessage() {
        return message;
    }
}
