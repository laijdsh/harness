package com.networkmonitor.controller.api;

import com.networkmonitor.dto.AlertDistributionResponse;
import com.networkmonitor.dto.DashboardSummaryResponse;
import com.networkmonitor.dto.ResponseTimeTrendResponse;
import com.networkmonitor.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据看板 REST API。
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardApiController {

    private final DashboardService dashboardService;

    public DashboardApiController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public DashboardSummaryResponse summary() {
        return dashboardService.summary();
    }

    @GetMapping("/response-time-trend")
    public ResponseTimeTrendResponse responseTimeTrend(
            @RequestParam(value = "hours", defaultValue = "24") int hours) {
        return dashboardService.responseTimeTrend(hours);
    }

    @GetMapping("/alert-distribution")
    public AlertDistributionResponse alertDistribution() {
        return dashboardService.alertDistribution();
    }
}
