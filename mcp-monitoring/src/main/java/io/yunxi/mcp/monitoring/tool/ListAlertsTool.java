package io.yunxi.mcp.monitoring.tool;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import io.yunxi.mcp.monitoring.service.AlertService;

import java.util.List;
import java.util.Map;

/**
 * 列出告警工具
 *
 * <p>
 * 查询告警历史记录，支持按级别和指标名称过滤。
 * </p>
 *
 * @author yunxi-mcp-servers
 * @since 1.0.0
 */
public class ListAlertsTool implements ToolHandler {

    private final AlertService alertService;

    public ListAlertsTool(AlertService alertService) {
        this.alertService = alertService;
    }

    @Override
    public ToolDefinition getDefinition() {
        return ToolDefinition.builder()
                .name("list_alerts")
                .description(
                        "查询告警历史记录。\n" +
                        "支持按告警级别和指标名称过滤，按时间倒序排列。\n" +
                        "用于回顾系统异常历史、分析问题趋势。")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "level", Map.of(
                                        "type", "string",
                                        "description", "告警级别过滤: warning, critical, info"),
                                "metricName", Map.of(
                                        "type", "string",
                                        "description", "按指标名称过滤"),
                                "limit", Map.of(
                                        "type", "number",
                                        "description", "最大返回条数，默认 50"))))
                .build();
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        String level = (String) arguments.get("level");
        String metricName = (String) arguments.get("metricName");
        int limit = arguments.containsKey("limit")
                ? ((Number) arguments.get("limit")).intValue()
                : 50;

        try {
            List<AlertService.AlertRecord> alerts = alertService.getAlerts(level, metricName, limit);

            if (alerts.isEmpty()) {
                return ToolResult.text("暂无告警记录");
            }

            StringBuilder sb = new StringBuilder();
            sb.append("## 告警列表 (").append(alerts.size()).append(" 条)\n\n");

            for (AlertService.AlertRecord alert : alerts) {
                sb.append("- **").append(alert.getRuleName()).append("** [").append(alert.getLevel()).append("]\n");
                sb.append("  - 指标: ").append(alert.getMetricName())
                        .append(" | 当前值: ").append(alert.getCurrentValue())
                        .append(" | 阈值: ").append(alert.getThreshold()).append("\n");
                sb.append("  - 消息: ").append(alert.getMessage()).append("\n");
                sb.append("  - 时间: ").append(alert.getTimestamp())
                        .append(" | 已确认: ").append(alert.isAcknowledged() ? "是" : "否").append("\n\n");
            }

            return ToolResult.text(sb.toString());
        } catch (Exception e) {
            return ToolResult.error("查询告警失败: " + e.getMessage());
        }
    }
}
