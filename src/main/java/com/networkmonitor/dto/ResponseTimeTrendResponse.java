package com.networkmonitor.dto;

import java.util.List;

/**
 * 响应时间趋势数据。
 *
 * <p>values 使用 null 表示该时间桶没有在线检测数据，ECharts 会显示为断点，
 * 避免把“没有检测”误画成 0ms。
 */
public record ResponseTimeTrendResponse(
        int hours,
        int bucketMinutes,
        List<String> labels,
        List<Double> values,
        List<Long> counts
) {
}
