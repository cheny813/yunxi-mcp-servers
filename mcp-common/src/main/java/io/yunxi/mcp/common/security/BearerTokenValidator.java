package io.yunxi.mcp.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Bearer Token (JWT) 验证器
 * <p>
 * 使用 JWT 验证 Bearer Token
 *
 * @since 1.0.0
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "mcp.auth", name = "type", havingValue = "bearer")
public class BearerTokenValidator implements TokenValidator {

    @Autowired
    private McpAuthProperties properties;

    @Override
    public boolean validate(String token) {
        if (!StringUtils.hasText(token)) {
            log.warn("Bearer Token 为空");
            return false;
        }

        String jwtSecret = properties.getJwtSecret();
        if (!StringUtils.hasText(jwtSecret)) {
            log.error("未配置 MCP JWT Secret，请在配置中设置 mcp.auth.jwt-secret");
            return false;
        }

        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // 可以在这里添加额外的 claims 验证
            log.debug("JWT 验证成功，subject: {}", claims.getSubject());
            return true;
        } catch (Exception e) {
            log.warn("JWT 验证失败: {}", e.getMessage());
            return false;
        }
    }
}
