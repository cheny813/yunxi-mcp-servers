package io.yunxi.mcp.monitoring.tool;

import org.springframework.stereotype.Component;

import io.yunxi.mcp.common.handler.ToolHandler;
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
@Component
public class GetSystemMetricsTool implements ToolHandler {

    private final MetricsService metricsService;

    public GetSystemMetricsTool(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @Override
    public String getName() {
        return "get_system_metrics";
    }

    @Override
    public String getDescription() {
        return "获取系统运行指标。\n采集 CPU、内存、磁盘、网络等操作系统级别的运行指标数据。\n用于监控服务器健康状况、诊断性能问题、容量规划。";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getInputSchema() {
        return (Map<String, Object>) (Map) Map.of(
                "type", "object",
                "properties", Map.of(
                        "metrics", Map.of(
                                "type", "array",
                                "description", "要获取的指标类别，可选值: cpu, memory, disk, network, os。不传则返回全部",
                                "items", Map.of("type", "string"))));
    }

    @Override
    @SuppressWarnings("unchecked")
    public String execute(Map<String, Object> arguments) {
        List<String> metricNames = arguments.containsKey("metrics")
                ? (List<String>) arguments.get("metrics")
                : List.of();

        try {
            Map<String, Object> metrics = metricsService.getSystemMetrics(metricNames);

            StringBuilder sb = new StringBuilder();
            sb.append("## 系统指标\n\n");

            formatMetrics(sb, metrics, 0);

            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("获取系统指标失败: " + e.getMessage());
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