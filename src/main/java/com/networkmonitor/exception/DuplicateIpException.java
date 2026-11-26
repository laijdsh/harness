package com.networkmonitor.exception;

/**
 * 设备 IP 地址重复时抛出。
 *
 * <p>页面表单会把它显示在 IP 输入框下方；
 * REST API 会把它转换成 HTTP 409 Conflict。
 */
public class DuplicateIpException extends RuntimeException {

    public DuplicateIpException(String ipAddress) {
        super("IP 地址已存在：" + ipAddress);
    }
}
