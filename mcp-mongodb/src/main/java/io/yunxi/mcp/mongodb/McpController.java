package io.yunxi.mcp.mongodb;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import io.yunxi.mcp.common.server.McpHttpEndpoint;
import io.yunxi.mcp.common.server.McpSseEndpoint;
import io.yunxi.mcp.mongodb.tool.MongoTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MCP MongoDB 控制器
 * <p>
 * 提供 MongoDB 操作的 MCP 端点，支持 HTTP 和 SSE 两种调用模式。
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
 * <li>{@code mongodb_list_databases} - 列出所有数据库</li>
 * <li>{@code mongodb_list_collections} - 列出数据库中的集合</li>
 * <li>{@code mongodb_find} - 查询文档</li>
 * <li>{@code mongodb_insert} - 插入文档</li>
 * <li>{@code mongodb_delete} - 删除文档</li>
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
     */
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * MongoDB 客户端
     * <p>
     * 用于与 MongoDB 数据库进行交互。
     * </p>
     */
    private final MongoClient mongoClient;

    /**
     * 构造函数
     * <p>
     * 初始化 MongoDB 客户端、HTTP 和 SSE 端点，并注册 MongoDB 操作工具。
     * </p>
     *
     * @param connectionString MongoDB 连接字符串，可通过配置项 mongodb.connection-string 指定
     * @param defaultDatabase  默认数据库名称，可通过配置项 mongodb.database 指定
     */
    public McpController(
            @Value("${mongodb.connection-string:mongodb://localhost:27017}") String connectionString,
            @Value("${mongodb.database:}") String defaultDatabase) {

        log.info("初始化 MCP MongoDB 控制器...");
        log.debug("MongoDB 连接字符串: {}", connectionString.replaceAll("//[^:]+:[^@]+@", "//***:***@"));

        // 创建 MongoDB 客户端
        this.mongoClient = MongoClients.create(connectionString);

        // 初始化 HTTP 和 SSE 端点
        this.httpEndpoint = new McpHttpEndpoint("yunxi-mcp-mongodb", "1.0.0");
        this.sseEndpoint = new McpSseEndpoint("yunxi-mcp-mongodb", "1.0.0");

        // 注册 MongoDB 操作工具到 HTTP 端点
        httpEndpoint.registerTool(new MongoTools.ListDatabasesTool(mongoClient));
        httpEndpoint.registerTool(new MongoTools.ListCollectionsTool(mongoClient));
        httpEndpoint.registerTool(new MongoTools.FindTool(mongoClient));
        httpEndpoint.registerTool(new MongoTools.InsertTool(mongoClient));
        httpEndpoint.registerTool(new MongoTools.DeleteTool(mongoClient));

        // 注册 MongoDB 操作工具到 SSE 端点
        sseEndpoint.registerTool(new MongoTools.ListDatabasesTool(mongoClient));
        sseEndpoint.registerTool(new MongoTools.ListCollectionsTool(mongoClient));
        sseEndpoint.registerTool(new MongoTools.FindTool(mongoClient));
        sseEndpoint.registerTool(new MongoTools.InsertTool(mongoClient));
        sseEndpoint.registerTool(new MongoTools.DeleteTool(mongoClient));

        log.info("MCP MongoDB 控制器初始化完成，已注册 {} 个工具", 5);
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
        log.debug("收到 HTTP MCP 请求: {}",
                requestJson.length() > 200 ? requestJson.substring(0, 200) + "..." : requestJson);
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
                "modes", new String[] { "http", "sse" });
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
