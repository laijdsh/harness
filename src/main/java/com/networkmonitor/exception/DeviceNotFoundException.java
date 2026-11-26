package com.networkmonitor.exception;

/**
 * 请求的设备不存在时抛出。
 *
 * <p>页面控制器会把它转换成“设备不存在”的提示；
 * REST API 会把它转换成 HTTP 404。
 */
public class DeviceNotFoundException extends RuntimeException {

    public DeviceNotFoundException(Long id) {
        super("设备不存在，ID=" + id);
    }
}
