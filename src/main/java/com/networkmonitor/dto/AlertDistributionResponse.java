package com.networkmonitor.dto;

import java.util.Map;

/**
 * 告警分布数据，按键名直接给前端使用。
 */
public record AlertDistributionResponse(
        Map<String, Long> statusCounts,
        Map<String, Long> typeCounts,
        Map<String, Long> levelCounts
) {
}
