package com.networkmonitor.controller.api;

import com.networkmonitor.dto.DeviceRequest;
import com.networkmonitor.dto.DeviceResponse;
import com.networkmonitor.dto.PageResponse;
import com.networkmonitor.entity.DeviceType;
import com.networkmonitor.entity.NetworkDevice;
import com.networkmonitor.service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * 设备管理 REST API。
 *
 * <p>Base URL：/api/devices
 *
 * <p>接口列表：
 * GET    /api/devices          分页查询，可用 keyword、type、page、size
 * GET    /api/devices/{id}     查询单台设备
 * POST   /api/devices          新增设备
 * PUT    /api/devices/{id}     修改设备
 * DELETE /api/devices/{id}     删除设备
 */
@RestController
@RequestMapping("/api/devices")
public class DeviceApiController {

    private final DeviceService deviceService;

    public DeviceApiController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    public PageResponse<DeviceResponse> list(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "type", required = false) DeviceType type,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        Page<DeviceResponse> result = deviceService.search(keyword, type, page, size)
                .map(DeviceResponse::from);
        return PageResponse.from(result);
    }

    @GetMapping("/{id}")
    public DeviceResponse get(@PathVariable Long id) {
        return DeviceResponse.from(deviceService.get(id));
    }

    @PostMapping
    public ResponseEntity<DeviceResponse> create(@Valid @RequestBody DeviceRequest request) {
        NetworkDevice created = deviceService.create(request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();

        return ResponseEntity.created(location).body(DeviceResponse.from(created));
    }

    @PutMapping("/{id}")
    public DeviceResponse update(@PathVariable Long id,
                                 @Valid @RequestBody DeviceRequest request) {
        return DeviceResponse.from(deviceService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        deviceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
