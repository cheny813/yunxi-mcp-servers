package io.yunxi.mcp.chart.tool;

import io.yunxi.mcp.chart.service.ChartService;
import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 图表生成工具集合
 * <p>
 * 提供多种图表类型的生成能力，支持柱状图、折线图、饼图等。
 * 每个工具都实现了 ToolHandler 接口，可以注册到 MCP 服务器。
 * </p>
 */
@Slf4j
@Component
public class ChartTools {

    private final ChartService chartService;

    public ChartTools(ChartService chartService) {
        this.chartService = chartService;
    }

    /**
     * 柱状图工具
     */
    public ToolHandler barChart() {
        return new ToolHandler() {
            @Override
            public ToolDefinition getDefinition() {
                return ToolDefinition.builder()
                        .name("chart_bar")
                        .description(
                                "Generate a bar chart for comparing data across categories. " +
                                "生成柱状图用于对比不同类别的数据。 " +
                                "Use this when you need to compare multiple data points. " +
                                "适用于需要对比多个数据点的场景。 " +
                                "Common use cases: sales comparison, performance metrics, survey results. " +
                                "典型用例：销售对比、性能指标、调查结果。"
                        )
                        .inputSchema(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "title", Map.of(
                                                "type", "string",
                                                "description", "图表标题 (Chart title)"
                                        ),
                                        "categories", Map.of(
                                                "type", "array",
                                                "items", Map.of("type", "string"),
                                                "description", "X 轴分类 (X-axis categories)"
                                        ),
                                        "series", Map.of(
                                                "type", "array",
                                                "items", Map.of(
                                                        "type", "object",
                                                        "properties", Map.of(
                                                                "name", Map.of("type", "string"),
                                                                "data", Map.of(
                                                                        "type", "array",
                                                                        "items", Map.of("type", "number")
                                                                )
                                                        ),
                                                        "required", java.util.List.of("name", "data")
                                                ),
                                                "description", "系列数据 (Series data)"
                                        ),
                                        "colors", Map.of(
                                                "type", "array",
                                                "items", Map.of("type", "string"),
                                                "description", "颜色主题 (Color theme, optional)"
                                        )
                                ),
                                "required", java.util.List.of("categories", "series")
                        ))
                        .build();
            }

