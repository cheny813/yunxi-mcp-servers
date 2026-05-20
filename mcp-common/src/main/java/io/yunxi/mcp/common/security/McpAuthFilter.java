package io.yunxi.mcp.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * MCP 统一鉴权过滤器
 * <p>
 * 拦截所有 /mcp/* 请求，进行统一认证验证
 *
 * @since 1.0.0
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "mcp.auth", name = "enabled", havingValue = "true")
public class McpAuthFilter extends OncePerRequestFilter {

    @Autowired
    private McpAuthProperties properties;

    @Autowired
    private TokenValidator tokenValidator;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestUri = request.getRequestURI();

        // 1. 检查白名单
        if (isWhitelisted(requestUri)) {
            log.debug("请求 {} 在白名单中，跳过认证", requestUri);
            filterChain.doFilter(request, response);
            return;
        }

        // 2. 提取 Token
        String token = extractToken(request);
        if (!StringUtils.hasText(token)) {
            log.warn("请求 {} 缺少认证令牌", requestUri);
            writeErrorResponse(response, HttpStatus.UNAUTHORIZED, "Missing authentication token");
            return;
        }

        // 3. 验证 Token
        if (!tokenValidator.validate(token)) {
            log.warn("请求 {} 认证失败", requestUri);
            writeErrorResponse(response, HttpStatus.UNAUTHORIZED, "Invalid authentication token");
            return;
        }

        // 4. 认证通过，继续处理
        log.debug("请求 {} 认证通过", requestUri);
        filterChain.doFilter(request, response);
    }

    /**
     * 检查请求路径是否在白名单中
     */
    private boolean isWhitelisted(String requestUri) {
        if (properties.getWhitelist() == null || properties.getWhitelist().isEmpty()) {
            return false;
        }

        for (String pattern : properties.getWhitelist()) {
            if (pathMatcher.match(pattern, requestUri)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从请求中提取 Token
     */
    private String extractToken(HttpServletRequest request) {
        McpAuthProperties.AuthType authType = properties.getType();

        if (authType == McpAuthProperties.AuthType.BEARER) {
            // 从 Authorization 头提取 Bearer Token
            String authHeader = request.getHeader("Authorization");
            if (StringUtils.hasText(authHeader) && authHeader.startsWith(properties.getBearerPrefix())) {
                return authHeader.substring(properties.getBearerPrefix().length());
            }
        } else {
            // 从自定义头提取 API Token
            String tokenHeader = request.getHeader(properties.getHeaderName());
            if (StringUtils.hasText(tokenHeader)) {
                return tokenHeader;
            }

            // 尝试从 Authorization 头提取（兼容 Bearer 格式）
            String authHeader = request.getHeader("Authorization");
            if (StringUtils.hasText(authHeader) && authHeader.startsWith(properties.getBearerPrefix())) {
                return authHeader.substring(properties.getBearerPrefix().length());
            }
        }

        return null;
    }

    /**
     * 写入错误响应
     */
    private void writeErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> error = new HashMap<>();
        error.put("error", status.getReasonPhrase());
        error.put("message", message);
        error.put("status", status.value());

        objectMapper.writeValue(response.getWriter(), error);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 只过滤 /mcp/* 路径
        String requestUri = request.getRequestURI();
        return !requestUri.startsWith("/mcp");
    }
}
