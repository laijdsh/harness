package com.networkmonitor.service;

import com.networkmonitor.dto.HealthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 健康检查业务逻辑。
 *
 * <p>本阶段检查两件事：
 * <ol>
 *     <li>应用本身是否已经启动并能够处理请求；</li>
 *     <li>MySQL 是否能够通过 JDBC 执行最简单的 SELECT 1。</li>
 * </ol>
 *
 * <p>把数据库检查放在 Service 层，而不是直接写在 Controller 中，
 * 这样后续可以复用，也便于编写单元测试。
 */
@Service
public class HealthService {

    private static final Logger log = LoggerFactory.getLogger(HealthService.class);

    private final JdbcTemplate jdbcTemplate;
    private final String applicationName;
    private final String applicationVersion;

    public HealthService(JdbcTemplate jdbcTemplate,
                         @Value("${spring.application.name}") String applicationName,
                         @Value("${app.version}") String applicationVersion) {
        this.jdbcTemplate = jdbcTemplate;
        this.applicationName = applicationName;
        this.applicationVersion = applicationVersion;
    }

    /**
     * 执行一次健康检查。
     *
     * @return 健康检查结果，包含总体状态、应用信息、数据库状态和检查时间
     */
    public HealthResponse check() {
        String databaseStatus = "DOWN";

        try {
            // SELECT 1 是最轻量的数据库连通性检查，不依赖任何业务表。
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            if (result != null && result == 1) {
                databaseStatus = "UP";
            }
        } catch (Exception ex) {
            // 健康检查失败时只记录日志，不把数据库异常细节返回给前端。
            log.warn("数据库健康检查失败: {}", ex.getMessage());
        }

        String overallStatus = "UP".equals(databaseStatus) ? "UP" : "DOWN";

        return new HealthResponse(
                overallStatus,
                applicationName,
                applicationVersion,
                databaseStatus,
                LocalDateTime.now()
        );
    }
}
