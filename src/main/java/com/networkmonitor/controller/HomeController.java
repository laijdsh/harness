package com.networkmonitor.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 首页控制器。
 *
 * <p>负责把页面需要的少量数据放入 Model，再交给 Thymeleaf 渲染 index.html。
 * 当前阶段首页只展示系统信息、健康状态入口和后续模块占位。
 */
@Controller
public class HomeController {

    private static final String SYSTEM_NAME = "轻量级网络设备状态监测与异常告警系统";
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String applicationVersion;

    public HomeController(@Value("${app.version}") String applicationVersion) {
        this.applicationVersion = applicationVersion;
    }

    @GetMapping({"/", "/index"})
    public String index(Model model) {
        model.addAttribute("systemName", SYSTEM_NAME);
        model.addAttribute("appVersion", applicationVersion);
        model.addAttribute("serverTime", LocalDateTime.now().format(TIME_FORMATTER));
        return "index";
    }
}
