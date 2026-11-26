package com.networkmonitor.controller.api;

import com.networkmonitor.dto.CheckAllResponse;
import com.networkmonitor.dto.DeviceResponse;
import com.networkmonitor.dto.MonitorRecordResponse;
import com.networkmonitor.dto.PageResponse;
import com.networkmonitor.service.MonitorService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 设备检测与监测历史 REST API。
 *
 * <p>Base URL：/api/devices
 *
 * <p>POST /api/devices/{id}/check          检测单台设备
 * POST /api/devices/check-all             检测全部设备
 * GET  /api/devices/{id}/records          分页查询监测历史
 * GET  /api/devices/{id}/records/latest   查询最近一条记录
 */
@RestController
@RequestMapping("/api/devices")
public class MonitorApiController {

    private final MonitorService monitorService;

    public MonitorApiController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @PostMapping("/{id}/check")
    public MonitorRecordResponse checkOne(@PathVariable Long id) {
        return monitorService.checkDevice(id);
    }

    @PostMapping("/check-all")
    public CheckAllResponse checkAll() {
        return monitorService.checkAll();
    }

    @PostMapping("/{id}/snmp-check")
    public DeviceResponse checkSnmp(@PathVariable Long id) {
        return monitorService.checkSnmp(id);
    }

    @GetMapping("/{id}/records")
    public PageResponse<MonitorRecordResponse> records(
            @PathVariable Long id,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        Page<MonitorRecordResponse> records = monitorService.getRecords(id, page, size);
        return PageResponse.from(records);
    }

    @GetMapping("/{id}/records/latest")
    public MonitorRecordResponse latest(@PathVariable Long id) {
        return monitorService.getLatest(id);
    }
}
