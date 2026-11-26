package com.networkmonitor.controller;

import com.networkmonitor.dto.AlertResponse;
import com.networkmonitor.entity.AlertLevel;
import com.networkmonitor.entity.AlertStatus;
import com.networkmonitor.exception.AlertNotFoundException;
import com.networkmonitor.exception.InvalidAlertStateException;
import com.networkmonitor.service.AlertService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 告警页面的控制器。
 */
@Controller
@RequestMapping("/alerts")
public class AlertController {

    private static final int PAGE_SIZE = 10;

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public String list(@RequestParam(value = "status", required = false) AlertStatus status,
                       @RequestParam(value = "level", required = false) AlertLevel level,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       Model model) {
        Page<AlertResponse> alerts = alertService.search(null, status, level, page, PAGE_SIZE);

        model.addAttribute("alerts", alerts);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedLevel", level);
        model.addAttribute("alertStatuses", AlertStatus.values());
        model.addAttribute("alertLevels", AlertLevel.values());
        return "alerts/list";
    }

    @PostMapping("/{id}/acknowledge")
    public String acknowledge(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            alertService.acknowledge(id);
            redirectAttributes.addFlashAttribute("successMessage", "告警已确认");
        } catch (AlertNotFoundException | InvalidAlertStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/alerts";
    }
}
