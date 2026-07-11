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
 * MCP 安全配置自动装配 (v2.0)
 * <p>
 * 适配官方 MCP SDK 的 SSE 端点路径：
 * <ul>
 * <li>{@code /mcp/sse} - SSE 连接建立</li>
 * <li>{@code /mcp/message} - JSON-RPC 消息端点</li>
 * </ul>
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
     * 拦截官方 MCP SDK 的 SSE 端点路径
     */
    @Bean
    @ConditionalOnProperty(prefix = "mcp.auth", name = "enabled", havingValue = "true")
    public FilterRegistrationBean<McpAuthFilter> mcpAuthFilterRegistration(McpAuthFilter filter) {
        FilterRegistrationBean<McpAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/mcp/*");
        registration.setOrder(100);
        registration.setName("mcpAuthFilter");
        return registration;
    }
}
