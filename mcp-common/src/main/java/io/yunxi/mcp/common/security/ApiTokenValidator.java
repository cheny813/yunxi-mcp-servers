package io.yunxi.mcp.common.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * API Token 验证器
 * <p>
 * 简单的 Token 字符串匹配验证
 *
 * @since 1.0.0
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "mcp.auth", name = "type", havingValue = "api-token")
public class ApiTokenValidator implements TokenValidator {

    @Autowired
    private McpAuthProperties properties;

    @Override
    public boolean validate(String token) {
        if (!StringUtils.hasText(token)) {
            log.warn("API Token 为空");
            return false;
        }

        String expectedToken = properties.getToken();
        if (!StringUtils.hasText(expectedToken)) {
            log.error("未配置 MCP API Token，请在配置中设置 mcp.auth.token");
            return false;
        }

        boolean valid = expectedToken.equals(token);
        if (!valid) {
            log.warn("API Token 验证失败");
        }
        return valid;
    }
}
