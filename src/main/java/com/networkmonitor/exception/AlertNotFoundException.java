package com.networkmonitor.exception;

/**
 * 告警记录不存在时抛出。
 */
public class AlertNotFoundException extends RuntimeException {

    public AlertNotFoundException(Long id) {
        super("告警记录不存在，ID=" + id);
    }
}
