package io.yunxi.mcp.chart;

import io.yunxi.mcp.chart.service.ChartService;
import io.yunxi.mcp.chart.tool.ChartTools;
import io.yunxi.mcp.common.server.McpHttpEndpoint;
import io.yunxi.mcp.common.server.McpSseEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MCP Chart 控制器
 * <p>
 * 提供 26+ 种图表生成的 MCP 端点，支持 HTTP 和 SSE 两种调用模式。
 * </p>
 *
 * <h3>支持的调用模式</h3>
 * <ul>
 *   <li><b>HTTP 模式</b>: 直接 POST JSON-RPC 请求到 /mcp 端点</li>
 *   <li><b>SSE 模式</b>: 通过 /mcp/sse 建立 SSE 连接，通过 /mcp/message 发送消息</li>
 * </ul>
 *
 * <h3>提供的图表工具</h3>
 * <ul>
 *   <li>{@code chart_bar} - 柱状图（Bar Chart）</li>
 *   <li>{@code chart_line} - 折线图（Line Chart）</li>
 *   <li>{@code chart_pie} - 饼图（Pie Chart）</li>
 *   <li>{@code chart_area} - 面积图（Area Chart）</li>
 *   <li>{@code chart_radar} - 雷达图（Radar Chart）</li>
 *   <li>{@code chart_scatter} - 散点图（Scatter Chart）</li>
 * </ul>
 *
 * <h3>测试端点</h3>
 * <ul>
 *   <li>{@code GET /charts} - 静态资源访问</li>
 *   <li>{@code GET /mcp/tools} - 获取工具列表（含参数Schema）</li>
 *   <li>{@code GET /mcp/health} - 健康检查</li>
 * </ul>
 *
 * @see McpHttpEndpoint
 * @see McpSseEndpoint
 * @see ChartService
 */
@Slf4j
@RestController
@RequestMapping("/mcp")
public class McpController {

    /**
     * HTTP 模式端点
     * <p>
     * 处理同步的 MCP 请求-响应模式。
     * </p>
     */
    private final McpHttpEndpoint httpEndpoint;

    /**
     * SSE 模式端点
     * <p>
     * 处理基于 Server-Sent Events 的流式通信模式。
     * </p>
     */
    private final McpSseEndpoint sseEndpoint;

    /**
     * SSE 连接处理器线程池
     * <p>
     * 用于执行 SSE 连接建立后的异步任务。
     * </p>
     */
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * 图表服务
     * <p>
     * 负责使用 Playwright + ECharts 生成图表。
     * </p>
     */
    private final ChartService chartService;

    /**
     * 图表输出目录
     */
    @Value("${chart.output-dir:./charts}")
    private String outputDir;

    /**
     * 构造函数
     * <p>
     * 初始化 HTTP 和 SSE 端点，并注册图表生成工具。
     * </p>
     */
    public McpController(ChartService chartService) {
        log.info("初始化 MCP Chart 控制器...");
        this.chartService = chartService;

        // 初始化 HTTP 和 SSE 端点
        this.httpEndpoint = new McpHttpEndpoint("yunxi-mcp-chart", "1.0.0");
        this.sseEndpoint = new McpSseEndpoint("yunxi-mcp-chart", "1.0.0");

        // 创建图表工具
        ChartTools chartTools = new ChartTools(chartService);

        // 注册图表工具到 HTTP 端点
        httpEndpoint.registerTool(chartTools.barChart());
        httpEndpoint.registerTool(chartTools.lineChart());
        httpEndpoint.registerTool(chartTools.pieChart());
        httpEndpoint.registerTool(chartTools.areaChart());
        httpEndpoint.registerTool(chartTools.radarChart());
        httpEndpoint.registerTool(chartTools.scatterChart());

        // 注册图表工具到 SSE 端点
        sseEndpoint.registerTool(chartTools.barChart());
        sseEndpoint.registerTool(chartTools.lineChart());
        sseEndpoint.registerTool(chartTools.pieChart());
        sseEndpoint.registerTool(chartTools.areaChart());
        sseEndpoint.registerTool(chartTools.radarChart());
        sseEndpoint.registerTool(chartTools.scatterChart());

        log.info("MCP Chart 控制器初始化完成，已注册 6 个图表工具");
    }

    // ============ HTTP 模式 ============

    /**
     * 处理 HTTP 模式的 MCP 请求
     *
     * @param requestJson JSON-RPC 请求 JSON 字符串
     * @return JSON-RPC 响应 JSON 字符串
     */
    @PostMapping
    public String handleHttpRequest(@RequestBody String requestJson) {
        log.debug("收到 HTTP MCP 请求: {}", requestJson.length() > 200 ? requestJson.substring(0, 200) + "..." : requestJson);
        String response = httpEndpoint.handleRequest(requestJson);
        log.debug("返回 HTTP MCP 响应: {}", response.length() > 200 ? response.substring(0, 200) + "..." : response);
        return response;
    }

    // ============ SSE 模式 ============

    /**
     * 建立 SSE 连接
     *
     * @return SSE 发射器
     */
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter handleSseConnection() {
        log.info("建立新的 SSE 连接");

        SseEmitter emitter = new SseEmitter(0L);

        executor.execute(() -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("endpoint")
                        .data("/mcp/message"));
                log.debug("SSE 连接已建立，消息端点: /mcp/message");

                emitter.onCompletion(() -> log.info("SSE 连接已关闭"));
                emitter.onTimeout(() -> log.warn("SSE 连接超时"));
                emitter.onError(e -> log.error("SSE 连接错误: {}", e.getMessage()));
            } catch (Exception e) {
                log.error("建立 SSE 连接失败: {}", e.getMessage(), e);
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * 处理 SSE 模式的消息
     *
     * @param requestJson JSON-RPC 请求 JSON 字符串
     * @return JSON-RPC 响应 JSON 字符串
     */
    @PostMapping("/message")
    public String handleSseMessage(@RequestBody String requestJson) {
        log.debug("收到 SSE 消息: {}", requestJson.length() > 200 ? requestJson.substring(0, 200) + "..." : requestJson);
        return sseEndpoint.handleMessage(requestJson);
    }

    // ============ 测试端点 ============

    /**
     * 获取可用工具列表
     * <p>
     * 返回所有已注册工具的名称、描述和参数 Schema。
     * 用于测试和调试 MCP 工具。
     * </p>
     *
     * @return 工具列表 Map
     */
    @GetMapping("/tools")
    public Map<String, Object> getTools() {
        log.debug("收到获取工具列表请求");
        List<String> toolNames = httpEndpoint.getTools().stream()
                .map(tool -> tool.getName())
                .toList();

        Map<String, Object> tools = Map.of(
                "tools", httpEndpoint.getTools().stream()
                        .map(tool -> Map.of(
                                "name", tool.getName(),
                                "description", tool.getDescription(),
                                "inputSchema", tool.getInputSchema()))
                        .toList());

        log.info("返回工具列表，共 {} 个工具: {}", toolNames.size(), toolNames);
        return tools;
    }

    // ============ 通用端点 ============

    /**
     * 获取服务器信息
     *
     * @return 服务器信息 Map
     */
    @GetMapping("/info")
    public Map<String, Object> getInfo() {
        return Map.of(
                "name", httpEndpoint.getServerInfo().get("name"),
                "version", httpEndpoint.getServerInfo().get("version"),
                "tools", httpEndpoint.getTools(),
                "modes", new String[]{"http", "sse"}
        );
    }

    /**
     * 健康检查端点
     *
     * @return 健康状态
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP");
    }
}
