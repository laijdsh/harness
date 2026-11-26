-- ============================================================
-- 阶段 3 → 阶段 4 数据库迁移脚本
--
-- 新增告警记录表 alert_record。
-- 不修改 network_device 和 monitor_record 的已有数据。
-- ============================================================

USE network_monitor;

CREATE TABLE IF NOT EXISTS alert_record (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '告警主键',
    device_id        BIGINT       NOT NULL COMMENT '设备 ID',
    alert_type       VARCHAR(30)  NOT NULL COMMENT '告警类型：DEVICE_OFFLINE/HIGH_LATENCY',
    alert_level      VARCHAR(20)  NOT NULL COMMENT '告警级别：INFO/WARNING/CRITICAL',
    message          VARCHAR(255) NOT NULL COMMENT '告警内容',
    status           VARCHAR(20)  NOT NULL DEFAULT 'OPEN' COMMENT '状态：OPEN/ACKNOWLEDGED/RECOVERED',
    triggered_at     DATETIME(6)  NOT NULL COMMENT '首次触发时间',
    last_seen_at     DATETIME(6)  NOT NULL COMMENT '最近一次仍然异常的时间',
    recovered_at     DATETIME(6)  NULL COMMENT '恢复时间',
    acknowledged_at  DATETIME(6)  NULL COMMENT '确认时间',
    occurrence_count INT          NOT NULL DEFAULT 1 COMMENT '持续出现次数',
    PRIMARY KEY (id),
    KEY idx_alert_device_triggered (device_id, triggered_at),
    KEY idx_alert_triggered_at (triggered_at),
    KEY idx_alert_status (status),
    KEY idx_alert_type_status (alert_type, status),
    CONSTRAINT fk_alert_record_device
        FOREIGN KEY (device_id) REFERENCES network_device (id)
        ON DELETE CASCADE,
    CONSTRAINT chk_alert_record_type
        CHECK (alert_type IN ('DEVICE_OFFLINE', 'HIGH_LATENCY')),
    CONSTRAINT chk_alert_record_level
        CHECK (alert_level IN ('INFO', 'WARNING', 'CRITICAL')),
    CONSTRAINT chk_alert_record_status
        CHECK (status IN ('OPEN', 'ACKNOWLEDGED', 'RECOVERED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '设备异常告警记录表';
