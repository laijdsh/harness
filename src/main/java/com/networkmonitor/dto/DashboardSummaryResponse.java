package com.networkmonitor.dto;

import java.time.LocalDateTime;

/**
 * 看板顶部汇总数据。
 */
public record DashboardSummaryResponse(
        long deviceTotal,
        long online,
        long offline,
        long unknown,
        long error,
        long activeAlerts,
        long totalAlerts,
        long monitorRecords,
        LocalDateTime generatedAt
) {
}
