-- ============================================================
-- 阶段 2 → 阶段 3 数据库迁移脚本
--
-- 适用场景：已经执行过阶段 2 的 init.sql，库中已有 network_device
-- 和 4 台演示设备，不能直接重建表。
--
-- 注意：ALTER TABLE 部分只需要执行一次；重复执行会提示列已存在。
-- ============================================================

USE network_monitor;

-- 给设备表增加“最近一次监测结果”摘要字段
ALTER TABLE network_device
    ADD COLUMN last_checked_at DATETIME(6) NULL COMMENT '最近一次检测时间' AFTER status,
    ADD COLUMN last_response_time_ms INT NULL COMMENT '最近一次响应时间（毫秒）' AFTER last_checked_at,
    ADD KEY idx_network_device_last_checked_at (last_checked_at);

-- 监测记录历史表
CREATE TABLE IF NOT EXISTS monitor_record (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录主键',
    device_id        BIGINT       NOT NULL COMMENT '设备 ID',
    check_time       DATETIME(6)  NOT NULL COMMENT '检测时间',
    reachable        TINYINT(1)   NOT NULL COMMENT '是否收到 ICMP 回复：1 是，0 否',
    response_time_ms INT          NULL COMMENT '响应时间（毫秒），失败时为空',
    status           VARCHAR(20)  NOT NULL COMMENT '检测状态：ONLINE/OFFLINE/ERROR',
    result_message   VARCHAR(255) NULL COMMENT '检测结果说明或错误原因',
    PRIMARY KEY (id),
    KEY idx_monitor_record_device_time (device_id, check_time),
    KEY idx_monitor_record_check_time (check_time),
    KEY idx_monitor_record_status (status),
    CONSTRAINT fk_monitor_record_device
        FOREIGN KEY (device_id) REFERENCES network_device (id)
        ON DELETE CASCADE,
    CONSTRAINT chk_monitor_record_status
        CHECK (status IN ('ONLINE', 'OFFLINE', 'ERROR'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '设备监测记录表';
