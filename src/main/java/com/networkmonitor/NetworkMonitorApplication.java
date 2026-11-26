package com.networkmonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 系统启动类。
 *
 * <p>@SpringBootApplication 是一个组合注解，包含：
 * <ul>
 *     <li>@SpringBootConfiguration：声明这是一个 Spring Boot 配置类；</li>
 *     <li>@EnableAutoConfiguration：让 Spring Boot 根据依赖自动配置 Tomcat、JPA 等；</li>
 *     <li>@ComponentScan：扫描 com.networkmonitor 及其子包中的 Controller、Service 等组件。</li>
 * </ul>
 *
 * <p>因此本项目所有业务代码都应放在 com.networkmonitor 的子包中。
 */
@SpringBootApplication
public class NetworkMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(NetworkMonitorApplication.class, args);
    }
}
