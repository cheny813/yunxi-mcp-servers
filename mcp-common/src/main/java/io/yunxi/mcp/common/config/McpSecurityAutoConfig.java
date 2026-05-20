package io.yunxi.mcp.common.config;

import io.yunxi.mcp.common.security.McpAuthFilter;
import io.yunxi.mcp.common.security.McpAuthProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP 安全配置自动装配
 * <p>
 * 自动配置 MCP 认证过滤器
 *
 * @since 1.0.0
 */
@Configuration
@ConditionalOnClass(McpAuthFilter.class)
@EnableConfigurationProperties(McpAuthProperties.class)
public class McpSecurityAutoConfig {

    /**
     * 注册 MCP 认证过滤器
     * <p>
     * 过滤器顺序：在 Spring Security 之后执行
     */
    @Bean
    @ConditionalOnProperty(prefix = "mcp.auth", name = "enabled", havingValue = "true")
    public FilterRegistrationBean<McpAuthFilter> mcpAuthFilterRegistration(McpAuthFilter filter) {
        FilterRegistrationBean<McpAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/mcp/*");
        registration.setOrder(100);  // 在 Spring Security 过滤器之后
        registration.setName("mcpAuthFilter");
        return registration;
    }
}
