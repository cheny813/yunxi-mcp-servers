package io.yunxi.mcp.common.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * MCP 认证配置属性
 * <p>
 * 配置示例：
 * <pre>{@code
 * mcp:
 *   auth:
 *     enabled: true
 *     type: api-token
 *     token: ${MCP_API_TOKEN:your-token}
 *     whitelist:
 *       - /mcp/health
 *       - /mcp/info
 * }</pre>
 *
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "mcp.auth")
public class McpAuthProperties {

    /**
     * 是否启用认证
     */
    private boolean enabled = false;

    /**
     * 认证类型
     */
    private AuthType type = AuthType.API_TOKEN;

    /**
     * API Token（当 type=api-token 时使用）
     */
    private String token;

    /**
     * JWT Secret（当 type=bearer 时使用）
     */
    private String jwtSecret;

    /**
     * 白名单路径（无需认证）
     */
    private List<String> whitelist = List.of("/mcp/health", "/mcp/info");

    /**
     * 认证头名称
     */
    private String headerName = "X-MCP-Token";

    /**
     * Bearer Token 头前缀
     */
    private String bearerPrefix = "Bearer ";

    /**
     * 认证类型枚举
     */
    public enum AuthType {
        /**
         * 简单 API Token
         */
        API_TOKEN,
        /**
         * Bearer Token (JWT)
         */
        BEARER,
        /**
         * 不认证
         */
        NONE
    }
}
