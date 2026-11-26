package com.networkmonitor.controller.api;

import com.networkmonitor.exception.DeviceNotFoundException;
import com.networkmonitor.exception.DuplicateIpException;
import com.networkmonitor.exception.MonitorRecordNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST API 的统一异常处理。
 *
 * <p>使用 Spring 的 ProblemDetail（RFC 9457）返回结构化错误，
 * 让前端可以根据 status、title、detail 和 errors 做不同处理。
 *
 * <p>只作用于 controller.api 包，不影响 Thymeleaf 页面控制器。
 */
@RestControllerAdvice(basePackageClasses = DeviceApiController.class)
public class ApiExceptionHandler {

    @ExceptionHandler(DeviceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(DeviceNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "设备不存在", ex.getMessage());
    }

    @ExceptionHandler(DuplicateIpException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateIp(DuplicateIpException ex) {
        return problem(HttpStatus.CONFLICT, "IP 地址冲突", ex.getMessage());
    }

    @ExceptionHandler(MonitorRecordNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleMonitorRecordNotFound(MonitorRecordNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "暂无监测记录", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage())
        );

        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "请求参数校验失败"
        );
        detail.setTitle("参数校验失败");
        detail.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(detail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadable(HttpMessageNotReadableException ex) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "请求体格式错误",
                "请检查 JSON 格式和字段类型，例如 deviceType 必须是 ROUTER/SWITCH/SERVER/OTHER"
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrity(DataIntegrityViolationException ex) {
        return problem(
                HttpStatus.CONFLICT,
                "数据冲突",
                "数据违反唯一性约束，可能是 IP 地址已被其他设备使用"
        );
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return ResponseEntity.status(status).body(problem);
    }
}
