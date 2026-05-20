package io.yunxi.mcp.common.tracing;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * TraceId 过滤器
 * <p>
 * 从 HTTP 请求头中提取 TraceId 并设置到 MDC 中，
 * 同时在响应头中返回 TraceId 便于客户端追踪。
 * </p>
 * <p>
 * 优先级设置为最高，确保在其他过滤器之前执行。
 * </p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 从请求头中提取 TraceId，不存在则生成新的
        String traceId = httpRequest.getHeader(TraceContext.TRACE_ID_HEADER);
        traceId = TraceContext.setTraceIdOrGenerate(traceId);

        try {
            // 在响应头中返回 TraceId
            httpResponse.setHeader(TraceContext.TRACE_ID_HEADER, traceId);

            chain.doFilter(request, response);
        } finally {
            // 清理 MDC 防止内存泄漏
            TraceContext.clear();
        }
    }
}
