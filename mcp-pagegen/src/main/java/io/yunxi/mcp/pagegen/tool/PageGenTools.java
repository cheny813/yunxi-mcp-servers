package io.yunxi.mcp.pagegen.tool;

import io.yunxi.mcp.pagegen.service.PageGenService;
import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * PageGen MCP 工具集
 * 
 * 提供页面生成能力，包括：
 * - 统计仪表板页面生成
 * - 数据列表页面生成
 * - 详情页面生成
 * 
 * 适用场景：
 * - 数据可视化展示
 * - 后台管理界面生成
 * - 报表页面生成
 * - 快速原型开发
 */
@Slf4j
@Component
public class PageGenTools {

    private final PageGenService pageGenService;

    public PageGenTools(PageGenService pageGenService) {
        this.pageGenService = pageGenService;
    }

    public ToolHandler generateDashboard() {
        return new ToolHandler() {
            @Override
            public ToolDefinition getDefinition() {
                return ToolDefinition.builder()
                        .name("pagegen_dashboard")
                        .description("生成统计仪表板页面。创建包含 KPI 卡片、图表和数据展示的数据可视化仪表板。")
                        .inputSchema(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "title", Map.of(
                                                "type", "string",
                                                "description", "仪表板标题，例如：销售数据统计、系统监控面板"),
                                        "kpi_cards", Map.of(
                                                "type", "array",
                                                "description",
                                                "KPI 卡片列表，每个卡片包含：label（标签）、value（数值）、unit（单位）、trend（趋势：up/down）",
                                                "items", Map.of(
                                                        "type", "object",
                                                        "properties", Map.of(
                                                                "label",
                                                                Map.of("type", "string", "description", "指标名称"),
                                                                "value", Map.of("type", "string", "description", "指标值"),
                                                                "unit", Map.of("type", "string", "description", "单位"),
                                                                "trend",
                                                                Map.of("type", "string", "enum", List.of("up", "down"),
                                                                        "description", "趋势方向")))),
                                        "charts", Map.of(
                                                "type", "array",
                                                "description", "图表配置列表，每个图表包含：type（类型：line/bar/pie）、title（标题）、data（数据）",
                                                "items", Map.of(
                                                        "type", "object",
                                                        "properties", Map.of(
                                                                "type",
                                                                Map.of("type", "string", "enum",
                                                                        List.of("line", "bar", "pie"), "description",
                                                                        "图表类型"),
                                                                "title",
                                                                Map.of("type", "string", "description", "图表标题"),
                                                                "data",
                                                                Map.of("type", "object", "description", "图表数据"))))),
                                "required", List.of("title")))
                        .build();
            }

            @Override
            public ToolResult execute(Map<String, Object> arguments) {
                try {
                    String html = pageGenService.generateDashboard(arguments);
                    return ToolResult.text("仪表板页面生成成功！HTML 长度: " + html.length() + " 字符");
                } catch (Exception e) {
                    log.error("生成仪表板页面失败", e);
                    return ToolResult.error("生成失败: " + e.getMessage());
                }
            }
        };
    }

    public ToolHandler generateList() {
        return new ToolHandler() {
            @Override
            public ToolDefinition getDefinition() {
                return ToolDefinition.builder()
                        .name("pagegen_list")
                        .description("生成数据列表页面。创建包含表格、搜索和分页功能的数据展示页面。")
                        .inputSchema(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "title", Map.of(
                                                "type", "string",
                                                "description", "列表页面标题，例如：用户列表、订单管理"),
                                        "columns", Map.of(
                                                "type", "array",
                                                "description",
                                                "表格列定义，每列包含：field（字段名）、label（列标题）、width（宽度）、sortable（是否可排序）",
                                                "items", Map.of(
                                                        "type", "object",
                                                        "properties", Map.of(
                                                                "field",
                                                                Map.of("type", "string", "description", "数据字段名"),
                                                                "label", Map.of("type", "string", "description", "列标题"),
                                                                "width",
                                                                Map.of("type", "number", "description", "列宽度（像素）"),
                                                                "sortable",
                                                                Map.of("type", "boolean", "description", "是否可排序")))),
                                        "data", Map.of(
                                                "type", "array",
                                                "description", "表格数据行，每行是一个对象，键为字段名，值为数据")),
                                "required", List.of("title", "columns")))
                        .build();
            }

            @Override
            public ToolResult execute(Map<String, Object> arguments) {
                try {
                    String html = pageGenService.generateListPage(arguments);
                    return ToolResult.text("列表页面生成成功！HTML 长度: " + html.length() + " 字符");
                } catch (Exception e) {
                    log.error("生成列表页面失败", e);
                    return ToolResult.error("生成失败: " + e.getMessage());
                }
            }
        };
    }

    public ToolHandler generateDetail() {
        return new ToolHandler() {
            @Override
            public ToolDefinition getDefinition() {
                return ToolDefinition.builder()
                        .name("pagegen_detail")
                        .description("生成详情页面。创建展示单个实体详细信息的数据详情页面。")
                        .inputSchema(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "title", Map.of(
                                                "type", "string",
                                                "description", "详情页面标题，例如：用户详情、订单详情"),
                                        "entity", Map.of(
                                                "type", "object",
                                                "description",
                                                "实体数据对象，包含要展示的所有字段信息，例如：{name: '张三', age: 25, email: 'zhangsan@example.com'}"),
                                        "related_data", Map.of(
                                                "type", "array",
                                                "description", "关联数据列表，用于展示与实体相关的其他信息")),
                                "required", List.of("title", "entity")))
                        .build();
            }

            @Override
            public ToolResult execute(Map<String, Object> arguments) {
                try {
                    String html = pageGenService.generateDetailPage(arguments);
                    return ToolResult.text("详情页面生成成功！HTML 长度: " + html.length() + " 字符");
                } catch (Exception e) {
                    log.error("生成详情页面失败", e);
                    return ToolResult.error("生成失败: " + e.getMessage());
                }
            }
        };
    }
}
