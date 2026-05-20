package io.yunxi.mcp.baidusearch;

import io.yunxi.mcp.baidusearch.tool.BaiduSearchTools;
import io.yunxi.mcp.common.server.McpHttpEndpoint;
import io.yunxi.mcp.common.server.McpSseEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MCP Baidu Search 控制器
 * <p>
 * 提供百度搜索操作的 MCP 端点，支持 HTTP 和 SSE 两种调用模式。
 * </p>
 *
 * <h3>支持的调用模式</h3>
 * <ul>
 * <li><b>HTTP 模式</b>: 直接 POST JSON-RPC 请求到 /mcp 端点</li>
 * <li><b>SSE 模式</b>: 通过 /mcp/sse 建立 SSE 连接，通过 /mcp/message 发送消息</li>
 * </ul>
 *
 * <h3>提供的工具</h3>
 * <ul>
 * <li>{@code baidu_search} - 使用百度 AI 搜索引擎进行网页搜索</li>
 * </ul>
 *
 * <h3>测试端点</h3>
 * <ul>
 * <li>{@code GET /mcp/tools} - 获取可用工具列表（含参数Schema）</li>
 * <li>{@code GET /mcp/health} - 健康检查</li>
 * </ul>
 *
 * @see McpHttpEndpoint
 * @see McpSseEndpoint
 */
@Slf4j
@RestController
@RequestMapping("/mcp")
public class BaiduSearchMcpController {

    /**
     * HTTP 模式端点
     */
    private final McpHttpEndpoint httpEndpoint;

    /**
     * SSE 模式端点
     */
    private final McpSseEndpoint sseEndpoint;

    /**
     * SSE 连接处理器线程池
     */
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * 百度搜索工具
     */
    private final BaiduSearchTools.SearchTool searchTool;

    /**
     * 构造函数
     */
    public BaiduSearchMcpController(BaiduSearchTools baiduSearchTools) {
        log.info("初始化 MCP Baidu Search 控制器...");

        // 初始化 HTTP 和 SSE 端点
        this.httpEndpoint = new McpHttpEndpoint("yunxi-mcp-baidu-search", "1.0.0");
        this.sseEndpoint = new McpSseEndpoint("yunxi-mcp-baidu-search", "1.0.0");

        // 初始化工具
        this.searchTool = new BaiduSearchTools.SearchTool(baiduSearchTools);

        // 注册工具到 HTTP 端点
        httpEndpoint.registerTool(searchTool);

        // 注册工具到 SSE 端点
        sseEndpoint.registerTool(searchTool);

        log.info("MCP Baidu Search 控制器初始化完成，已注册 {} 个工具: baidu_search", 1);
    }

    // ============ HTTP 模式 ============

    /**
     * 处理 HTTP 模式的 MCP 请求
     */
    @PostMapping
    public String handleHttpRequest(@RequestBody String requestJson) {
        log.debug("收到 HTTP MCP 请求: {}",
                requestJson.length() > 200 ? requestJson.substring(0, 200) + "..." : requestJson);
        String response = httpEndpoint.handleRequest(requestJson);
        log.debug("返回 HTTP MCP 响应: {}", response.length() > 200 ? response.substring(0, 200) + "..." : response);
        return response;
    }

    // ============ SSE 模式 ============

    /**
     * 建立 SSE 连接
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
     */
    @PostMapping("/message")
    public String handleSseMessage(@RequestBody String requestJson) {
        log.debug("收到 SSE 消息: {}", requestJson.length() > 200 ? requestJson.substring(0, 200) + "..." : requestJson);
        return sseEndpoint.handleMessage(requestJson);
    }

    // ============ 测试端点 ============

    /**
     * 获取可用工具列表
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

    /**
     * 获取服务器信息
     */
    @GetMapping("/info")
    public Map<String, Object> getInfo() {
        return Map.of(
                "name", httpEndpoint.getServerInfo().get("name"),
                "version", httpEndpoint.getServerInfo().get("version"),
                "tools", httpEndpoint.getTools(),
                "modes", new String[] { "http", "sse" });
    }

    /**
     * 健康检查端点
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP");
    }
}