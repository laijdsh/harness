package com.networkmonitor.controller;

import com.networkmonitor.dto.DeviceRequest;
import com.networkmonitor.dto.DeviceResponse;
import com.networkmonitor.entity.DeviceType;
import com.networkmonitor.entity.NetworkDevice;
import com.networkmonitor.exception.DeviceNotFoundException;
import com.networkmonitor.exception.DuplicateIpException;
import com.networkmonitor.service.DeviceService;
import com.networkmonitor.service.MonitorService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 设备管理的页面控制器。
 *
 * <p>负责 Thymeleaf 页面跳转和表单提交，
 * 真正的业务规则在 DeviceService 中。
 */
@Controller
@RequestMapping("/devices")
public class DeviceController {

    private static final int PAGE_SIZE = 10;

    private final DeviceService deviceService;
    private final MonitorService monitorService;

    public DeviceController(DeviceService deviceService, MonitorService monitorService) {
        this.deviceService = deviceService;
        this.monitorService = monitorService;
    }

    @GetMapping
    public String list(@RequestParam(value = "keyword", required = false) String keyword,
                       @RequestParam(value = "type", required = false) DeviceType type,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       Model model) {
        Page<DeviceResponse> devices = deviceService.search(keyword, type, page, PAGE_SIZE)
                .map(DeviceResponse::from);

        model.addAttribute("devices", devices);
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        model.addAttribute("selectedType", type);
        model.addAttribute("deviceTypes", DeviceType.values());
        return "devices/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("deviceRequest", new DeviceRequest());
        addFormOptions(model, false);
        return "devices/form";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        NetworkDevice device = deviceService.get(id);
        model.addAttribute("device", DeviceResponse.from(device));
        model.addAttribute("latestRecord", monitorService.getLatestOrNull(id));
        model.addAttribute("recentRecords", monitorService.getRecentRecords(id, 10));
        return "devices/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        NetworkDevice device = deviceService.get(id);

        DeviceRequest request = new DeviceRequest();
        request.setId(device.getId());
        request.setName(device.getName());
        request.setIpAddress(device.getIpAddress());
        request.setDeviceType(device.getDeviceType());
        request.setArea(device.getArea());
        request.setDescription(device.getDescription());

        model.addAttribute("deviceRequest", request);
        model.addAttribute("currentStatus", device.getStatus());
        addFormOptions(model, true);
        return "devices/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("deviceRequest") DeviceRequest request,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        boolean edit = request.getId() != null;

        if (bindingResult.hasErrors()) {
            addFormOptions(model, edit);
            addCurrentStatus(model, request.getId());
            return "devices/form";
        }

        try {
            if (edit) {
                deviceService.update(request.getId(), request);
                redirectAttributes.addFlashAttribute("successMessage", "设备信息已更新");
            } else {
                deviceService.create(request);
                redirectAttributes.addFlashAttribute("successMessage", "设备添加成功");
            }
            return "redirect:/devices";
        } catch (DuplicateIpException ex) {
            bindingResult.rejectValue("ipAddress", "duplicate", ex.getMessage());
            addFormOptions(model, edit);
            addCurrentStatus(model, request.getId());
            return "devices/form";
        } catch (DeviceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/devices";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            deviceService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "设备已删除");
        } catch (DeviceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/devices";
    }

    /**
     * 编辑一个已经不存在的设备时，回到列表并显示提示。
     */
    @ExceptionHandler(DeviceNotFoundException.class)
    public String handleDeviceNotFound(DeviceNotFoundException ex,
                                       RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:/devices";
    }

    private void addFormOptions(Model model, boolean edit) {
        model.addAttribute("deviceTypes", DeviceType.values());
        model.addAttribute("formTitle", edit ? "编辑设备" : "添加设备");
    }

    /**
     * 编辑表单校验失败时，仍需要显示设备当前的监测状态。
     */
    private void addCurrentStatus(Model model, Long id) {
        if (id == null) {
            return;
        }
        try {
            model.addAttribute("currentStatus", deviceService.get(id).getStatus());
        } catch (DeviceNotFoundException ignored) {
            // 设备已被删除时不再显示旧状态，保存逻辑会在后面处理。
        }
    }
}
