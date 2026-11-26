package com.networkmonitor.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * “检测全部设备”的汇总结果。
 */
public class CheckAllResponse {

    private final int total;
    private final int online;
    private final int offline;
    private final int error;
    private final LocalDateTime checkedAt;
    private final List<MonitorRecordResponse> results;

    public CheckAllResponse(int total,
                            int online,
                            int offline,
                            int error,
                            LocalDateTime checkedAt,
                            List<MonitorRecordResponse> results) {
        this.total = total;
        this.online = online;
        this.offline = offline;
        this.error = error;
        this.checkedAt = checkedAt;
        this.results = results;
    }

    public int getTotal() {
        return total;
    }

    public int getOnline() {
        return online;
    }

    public int getOffline() {
        return offline;
    }

    public int getError() {
        return error;
    }

    public LocalDateTime getCheckedAt() {
        return checkedAt;
    }

    public List<MonitorRecordResponse> getResults() {
        return results;
    }
}
