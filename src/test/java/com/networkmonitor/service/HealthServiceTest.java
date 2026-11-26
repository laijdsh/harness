package com.networkmonitor.service;

import com.networkmonitor.dto.HealthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * HealthService 单元测试。
 *
 * <p>本测试不启动 Spring 容器、不连接真实 MySQL，
 * 而是用 Mockito 模拟 JdbcTemplate，专门验证健康检查的判断逻辑。
 */
@ExtendWith(MockitoExtension.class)
class HealthServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private HealthService healthService;

    @BeforeEach
    void setUp() {
        healthService = new HealthService(jdbcTemplate, "test-application", "0.0.1");
    }

    @Test
    void shouldReturnUpWhenDatabaseIsAvailable() {
        // 模拟 SELECT 1 返回 1，代表数据库可用
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);

        HealthResponse response = healthService.check();

        assertEquals("UP", response.getStatus());
        assertEquals("UP", response.getDatabase());
        assertEquals("test-application", response.getApplication());
        assertEquals("0.0.1", response.getVersion());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void shouldReturnDownWhenDatabaseIsUnavailable() {
        // 模拟数据库访问失败
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class))
                .thenThrow(new DataAccessResourceFailureException("database unavailable"));

        HealthResponse response = healthService.check();

        assertEquals("DOWN", response.getStatus());
        assertEquals("DOWN", response.getDatabase());
        assertNotNull(response.getTimestamp());
    }
}
