package com.networkmonitor.controller;

import com.networkmonitor.dto.HealthResponse;
import com.networkmonitor.service.HealthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查接口。
 *
 * <p>接口地址：GET /api/health
 *
 * <p>返回 200 表示应用和数据库都正常；
 * 返回 503 表示应用还能响应，但数据库不可用。
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HealthResponse> health() {
        HealthResponse response = healthService.check();

        HttpStatus httpStatus = "UP".equals(response.getStatus())
                ? HttpStatus.OK
                : HttpStatus.SERVICE_UNAVAILABLE;

        return ResponseEntity.status(httpStatus).body(response);
    }
}
