package com.networkmonitor.controller;

import com.networkmonitor.dto.CheckAllResponse;
import com.networkmonitor.dto.DeviceResponse;
import com.networkmonitor.dto.MonitorRecordResponse;
import com.networkmonitor.exception.DeviceNotFoundException;
import com.networkmonitor.service.MonitorService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 设备检测的页面操作入口。
 *
 * <p>页面按钮通过 POST 触发表单提交，检测完成后以 Flash 消息提示，
 * 再重定向回列表或详情页，避免刷新页面重复提交。
 */
@Controller
@RequestMapping("/devices")
public class MonitorController {

    private final MonitorService monitorService;

    public MonitorController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @PostMapping("/{id}/check")
    public String checkOne(@PathVariable Long id,
                           @RequestParam(value = "returnTo", required = false) String returnTo,
                           RedirectAttributes redirectAttributes) {
        try {
            MonitorRecordResponse result = monitorService.checkDevice(id);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "检测完成：" + result.getStatus().getDisplayName()
                            + "，响应时间 " + result.getResponseTimeText()
            );
        } catch (DeviceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        if ("detail".equals(returnTo)) {
            return "redirect:/devices/" + id;
        }
        return "redirect:/devices";
    }

    @PostMapping("/{id}/snmp-check")
    public String checkSnmp(@PathVariable Long id,
                            RedirectAttributes redirectAttributes) {
        try {
            DeviceResponse result = monitorService.checkSnmp(id);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "SNMP 采集完成：" + result.getSnmpStatusText()
                            + (result.getSnmpSysName() == null ? "" : "，sysName=" + result.getSnmpSysName())
            );
        } catch (DeviceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "SNMP 采集失败：" + ex.getMessage());
        }
        return "redirect:/devices/" + id;
    }

    @PostMapping("/check-all")
    public String checkAll(RedirectAttributes redirectAttributes) {
        try {
            CheckAllResponse summary = monitorService.checkAll();
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "检测完成：共 " + summary.getTotal() + " 台，在线 " + summary.getOnline()
                            + " 台，离线 " + summary.getOffline() + " 台，异常 "
                            + summary.getError() + " 台"
            );
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "检测失败：" + ex.getMessage());
        }
        return "redirect:/devices";
    }
}
