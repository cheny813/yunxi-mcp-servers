package io.yunxi.mcp.monitoring.tool;

import org.springframework.stereotype.Component;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.monitoring.service.AlertService;

import java.util.List;
import java.util.Map;

/**
 * 设置告警规则工具
 *
 * <p>
 * 创建或更新告警规则，当指标超过阈值时自动触发告警。
 * </p>
 *
 * @author yunxi-mcp-servers
 * @since 1.0.0
 */
@Component
public class SetAlertRuleTool implements ToolHandler {

    private final AlertService alertService;

    public SetAlertRuleTool(AlertService alertService) {
        this.alertService = alertService;
    }

    @Override
    public String getName() {
        return "set_alert_rule";
    }

    @Override
    public String getDescription() {
        return "设置告警规则。\n创建或更新指标阈值告警规则，当指标值超过设定阈值时自动触发告警。\n支持大于、小于、大于等于、小于等于、等于等比较方式。";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getInputSchema() {
        return (Map<String, Object>) (Map) Map.of(
                "type", "object",
                "properties", Map.of(
                        "name", Map.of(
                                "type", "string",
                                "description", "告警规则名称"),
                        "metricName", Map.of(
                                "type", "string",
                                "description", "监控指标名称，如 cpu_usage, memory_usage, disk_usage_percent"),
                        "threshold", Map.of(
                                "type", "number",
                                "description", "告警阈值"),
                        "operator", Map.of(
                                "type", "string",
                                "description", "比较运算符: gt(大于), lt(小于), gte(大于等于), lte(小于等于), eq(等于)。默认 gt"),
                        "durationSec", Map.of(
                                "type", "number",
                                "description", "持续超过阈值多少秒后触发告警，默认 60"),
                        "description", Map.of(
                                "type", "string",
                                "description", "规则描述")),
                "required", List.of("name", "metricName", "threshold"));
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        String name = (String) arguments.get("name");
        String metricName = (String) arguments.get("metricName");

        if (name == null || metricName == null) {
            throw new RuntimeException("缺少必填参数: name 和 metricName");
        }

        double threshold = arguments.containsKey("threshold")
                ? ((Number) arguments.get("threshold")).doubleValue()
                : 0;

        String operator = (String) arguments.getOrDefault("operator", "gt");
        int durationSec = arguments.containsKey("durationSec")
                ? ((Number) arguments.get("durationSec")).intValue()
                : 60;
        String description = (String) arguments.get("description");

        try {
            String ruleId = alertService.setAlertRule(name, metricName, threshold, operator, durationSec, description);

            StringBuilder sb = new StringBuilder();
            sb.append("## 告警规则已创建\n\n");
            sb.append("- **规则 ID**: ").append(ruleId).append("\n");
            sb.append("- **名称**: ").append(name).append("\n");
            sb.append("- **指标**: ").append(metricName).append("\n");
            sb.append("- **阈值**: ").append(threshold).append("\n");
            sb.append("- **比较方式**: ").append(operator).append("\n");
            sb.append("- **持续时间**: ").append(durationSec).append("s\n");
            if (description != null) {
                sb.append("- **描述**: ").append(description).append("\n");
            }

            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("设置告警规则失败: " + e.getMessage());
        }
    }
}