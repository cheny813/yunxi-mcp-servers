package io.yunxi.mcp.monitoring.tool;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import io.yunxi.mcp.monitoring.service.AlertService;

import java.util.Map;

/**
 * 健康检查工具
 *
 * <p>
 * 支持 HTTP 和 TCP 两种健康检查方式。
 * </p>
 *
 * @author yunxi-mcp-servers
 * @since 1.0.0
 */
public class CheckHealthTool implements ToolHandler {

    private final AlertService alertService;

    public CheckHealthTool(AlertService alertService) {
        this.alertService = alertService;
    }

    @Override
    public ToolDefinition getDefinition() {
        return ToolDefinition.builder()
                .name("check_health")
                .description(
                        "执行健康检查。\n" +
                        "支持 HTTP 健康检查和 TCP 端口连通性检查。\n" +
                        "用于验证服务可用性、诊断连接问题。")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.ofEntries(
                                Map.entry("type", Map.of(
                                        "type", "string",
                                        "description", "检查类型: http 或 tcp，默认 http")),
                                Map.entry("url", Map.of(
                                        "type", "string",
                                        "description", "HTTP 检查的目标 URL（type=http 时必填）")),
                                Map.entry("host", Map.of(
                                        "type", "string",
                                        "description", "TCP 检查的目标主机（type=tcp 时必填）")),
                                Map.entry("port", Map.of(
                                        "type", "number",
                                        "description", "TCP 检查的目标端口（type=tcp 时必填）")),
                                Map.entry("timeoutMs", Map.of(
                                        "type", "number",
                                        "description", "超时时间（毫秒），默认 5000")))))
                .build();
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        String type = (String) arguments.getOrDefault("type", "http");
        int timeoutMs = arguments.containsKey("timeoutMs")
                ? ((Number) arguments.get("timeoutMs")).intValue()
                : 5000;

        AlertService.HealthCheckResult result;

        if ("tcp".equals(type)) {
            String host = (String) arguments.get("host");
            Object portObj = arguments.get("port");
            if (host == null || portObj == null) {
                return ToolResult.error("TCP 检查需要提供 host 和 port 参数");
            }
            int port = ((Number) portObj).intValue();
            result = alertService.checkTcp(host, port, timeoutMs);
        } else {
            String url = (String) arguments.get("url");
            if (url == null) {
                return ToolResult.error("HTTP 检查需要提供 url 参数");
            }
            result = alertService.checkHttp(url, timeoutMs);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## 健康检查结果\n\n");
        sb.append("- **类型**: ").append(result.getType()).append("\n");
        sb.append("- **目标**: ").append(result.getTarget()).append("\n");
        sb.append("- **状态**: ").append(result.isHealthy() ? "健康" : "异常").append("\n");
        sb.append("- **耗时**: ").append(result.getLatencyMs()).append("ms\n");
        if (result.getStatusCode() > 0) {
            sb.append("- **HTTP 状态码**: ").append(result.getStatusCode()).append("\n");
        }
        sb.append("- **消息**: ").append(result.getMessage()).append("\n");
        sb.append("- **时间**: ").append(result.getTimestamp()).append("\n");

        return ToolResult.text(sb.toString());
    }
}
