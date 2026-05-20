package io.yunxi.mcp.common.tracing;

import io.yunxi.mcp.common.protocol.McpRequest;
import io.yunxi.mcp.common.protocol.McpResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * MCP 客户端 TraceId 拦截器
 * <p>
 * 在调用 MCP 服务时自动将当前 TraceId 添加到请求头中，
 * 实现跨服务的 TraceId 传递。
 * </p>
 */
public class McpTraceIdInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {

        // 获取当前 TraceId 并添加到请求头
        String traceId = TraceContext.getTraceId();
        if (traceId != null) {
            request.getHeaders().add(TraceContext.TRACE_ID_HEADER, traceId);
        }

        return execution.execute(request, body);
    }
}
