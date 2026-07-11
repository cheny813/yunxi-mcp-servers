package io.yunxi.mcp.monitoring.tool;

import org.springframework.stereotype.Component;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.monitoring.service.MetricsService;

import java.util.Map;

/**
 * 获取 JVM 指标工具
 *
 * <p>
 * 采集 JVM 堆内存、线程、GC、运行时等指标。
 * </p>
 *
 * @author yunxi-mcp-servers
 * @since 1.0.0
 */
@Component
public class GetJvmMetricsTool implements ToolHandler {

    private final MetricsService metricsService;

    public GetJvmMetricsTool(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @Override
    public String getName() {
        return "get_jvm_metrics";
    }

    @Override
    public String getDescription() {
        return "获取 JVM 运行时指标。\n采集堆内存、非堆内存、线程数、GC 次数和时间、JVM 运行时长等指标。\n用于诊断内存泄漏、线程问题、GC 压力等 JVM 性能问题。";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getInputSchema() {
        return (Map<String, Object>) (Map) Map.of(
                "type", "object",
                "properties", Map.of());
    }

    @Override
    @SuppressWarnings("unchecked")
    public String execute(Map<String, Object> arguments) {
        try {
            Map<String, Object> metrics = metricsService.getJvmMetrics();

            StringBuilder sb = new StringBuilder();
            sb.append("## JVM 指标\n\n");

            for (Map.Entry<String, Object> entry : metrics.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof Map) {
                    sb.append("**").append(entry.getKey()).append("**:\n");
                    for (Map.Entry<String, Object> sub : ((Map<String, Object>) value).entrySet()) {
                        sb.append("  - ").append(sub.getKey()).append(": ").append(sub.getValue()).append("\n");
                    }
                } else {
                    sb.append("- ").append(entry.getKey()).append(": ").append(value).append("\n");
                }
            }

            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("获取 JVM 指标失败: " + e.getMessage());
        }
    }
}