package com.networkmonitor.scheduler;

import com.networkmonitor.dto.CheckAllResponse;
import com.networkmonitor.service.MonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时检测任务。
 *
 * <p>默认每 60 秒执行一次全部设备检测，应用启动后延迟 15 秒开始，
 * 避免和启动过程、手动测试争抢资源。
 *
 * <p>可以通过配置关闭：monitor.scheduler.enabled=false。
 */
@Component
@ConditionalOnProperty(
        name = "monitor.scheduler.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class MonitorScheduler {

    private static final Logger log = LoggerFactory.getLogger(MonitorScheduler.class);

    private final MonitorService monitorService;

    public MonitorScheduler(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @Scheduled(
            fixedDelayString = "${monitor.interval-ms:60000}",
            initialDelayString = "${monitor.initial-delay-ms:15000}"
    )
    public void checkAllDevices() {
        try {
            CheckAllResponse summary = monitorService.checkAll();
            log.info("定时检测任务完成：总数={}，在线={}，离线={}，异常={}",
                    summary.getTotal(), summary.getOnline(),
                    summary.getOffline(), summary.getError());
        } catch (Exception ex) {
            // 定时任务必须自己吞掉异常，否则一次失败可能影响后续调度。
            log.error("定时检测任务执行失败", ex);
        }
    }
}
