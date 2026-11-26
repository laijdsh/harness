package com.networkmonitor.controller;

import com.networkmonitor.dto.AlertResponse;
import com.networkmonitor.service.AlertService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 数据看板页面控制器。
 */
@Controller
public class DashboardController {

    private final AlertService alertService;

    public DashboardController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Page<AlertResponse> recentAlerts = alertService.search(null, null, null, 0, 5);
        model.addAttribute("recentAlerts", recentAlerts.getContent());
        return "dashboard";
    }
}
