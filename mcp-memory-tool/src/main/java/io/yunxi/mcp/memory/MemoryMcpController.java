package io.yunxi.mcp.memory;

import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.server.McpHttpEndpoint;
import io.yunxi.mcp.common.server.McpSseEndpoint;
import io.yunxi.mcp.memory.tool.MemoryAddTool;
import io.yunxi.mcp.memory.tool.MemoryGetTool;
import io.yunxi.mcp.memory.tool.MemoryRemoveTool;
import io.yunxi.mcp.memory.tool.MemoryReplaceTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MCP Memory 工具控制器
 * <p>
 * 提供记忆管理的 MCP 端点，支持 HTTP 和 SSE 两种调用模式。
 * </p>
 *
 * <h3>提供的工具</h3>
 * <ul>
 * <li>memory_add - 添加新的记忆条目</li>
 * <li>memory_replace - 替换现有的记忆条目</li>
 * <li>memory_remove - 删除指定的记忆条目</li>
 * <li>memory_get - 获取指定存储的所有记忆内容</li>
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
 *
 * @see McpHttpEndpoint
 * @see McpSseEndpoint
 *
 * @author yunxi-mcp-servers
 * @version 1.0.0
 */
@RestController
@RequestMapping("/mcp")
public class MemoryMcpController {

    private static final Logger log = LoggerFactory.getLogger(MemoryMcpController.class);

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
     * 构造函数
     * 初始化 HTTP 和 SSE 端点，并注册记忆管理工具。
     */
    public MemoryMcpController(
            MemoryAddTool memoryAddTool,
            MemoryReplaceTool memoryReplaceTool,
            MemoryRemoveTool memoryRemoveTool,
            MemoryGetTool memoryGetTool) {

        // 创建 HTTP 和 SSE 端点
        this.httpEndpoint = new McpHttpEndpoint("yunxi-mcp-memory", "1.0.0");
        this.sseEndpoint = new McpSseEndpoint("yunxi-mcp-memory", "1.0.0");

        // 注册工具到 HTTP 端点
        httpEndpoint.registerTool(memoryAddTool);
        httpEndpoint.registerTool(memoryReplaceTool);
        httpEndpoint.registerTool(memoryRemoveTool);
        httpEndpoint.registerTool(memoryGetTool);

        // 注册工具到 SSE 端点
        sseEndpoint.registerTool(memoryAddTool);
        sseEndpoint.registerTool(memoryReplaceTool);
        sseEndpoint.registerTool(memoryRemoveTool);
        sseEndpoint.registerTool(memoryGetTool);

        log.info("MemoryMcpController initialized with 4 tools");
    }

    /**
     * 健康检查端点
     *
     * @return 健康状态
     */
    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public String health() {
        return "{\"status\":\"UP\",\"service\":\"MCP Memory Tool Server\"}";
    }

    /**
     * 获取服务器信息
     *
     * @return 服务器信息
     */
    @GetMapping(value = "/info", produces = MediaType.APPLICATION_JSON_VALUE)
    public String info() {
        return """
                {
                    "name": "MCP Memory Tool Server",
                    "version": "1.0.0",
                    "description": "Memory management tools for MEMORY.md and USER.md",
                    "tools": ["memory_add", "memory_replace", "memory_remove", "memory_get"],
                    "endpoints": {
                        "http": {
                            "url": "/mcp",
                            "method": "POST",
                            "contentType": "application/json"
                        },
                        "sse": {
                            "connectUrl": "/mcp/sse",
                            "messageUrl": "/mcp/message",
                            "method": "POST",
                            "contentType": "application/json"
                        }
                    }
                }
                """;
    }

    /**
     * 列出所有可用工具
     *
     * @return 工具列表
     */
    @GetMapping(value = "/tools", produces = MediaType.APPLICATION_JSON_VALUE)
    public String listTools() {
        List<ToolDefinition> tools = httpEndpoint.getTools();
        return """
                {
                    "tools": [
                        {"name": "memory_add", "description": "添加新的记忆条目（支持多用户隔离）"},
                        {"name": "memory_replace", "description": "替换现有的记忆条目（支持多用户隔离）"},
                        {"name": "memory_remove", "description": "删除指定的记忆条目（支持多用户隔离）"},
                        {"name": "memory_get", "description": "获取指定存储的所有记忆内容（支持多用户隔离）"}
                    ]
                }
                """;
    }

    /**
     * HTTP 模式：处理 MCP 请求
     *
     * @param request MCP 请求（JSON 格式）
     * @return MCP 响应（JSON 格式）
     */
    @PostMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public String handleHttp(@RequestBody String request) {
        log.debug("Received HTTP MCP request");
        return httpEndpoint.handleRequest(request);
    }

    /**
     * SSE 模式：建立 SSE 连接
     *
     * @return SSE Emitter
     */
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connectSse() {
        log.info("New SSE connection established");
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        executor.execute(() -> {
            try {
                // 发送 endpoint 事件，告知客户端消息发送的端点 URL
                emitter.send(SseEmitter.event()
                        .name("endpoint")
                        .data("/mcp/message"));
                log.info("SSE endpoint event sent");
            } catch (Exception e) {
                log.error("SSE connection error", e);
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * SSE 模式：处理 SSE 消息
     *
     * @param request SSE 请求（JSON 格式）
     * @return SSE 响应（JSON 格式）
     */
    @PostMapping(value = "/message", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public String handleSseMessage(@RequestBody String request) {
        log.debug("Received SSE message");
        return sseEndpoint.handleMessage(request);
    }
}
