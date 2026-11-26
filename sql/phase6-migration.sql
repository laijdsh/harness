-- ============================================================
-- 阶段 5 → 阶段 6 数据库迁移脚本
--
-- 为 network_device 增加 SNMP v2c 配置和最近采集结果字段。
-- 不修改已有设备、监测记录和告警数据。
--
-- 注意：snmp_community 是敏感字段，只能保存在数据库中，
-- 不应通过 API 返回、不应写入日志或提交到 Git。
-- ============================================================

USE network_monitor;

ALTER TABLE network_device
    ADD COLUMN snmp_enabled TINYINT(1) NOT NULL DEFAULT 0
        COMMENT '是否启用 SNMP：1 是，0 否' AFTER last_response_time_ms,
    ADD COLUMN snmp_version VARCHAR(10) NOT NULL DEFAULT '2c'
        COMMENT 'SNMP 版本，当前仅支持 2c' AFTER snmp_enabled,
    ADD COLUMN snmp_port INT NOT NULL DEFAULT 161
        COMMENT 'SNMP UDP 端口' AFTER snmp_version,
    ADD COLUMN snmp_community VARCHAR(100) NULL
        COMMENT 'SNMP 社区字符串，敏感字段，不通过 API 返回' AFTER snmp_port,
    ADD COLUMN snmp_status VARCHAR(20) NOT NULL DEFAULT 'DISABLED'
        COMMENT 'SNMP 状态：DISABLED/OK/UNREACHABLE/ERROR' AFTER snmp_community,
    ADD COLUMN snmp_sys_name VARCHAR(255) NULL
        COMMENT 'SNMP sysName' AFTER snmp_status,
    ADD COLUMN snmp_sys_descr VARCHAR(500) NULL
        COMMENT 'SNMP sysDescr' AFTER snmp_sys_name,
    ADD COLUMN snmp_sys_uptime_ticks BIGINT NULL
        COMMENT 'SNMP sysUpTime，单位 1/100 秒' AFTER snmp_sys_descr,
    ADD COLUMN snmp_last_checked_at DATETIME(6) NULL
        COMMENT '最近一次 SNMP 采集时间' AFTER snmp_sys_uptime_ticks,
    ADD COLUMN snmp_message VARCHAR(255) NULL
        COMMENT '最近一次 SNMP 采集说明' AFTER snmp_last_checked_at;

ALTER TABLE network_device
    ADD KEY idx_network_device_snmp_status (snmp_status),
    ADD CONSTRAINT chk_network_device_snmp_version
        CHECK (snmp_version IN ('2c')),
    ADD CONSTRAINT chk_network_device_snmp_status
        CHECK (snmp_status IN ('DISABLED', 'OK', 'UNREACHABLE', 'ERROR'));
