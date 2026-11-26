package com.networkmonitor.exception;

/**
 * 设备存在但还没有任何监测记录时抛出。
 */
public class MonitorRecordNotFoundException extends RuntimeException {

    public MonitorRecordNotFoundException(Long deviceId) {
        super("设备暂无监测记录，设备 ID=" + deviceId);
    }
}
