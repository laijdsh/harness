package com.networkmonitor.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 开启 Spring 定时任务支持。
 *
 * <p>阶段 3 的 MonitorScheduler 使用 @Scheduled 定期检测设备。
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
