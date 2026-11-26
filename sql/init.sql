-- ============================================================
-- 轻量级网络设备状态监测与异常告警系统
-- 数据库初始化脚本（最新结构）
--
-- 说明：
-- 1. 本文件不包含任何密码。
-- 2. 应用账号 netmon 已在开发环境中创建，密码保存在项目外的
--    D:\dev\secrets\mysql-netmon.txt，禁止提交到 Git。
-- 3. 新环境可直接执行本文件；已有旧数据库请按阶段执行迁移：
--    sql/phase3-migration.sql、sql/phase4-migration.sql、
--    sql/phase6-migration.sql。
-- ============================================================

CREATE DATABASE IF NOT EXISTS network_monitor
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE network_monitor;

-- ------------------------------------------------------------
-- 网络设备信息表
--
-- 保存设备基础资料，以及“最近一次监测结果”的摘要。
-- 完整历史记录保存在 monitor_record 表。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS network_device (
    id                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '设备主键',
    device_name           VARCHAR(100) NOT NULL COMMENT '设备名称',
    ip_address            VARCHAR(45)  NOT NULL COMMENT '设备 IP 地址（当前按 IPv4 校验，字段预留 IPv6）',
    device_type           VARCHAR(20)  NOT NULL COMMENT '设备类型：ROUTER/SWITCH/SERVER/OTHER',
    area                  VARCHAR(100) NULL COMMENT '所属区域',
    description           VARCHAR(255) NULL COMMENT '备注',
    status                VARCHAR(20)  NOT NULL DEFAULT 'UNKNOWN' COMMENT '状态：ONLINE/OFFLINE/UNKNOWN/ERROR',
    last_checked_at       DATETIME(6)  NULL COMMENT '最近一次检测时间',
    last_response_time_ms INT          NULL COMMENT '最近一次响应时间（毫秒）',
    snmp_enabled          TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否启用 SNMP：1 是，0 否',
    snmp_version          VARCHAR(10)  NOT NULL DEFAULT '2c' COMMENT 'SNMP 版本，当前仅支持 2c',
    snmp_port             INT          NOT NULL DEFAULT 161 COMMENT 'SNMP UDP 端口',
    snmp_community        VARCHAR(100) NULL COMMENT 'SNMP 社区字符串，敏感字段，不通过 API 返回',
    snmp_status           VARCHAR(20)  NOT NULL DEFAULT 'DISABLED' COMMENT 'SNMP 状态：DISABLED/OK/UNREACHABLE/ERROR',
    snmp_sys_name         VARCHAR(255) NULL COMMENT 'SNMP sysName',
    snmp_sys_descr        VARCHAR(500) NULL COMMENT 'SNMP sysDescr',
    snmp_sys_uptime_ticks BIGINT       NULL COMMENT 'SNMP sysUpTime，单位 1/100 秒',
    snmp_last_checked_at  DATETIME(6)  NULL COMMENT '最近一次 SNMP 采集时间',
    snmp_message          VARCHAR(255) NULL COMMENT '最近一次 SNMP 采集说明',
    created_at            DATETIME(6)  NOT NULL COMMENT '创建时间',
    updated_at            DATETIME(6)  NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_network_device_ip (ip_address),
    KEY idx_network_device_name (device_name),
    KEY idx_network_device_type (device_type),
    KEY idx_network_device_status (status),
    KEY idx_network_device_last_checked_at (last_checked_at),
    KEY idx_network_device_snmp_status (snmp_status),
    KEY idx_network_device_created_at (created_at),
    CONSTRAINT chk_network_device_type
        CHECK (device_type IN ('ROUTER', 'SWITCH', 'SERVER', 'OTHER')),
    CONSTRAINT chk_network_device_status
        CHECK (status IN ('ONLINE', 'OFFLINE', 'UNKNOWN', 'ERROR')),
    CONSTRAINT chk_network_device_snmp_version
        CHECK (snmp_version IN ('2c')),
    CONSTRAINT chk_network_device_snmp_status
        CHECK (snmp_status IN ('DISABLED', 'OK', 'UNREACHABLE', 'ERROR'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '网络设备信息表';

-- ------------------------------------------------------------
-- 设备监测记录表
--
-- 每次 Ping 检测保存一条记录，用于历史查询和后续趋势图。
-- 一个设备可以有多条记录；设备删除时记录级联删除。
-- ------------------------------------------------------------
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

-- ------------------------------------------------------------
-- 设备异常告警记录表
--
-- 同一设备、同一告警类型只保留一个活动告警；
-- 持续异常时更新 last_seen_at 和 occurrence_count，不重复插入。
-- ------------------------------------------------------------
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
