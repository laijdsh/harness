package com.networkmonitor.exception;

/**
 * 告警状态不允许当前操作时抛出，例如确认一条已经恢复的告警。
 */
public class InvalidAlertStateException extends RuntimeException {

    public InvalidAlertStateException(String message) {
        super(message);
    }
}
