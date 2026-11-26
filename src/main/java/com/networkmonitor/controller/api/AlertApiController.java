package com.networkmonitor.controller.api;

import com.networkmonitor.dto.AlertResponse;
import com.networkmonitor.dto.PageResponse;
import com.networkmonitor.entity.AlertLevel;
import com.networkmonitor.entity.AlertStatus;
import com.networkmonitor.service.AlertService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 告警 REST API。
 *
 * <p>GET  /api/alerts                      分页查询告警
 * GET  /api/alerts/{id}                   查询单条告警
 * POST /api/alerts/{id}/acknowledge       确认告警
 */
@RestController
@RequestMapping("/api/alerts")
public class AlertApiController {

    private final AlertService alertService;

    public AlertApiController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public PageResponse<AlertResponse> list(
            @RequestParam(value = "deviceId", required = false) Long deviceId,
            @RequestParam(value = "status", required = false) AlertStatus status,
            @RequestParam(value = "level", required = false) AlertLevel level,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        Page<AlertResponse> alerts = alertService.search(deviceId, status, level, page, size);
        return PageResponse.from(alerts);
    }

    @GetMapping("/{id}")
    public AlertResponse get(@PathVariable Long id) {
        return alertService.get(id);
    }

    @PostMapping("/{id}/acknowledge")
    public AlertResponse acknowledge(@PathVariable Long id) {
        return alertService.acknowledge(id);
    }
}
