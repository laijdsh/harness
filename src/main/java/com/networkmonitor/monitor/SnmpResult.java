package com.networkmonitor.monitor;

import com.networkmonitor.entity.SnmpStatus;

import java.time.LocalDateTime;

/**
 * 一次 SNMP 采集的结果。
 */
public class SnmpResult {

    private final SnmpStatus status;
    private final String sysName;
    private final String sysDescr;
    private final Long sysUptimeTicks;
    private final String message;
    private final LocalDateTime checkedAt;

    private SnmpResult(SnmpStatus status,
                       String sysName,
                       String sysDescr,
                       Long sysUptimeTicks,
                       String message,
                       LocalDateTime checkedAt) {
        this.status = status;
        this.sysName = sysName;
        this.sysDescr = sysDescr;
        this.sysUptimeTicks = sysUptimeTicks;
        this.message = message;
        this.checkedAt = checkedAt;
    }

    public static SnmpResult success(String sysName,
                                     String sysDescr,
                                     Long sysUptimeTicks,
                                     LocalDateTime checkedAt) {
        return new SnmpResult(SnmpStatus.OK, sysName, sysDescr, sysUptimeTicks,
                "SNMP 采集成功", checkedAt);
    }

    public static SnmpResult failure(SnmpStatus status, String message) {
        return new SnmpResult(status, null, null, null, message, LocalDateTime.now());
    }

    public SnmpStatus getStatus() {
        return status;
    }

    public String getSysName() {
        return sysName;
    }

    public String getSysDescr() {
        return sysDescr;
    }

    public Long getSysUptimeTicks() {
        return sysUptimeTicks;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getCheckedAt() {
        return checkedAt;
    }
}
