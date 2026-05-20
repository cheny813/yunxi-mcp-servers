package io.yunxi.mcp.monitoring.tool;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import io.yunxi.mcp.monitoring.service.MetricsService;

import java.util.List;
import java.util.Map;

/**
 * 获取系统指标工具
 *
 * <p>
 * 采集 CPU、内存、磁盘、网络等操作系统级运行指标。
 * </p>
 *
 * @author yunxi-mcp-servers
 * @since 1.0.0
 */
public class GetSystemMetricsTool implements ToolHandler {

    private final MetricsService metricsService;

    public GetSystemMetricsTool(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @Override
    public ToolDefinition getDefinition() {
        return ToolDefinition.builder()
                .name("get_system_metrics")
                .description(
                        "获取系统运行指标。\n" +
                        "采集 CPU、内存、磁盘、网络等操作系统级别的运行指标数据。\n" +
                        "用于监控服务器健康状况、诊断性能问题、容量规划。")
                .inputSchema(Map.ofEntries(
                        Map.entry("type", "object"),
                        Map.entry("properties", Map.of(
                                "metrics", Map.ofEntries(
                                        Map.entry("type", "array"),
                                        Map.entry("description", "要获取的指标类别，可选值: cpu, memory, disk, network, os。不传则返回全部"),
                                        Map.entry("items", Map.of("type", "string")))))))
                .build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public ToolResult execute(Map<String, Object> arguments) {
        List<String> metricNames = arguments.containsKey("metrics")
                ? (List<String>) arguments.get("metrics")
                : List.of();

        try {
            Map<String, Object> metrics = metricsService.getSystemMetrics(metricNames);

            StringBuilder sb = new StringBuilder();
            sb.append("## 系统指标\n\n");

            formatMetrics(sb, metrics, 0);

            return ToolResult.text(sb.toString());
        } catch (Exception e) {
            return ToolResult.error("获取系统指标失败: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void formatMetrics(StringBuilder sb, Map<String, Object> data, int indent) {
        String prefix = "  ".repeat(indent);
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                sb.append(prefix).append("**").append(entry.getKey()).append("**:\n");
                formatMetrics(sb, (Map<String, Object>) value, indent + 1);
            } else if (value instanceof List) {
                sb.append(prefix).append("**").append(entry.getKey()).append("**: ").append(value).append("\n");
            } else {
                sb.append(prefix).append("- ").append(entry.getKey()).append(": ").append(value).append("\n");
            }
        }
    }
}
