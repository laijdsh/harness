package com.networkmonitor.dto;

import java.time.LocalDateTime;

/**
 * 健康检查响应对象（DTO）。
 *
 * <p>DTO（Data Transfer Object）用于把数据返回给前端，
 * 不直接把数据库实体暴露给外部接口。
 */
public class HealthResponse {

    /** 系统总体状态：UP 或 DOWN */
    private final String status;

    /** 应用名称 */
    private final String application;

    /** 应用版本 */
    private final String version;

    /** 数据库状态：UP 或 DOWN */
    private final String database;

    /** 本次检查时间 */
    private final LocalDateTime timestamp;

    public HealthResponse(String status,
                          String application,
                          String version,
                          String database,
                          LocalDateTime timestamp) {
        this.status = status;
        this.application = application;
        this.version = version;
        this.database = database;
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public String getApplication() {
        return application;
    }

    public String getVersion() {
        return version;
    }

    public String getDatabase() {
        return database;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