            @Override
            public ToolResult execute(Map<String, Object> arguments) {
                try {
                    log.debug("执行柱状图生成: args={}", arguments);

                    ChartService.ChartConfig config = new ChartService.ChartConfig();
                    config.setType("bar");
                    config.setTitle((String) arguments.get("title"));
                    config.setXAxis(((java.util.List<?>) arguments.get("categories")).toArray());
                    config.setSeriesData((java.util.List<Map<String, Object>>) arguments.get("series"));

                    ChartService.ChartResult result = chartService.generateChart(config);

                    return ToolResult.text(String.format(
                            "图表生成成功！\n" +
                            "图表ID: %s\n" +
                            "文件URL: http://localhost:40302%s\n" +
                            "Base64数据: %s",
                            result.getChartId(),
                            result.getFileUrl(),
                            result.getBase64Data().substring(0, Math.min(100, result.getBase64Data().length())) + "..."
                    ));

                } catch (Exception e) {
                    log.error("柱状图生成失败: {}", e.getMessage(), e);
                    return ToolResult.error("图表生成失败: " + e.getMessage());
                }
            }
        };
    }

    /**
     * 折线图工具
     */
    public ToolHandler lineChart() {
        return new ToolHandler() {
            @Override
            public ToolDefinition getDefinition() {
                return ToolDefinition.builder()
                        .name("chart_line")
                        .description(
                                "Generate a line chart to show trends over time. " +
                                "生成折线图用于展示数据随时间的变化趋势。 " +
                                "Use this when you need to visualize data changes over time. " +
                                "适用于需要展示数据随时间变化的场景。 " +
                                "Common use cases: sales trends, temperature monitoring, stock prices. " +
                                "典型用例：销售趋势、温度监控、股票价格。"
                        )
                        .inputSchema(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "title", Map.of(
                                                "type", "string",
                                                "description", "图表标题 (Chart title)"
                                        ),
                                        "xAxis", Map.of(
                                                "type", "array",
                                                "items", Map.of("type", "string"),
                                                "description", "X 轴数据，通常是时间点 (X-axis data, usually time points)"
                                        ),
                                        "series", Map.of(
                                                "type", "array",
                                                "items", Map.of(
                                                        "type", "object",
                                                        "properties", Map.of(
                                                                "name", Map.of("type", "string"),
                                                                "data", Map.of(
                                                                        "type", "array",
                                                                        "items", Map.of("type", "number")
                                                                )
                                                        ),
                                                        "required", java.util.List.of("name", "data")
                                                ),
                                                "description", "系列数据 (Series data)"
                                        ),
                                        "colors", Map.of(
                                                "type", "array",
                                                "items", Map.of("type", "string"),
                                                "description", "颜色主题 (Color theme, optional)"
                                        )
                                ),
                                "required", java.util.List.of("xAxis", "series")
                        ))
                        .build();
            }

            @Override
            public ToolResult execute(Map<String, Object> arguments) {
                try {
                    log.debug("执行折线图生成: args={}", arguments);

                    ChartService.ChartConfig config = new ChartService.ChartConfig();
                    config.setType("line");
                    config.setTitle((String) arguments.get("title"));
                    config.setXAxis(((java.util.List<?>) arguments.get("xAxis")).toArray());
                    config.setSeriesData((java.util.List<Map<String, Object>>) arguments.get("series"));

                    ChartService.ChartResult result = chartService.generateChart(config);

                    return ToolResult.text(String.format(
                            "图表生成成功！\n" +
                            "图表ID: %s\n" +
                            "文件URL: http://localhost:40302%s",
                            result.getChartId(),
                            result.getFileUrl()
                    ));

                } catch (Exception e) {
                    log.error("折线图生成失败: {}", e.getMessage(), e);
                    return ToolResult.error("图表生成失败: " + e.getMessage());
                }
            }
        };
    }

    /**
     * 饼图工具
     */
    public ToolHandler pieChart() {
        return new ToolHandler() {
            @Override
            public ToolDefinition getDefinition() {
                return ToolDefinition.builder()
                        .name("chart_pie")
                        .description(
                                "Generate a pie chart to show proportions of a whole. " +
                                "生成饼图用于展示各部分占整体的比例。 " +
                                "Use this when you need to show percentage or proportion data. " +
                                "适用于需要展示百分比或比例数据的场景。 " +
                                "Common use cases: market share, expense breakdown, survey results. " +
                                "典型用例：市场份额、费用分解、调查结果。"
                        )
                        .inputSchema(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "title", Map.of(
                                                "type", "string",
                                                "description", "图表标题 (Chart title)"
                                        ),
                                        "data", Map.of(
                                                "type", "array",
                                                "items", Map.of(
                                                        "type", "object",
                                                        "properties", Map.of(
                                                                "name", Map.of("type", "string"),
                                                                "value", Map.of("type", "number")
                                                        ),
                                                        "required", java.util.List.of("name", "value")
                                                ),
                                                "description", "数据项 (Data items)"
                                        ),
                                        "colors", Map.of(
                                                "type", "array",
                                                "items", Map.of("type", "string"),
                                                "description", "颜色主题 (Color theme, optional)"
                                        )
                                ),
                                "required", java.util.List.of("data")
                        ))
                        .build();
            }

            @Override
            public ToolResult execute(Map<String, Object> arguments) {
                try {
                    log.debug("执行饼图生成: args={}", arguments);

                    ChartService.ChartConfig config = new ChartService.ChartConfig();
                    config.setType("pie");
                    config.setTitle((String) arguments.get("title"));

                    // 转换饼图数据格式
                    java.util.List<Map<String, Object>> dataItems = (java.util.List<Map<String, Object>>) arguments.get("data");
                    java.util.List<String> names = dataItems.stream()
                            .map(item -> (String) item.get("name"))
                            .collect(java.util.stream.Collectors.toList());
                    java.util.List<Number> values = dataItems.stream()
                            .map(item -> (Number) item.get("value"))
                            .collect(java.util.stream.Collectors.toList());

                    config.setXAxis(names.toArray());
                    java.util.List<Map<String, Object>> seriesData = new java.util.ArrayList<>();
                    Map<String, Object> series = new java.util.HashMap<>();
                    series.put("name", "Value");
                    series.put("data", values);
                    seriesData.add(series);
                    config.setSeriesData(seriesData);

                    ChartService.ChartResult result = chartService.generateChart(config);

                    return ToolResult.text(String.format(
                            "饼图生成成功！\n" +
                            "图表ID: %s\n" +
                            "文件URL: http://localhost:40302%s",
                            result.getChartId(),
                            result.getFileUrl()
                    ));

                } catch (Exception e) {
                    log.error("饼图生成失败: {}", e.getMessage(), e);
                    return ToolResult.error("饼图生成失败: " + e.getMessage());
                }
            }
        };
    }

    /**
     * 面积图工具
     */
    public ToolHandler areaChart() {
        return new ToolHandler() {
            @Override
            public ToolDefinition getDefinition() {
                return ToolDefinition.builder()
                        .name("chart_area")
                        .description(
                                "Generate an area chart to show trends with filled area. " +
                                "生成面积图用于展示带填充区域的变化趋势。 " +
                                "Use this when you want to emphasize the volume of data. " +
                                "适用于需要强调数据体量的场景。 " +
                                "Common use cases: website traffic, resource usage, cumulative data. " +
                                "典型用例：网站流量、资源使用、累积数据。"
                        )
                        .inputSchema(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "title", Map.of(
                                                "type", "string",
                                                "description", "图表标题 (Chart title)"
                                        ),
                                        "xAxis", Map.of(
                                                "type", "array",
                                                "items", Map.of("type", "string"),
                                                "description", "X 轴数据 (X-axis data)"
                                        ),
                                        "series", Map.of(
                                                "type", "array",
                                                "items", Map.of(
                                                        "type", "object",
                                                        "properties", Map.of(
                                                                "name", Map.of("type", "string"),
                                                                "data", Map.of(
                                                                        "type", "array",
                                                                        "items", Map.of("type", "number")
                                                                )
                                                        ),
                                                        "required", java.util.List.of("name", "data")
                                                ),
                                                "description", "系列数据 (Series data)"
                                        )
                                ),
                                "required", java.util.List.of("xAxis", "series")
                        ))
                        .build();
            }

            @Override
            public ToolResult execute(Map<String, Object> arguments) {
                try {
                    log.debug("执行面积图生成: args={}", arguments);

                    ChartService.ChartConfig config = new ChartService.ChartConfig();
                    config.setType("line");  // ECharts 中面积图通过 line + areaStyle 实现
                    config.setTitle((String) arguments.get("title"));
                    config.setXAxis(((java.util.List<?>) arguments.get("xAxis")).toArray());
                    config.setSeriesData((java.util.List<Map<String, Object>>) arguments.get("series"));

                    ChartService.ChartResult result = chartService.generateChart(config);

                    return ToolResult.text(String.format(
                            "面积图生成成功！\n" +
                            "图表ID: %s\n" +
                            "文件URL: http://localhost:40302%s",
                            result.getChartId(),
                            result.getFileUrl()
                    ));

                } catch (Exception e) {
                    log.error("面积图生成失败: {}", e.getMessage(), e);
                    return ToolResult.error("面积图生成失败: " + e.getMessage());
                }
            }
        };
    }

    /**
     * 雷达图工具
     */
    public ToolHandler radarChart() {
        return new ToolHandler() {
            @Override
            public ToolDefinition getDefinition() {
                return ToolDefinition.builder()
                        .name("chart_radar")
                        .description(
                                "Generate a radar chart for multi-dimensional comparison. " +
                                "生成雷达图用于多维度数据对比。 " +
                                "Use this when you need to compare multiple dimensions. " +
                                "适用于需要对比多个维度的场景。 " +
                                "Common use cases: skill assessment, performance metrics, product comparison. " +
                                "典型用例：技能评估、性能指标、产品对比。"
                        )
                        .inputSchema(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "title", Map.of(
                                                "type", "string",
                                                "description", "图表标题 (Chart title)"
                                        ),
                                        "indicators", Map.of(
                                                "type", "array",
                                                "items", Map.of("type", "string"),
                                                "description", "指标名称 (Indicator names)"
                                        ),
                                        "series", Map.of(
                                                "type", "array",
                                                "items", Map.of(
                                                        "type", "object",
                                                        "properties", Map.of(
                                                                "name", Map.of("type", "string"),
                                                                "data", Map.of(
                                                                        "type", "array",
                                                                        "items", Map.of("type", "number")
                                                                )
                                                        ),
                                                        "required", java.util.List.of("name", "data")
                                                ),
                                                "description", "系列数据 (Series data)"
                                        )
                                ),
                                "required", java.util.List.of("indicators", "series")
                        ))
                        .build();
            }

            @Override
            public ToolResult execute(Map<String, Object> arguments) {
                try {
                    log.debug("执行雷达图生成: args={}", arguments);

                    ChartService.ChartConfig config = new ChartService.ChartConfig();
                    config.setType("radar");
                    config.setTitle((String) arguments.get("title"));
                    config.setXAxis(((java.util.List<?>) arguments.get("indicators")).toArray());
                    config.setSeriesData((java.util.List<Map<String, Object>>) arguments.get("series"));

                    ChartService.ChartResult result = chartService.generateChart(config);

                    return ToolResult.text(String.format(
                            "雷达图生成成功！\n" +
                            "图表ID: %s\n" +
                            "文件URL: http://localhost:40302%s",
                            result.getChartId(),
                            result.getFileUrl()
                    ));

                } catch (Exception e) {
                    log.error("雷达图生成失败: {}", e.getMessage(), e);
                    return ToolResult.error("雷达图生成失败: " + e.getMessage());
                }
            }
        };
    }

    /**
     * 散点图工具
     */
    public ToolHandler scatterChart() {
        return new ToolHandler() {
            @Override
            public ToolDefinition getDefinition() {
                return ToolDefinition.builder()
                        .name("chart_scatter")
                        .description(
                                "Generate a scatter plot to show data distribution. " +
                                "生成散点图用于展示数据分布情况。 " +
                                "Use this when you need to analyze correlation or distribution. " +
                                "适用于需要分析相关性或分布的场景。 " +
                                "Common use cases: correlation analysis, outlier detection, data clustering. " +
                                "典型用例：相关性分析、异常值检测、数据聚类。"
                        )
                        .inputSchema(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "title", Map.of(
                                                "type", "string",
                                                "description", "图表标题 (Chart title)"
                                        ),
                                        "xAxisName", Map.of(
                                                "type", "string",
                                                "description", "X 轴名称 (X-axis name)"
                                        ),
                                        "yAxisName", Map.of(
                                                "type", "string",
                                                "description", "Y 轴名称 (Y-axis name)"
                                        ),
                                        "series", Map.of(
                                                "type", "array",
                                                "items", Map.of(
                                                        "type", "object",
                                                        "properties", Map.of(
                                                                "name", Map.of("type", "string"),
                                                                "data", Map.of(
                                                                        "type", "array",
                                                                        "items", Map.of(
                                                                                "type", "array",
                                                                                "items", Map.of("type", "number")
                                                                        )
                                                                )
                                                        ),
                                                        "required", java.util.List.of("name", "data")
                                                ),
                                                "description", "系列数据 (Series data), each point as [x, y]"
                                        )
                                ),
                                "required", java.util.List.of("series")
                        ))
                        .build();
            }

            @Override
            public ToolResult execute(Map<String, Object> arguments) {
                try {
                    log.debug("执行散点图生成: args={}", arguments);

                    ChartService.ChartConfig config = new ChartService.ChartConfig();
                    config.setType("scatter");
                    config.setTitle((String) arguments.get("title"));
                    config.setSeriesData((java.util.List<Map<String, Object>>) arguments.get("series"));

                    ChartService.ChartResult result = chartService.generateChart(config);

                    return ToolResult.text(String.format(
                            "散点图生成成功！\n" +
                            "图表ID: %s\n" +
                            "文件URL: http://localhost:40302%s",
                            result.getChartId(),
                            result.getFileUrl()
                    ));

                } catch (Exception e) {
                    log.error("散点图生成失败: {}", e.getMessage(), e);
                    return ToolResult.error("散点图生成失败: " + e.getMessage());
                }
            }
        };
    }
}
