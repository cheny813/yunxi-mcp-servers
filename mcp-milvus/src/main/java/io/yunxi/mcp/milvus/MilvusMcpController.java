package io.yunxi.mcp.milvus;

import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.server.McpHttpEndpoint;
import io.yunxi.mcp.common.server.McpSseEndpoint;
import io.yunxi.mcp.milvus.config.MilvusClientHolder;
import io.yunxi.mcp.milvus.tool.SearchDishesTool;
import io.yunxi.mcp.milvus.tool.GetDishDetailsTool;
import io.yunxi.mcp.milvus.tool.SearchIngredientsTool;
import io.yunxi.mcp.milvus.tool.GetNutrientStandardTool;
import io.yunxi.mcp.milvus.tool.EvaluateRecipeTool;
import io.yunxi.mcp.milvus.embedding.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MCP Milvus 向量搜索控制器
 * <p>
 * 提供 Milvus 向量搜索操作的 MCP 端点，支持 HTTP 和 SSE 两种调用模式。
 * </p>
 *
 * <h3>提供的工具</h3>
 * <ul>
 * <li>search_dishes - 向量搜索菜品（按学校ID和查询词）</li>
 * <li>get_dish_details - 获取菜品详情</li>
 * </ul>
 *
 * <h3>测试端点</h3>
 * <ul>
 * <li>GET /mcp/health - 健康检查</li>
 * <li>GET /mcp/info - 服务器信息</li>
 * <li>GET /mcp/tools - 工具列表</li>
 * <li>POST /mcp - HTTP 模式调用</li>
 * <li>GET /mcp/sse - SSE 模式连接</li>
 * <li>POST /mcp/message - SSE 模式消息</li>
 * </ul>
 */
@RestController
@RequestMapping("/mcp")
public class MilvusMcpController {

    private static final Logger log = LoggerFactory.getLogger(MilvusMcpController.class);

    /**
     * Milvus 客户端持有者（支持自动重连）
     */
    private final MilvusClientHolder milvusClientHolder;

    /**
     * Embedding 服务
     */
    private final EmbeddingService embeddingService;

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
     * 构造函数
     * <p>
     * 初始化 HTTP 和 SSE 端点，并注册向量搜索工具。
     * </p>
     *
     * @param milvusClientHolder Milvus 客户端持有者，由 Spring 自动注入
     * @param embeddingService Embedding 服务，由 Spring 自动注入
     */
    @Autowired
    public MilvusMcpController(MilvusClientHolder milvusClientHolder, EmbeddingService embeddingService) {
        this.milvusClientHolder = milvusClientHolder;
        this.embeddingService = embeddingService;
        log.info("初始化 MCP Milvus 控制器...");

        this.httpEndpoint = new McpHttpEndpoint("yunxi-mcp-milvus", "1.0.0");
        this.sseEndpoint = new McpSseEndpoint("yunxi-mcp-milvus", "1.0.0");

        log.info("初始化 HTTP 和 SSE 端点完成");

        // 注册向量搜索工具到 HTTP 端点（使用 ClientHolder 支持自动重连）
        httpEndpoint.registerTool(new SearchDishesTool(milvusClientHolder, embeddingService));
        httpEndpoint.registerTool(new GetDishDetailsTool(milvusClientHolder, embeddingService));
        httpEndpoint.registerTool(new SearchIngredientsTool(milvusClientHolder));
        httpEndpoint.registerTool(new GetNutrientStandardTool(milvusClientHolder));
        httpEndpoint.registerTool(new EvaluateRecipeTool(milvusClientHolder));
        log.info("已注册 {} 个工具到 HTTP 端点", httpEndpoint.getTools().size());

        // 注册向量搜索工具到 SSE 端点
        sseEndpoint.registerTool(new SearchDishesTool(milvusClientHolder, embeddingService));
        sseEndpoint.registerTool(new GetDishDetailsTool(milvusClientHolder, embeddingService));
        sseEndpoint.registerTool(new SearchIngredientsTool(milvusClientHolder));
        sseEndpoint.registerTool(new GetNutrientStandardTool(milvusClientHolder));
        sseEndpoint.registerTool(new EvaluateRecipeTool(milvusClientHolder));
        log.info("已注册 {} 个工具到 SSE 端点", sseEndpoint.getTools().size());

        log.info("MCP Milvus 控制器初始化完成，可用工具: {}",
                httpEndpoint.getTools().stream().map(tool -> tool.getName()).toList());
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
        log.debug("收到 HTTP MCP 请求: {}", requestJson);
        String response = httpEndpoint.handleRequest(requestJson);
        log.debug("HTTP MCP 响应: {}", response);
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
        log.info("收到新的 SSE 连接请求");

        SseEmitter emitter = new SseEmitter(0L);

        executor.execute(() -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("endpoint")
                        .data("/mcp/message"));
                log.info("SSE 连接建立成功，端点信息已发送: /mcp/message");

                emitter.onCompletion(() -> {
                    log.info("SSE 连接完成");
                });
                emitter.onTimeout(() -> {
                    log.warn("SSE 连接超时");
                });
                emitter.onError(e -> {
                    log.error("SSE 连接发生错误", e);
                });
            } catch (Exception e) {
                log.error("发送 SSE 端点信息失败", e);
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
        log.debug("收到 SSE MCP 消息: {}", requestJson);
        String response = sseEndpoint.handleMessage(requestJson);
        log.debug("SSE MCP 响应: {}", response);
        return response;
    }

    // ============ 通用端点 ============

    /**
     * 获取服务器信息
     *
     * @return 服务器信息 Map
     */
    @GetMapping("/info")
    public Map<String, Object> getInfo() {
        log.debug("收到获取服务器信息请求");
        Map<String, Object> info = Map.of(
                "name", httpEndpoint.getServerInfo().get("name"),
                "version", httpEndpoint.getServerInfo().get("version"),
                "tools", httpEndpoint.getTools(),
                "modes", new String[] { "http", "sse" });
        log.info("返回服务器信息: name={}, version={}, 工具数={}",
                info.get("name"), info.get("version"), ((java.util.List<?>) info.get("tools")).size());
        return info;
    }

    /**
     * 健康检查端点
     *
     * @return 健康状态
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        log.debug("收到健康检查请求");
        try {
            // 测试 Milvus 连接
            boolean connected = milvusClientHolder.isAvailable();
            Map<String, Object> healthStatus = Map.of(
                    "status", connected ? "UP" : "DOWN"
            );
            log.info("健康检查状态: {}", healthStatus);
            return healthStatus;
        } catch (Exception e) {
            log.error("健康检查失败", e);
            return Map.of("status", "DOWN", "error", e.getMessage());
        }
    }

    /**
     * 获取所有可用工具列表
     * <p>
     * 返回所有已注册的向量搜索工具的详细信息，包括工具名称、描述和参数定义。
     * </p>
     *
     * @return 工具定义列表（JSON 数组，与标准 MCP 协议一致）
     */
    @GetMapping("/tools")
    public List<ToolDefinition> getTools() {
        log.debug("收到获取工具列表请求");
        List<ToolDefinition> tools = httpEndpoint.getTools();
        log.info("返回工具列表，共 {} 个工具", tools.size());
        return tools;
    }
}
