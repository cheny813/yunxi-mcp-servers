package io.yunxi.mcp.common.controller;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import io.yunxi.mcp.common.server.McpHttpEndpoint;
import io.yunxi.mcp.common.server.McpSseEndpoint;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MCP 统一 HTTP 控制器抽象基类
 * <p>
 * 提供统一的 MCP HTTP 接口实现：
 * <ul>
 * <li>POST /mcp - JSON-RPC 请求处理</li>
 * <li>GET /mcp/sse - SSE 连接建立</li>
 * <li>GET /mcp/health - 健康检查</li>
 * <li>GET /mcp/tools - 工具列表</li>
 * <li>GET /mcp/info - 服务器信息</li>
 * </ul>
 * </p>
 * <p>
 * 子类需要实现 {@link #getServerName()} 和 {@link #registerTools()} 方法。
 * 工具注册在 {@code @PostConstruct} 阶段执行，此时 Spring 字段注入已完成，
 * 子类可安全访问 {@code @Autowired} / {@code @Value} 注入的依赖。
 * </p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * @RestController
 * public class MyMcpController extends AbstractMcpController {
 *     @Autowired
 *     private MyService myService;
 *
 *     @Override
 *     protected String getServerName() { return "my-mcp-server"; }
 *
 *     @Override
 *     protected void registerTools() {
 *         registerTool(new MyTool(myService));
 *     }
 * }
 * }</pre>
 *
 * @version 1.1.0
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/mcp")
public abstract class AbstractMcpController {

    protected McpHttpEndpoint httpEndpoint;
    protected McpSseEndpoint sseEndpoint;
    protected final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * 初始化端点实例（不含工具注册，工具注册在 @PostConstruct 中完成）
     */
    public AbstractMcpController() {
        log.info("初始化 MCP 统一控制器 [{}]...", getServerName());
        this.httpEndpoint = new McpHttpEndpoint(getServerName(), getServerVersion());
        this.sseEndpoint = new McpSseEndpoint(getServerName(), getServerVersion());
    }

    /**
     * Spring Bean 初始化后注册工具。
     * 此时所有 @Autowired / @Value 字段注入已完成，子类可安全访问依赖。
     */
    @PostConstruct
    protected void init() {
        registerTools();
        log.info("MCP 统一控制器 [{}] 初始化完成，已注册工具数量: {}",
                getServerName(), httpEndpoint.getTools().size());
    }

    /**
     * 获取服务器名称
     *
     * @return 服务器名称
     */
    protected abstract String getServerName();

    /**
     * 获取服务器版本
     *
     * @return 服务器版本
     */
    protected String getServerVersion() {
        return "1.0.0";
    }

    /**
     * 注册工具处理器
     * <p>
     * 子类实现此方法来注册具体的工具处理器。
     * 在此方法中可安全访问 Spring 注入的依赖。
     * </p>
     */
    protected abstract void registerTools();

    /**
     * 注册单个工具处理器（同时注册到 HTTP 和 SSE 端点）
     *
     * @param handler 工具处理器
     */
    protected void registerTool(ToolHandler handler) {
        httpEndpoint.registerTool(handler);
        sseEndpoint.registerTool(handler);
        log.debug("注册工具: {}", handler.getName());
    }

    // ============ HTTP 端点 ============

    /**
     * 处理 JSON-RPC 请求
     *
     * @param requestJson JSON-RPC 请求字符串
     * @return JSON-RPC 响应字符串
     */
    @PostMapping
    public String handleRequest(@RequestBody String requestJson) {
        log.debug("收到 HTTP MCP 请求: {}", requestJson);
        return httpEndpoint.handleRequest(requestJson);
    }

    // ============ SSE 端点 ============

    /**
     * 建立 SSE 连接
     *
     * @return SSE 发射器
     */
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter handleSseConnection() {
        SseEmitter emitter = new SseEmitter(0L);
        executor.execute(() -> {
            try {
                emitter.send(SseEmitter.event().name("endpoint").data("/mcp/message"));
                emitter.onCompletion(() -> log.info("SSE 连接完成"));
                emitter.onTimeout(() -> log.warn("SSE 连接超时"));
                emitter.onError(e -> log.error("SSE 连接错误", e));
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    /**
     * 处理 SSE 消息
     *
     * @param requestJson JSON-RPC 请求字符串
     * @return JSON-RPC 响应字符串
     */
    @PostMapping("/message")
    public String handleSseMessage(@RequestBody String requestJson) {
        log.debug("收到 SSE MCP 消息: {}", requestJson);
        return sseEndpoint.handleMessage(requestJson);
    }

    // ============ 通用端点 ============

    /**
     * 健康检查
     *
     * @return 健康状态
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "server", getServerName(),
                "version", getServerVersion(),
                "tools", httpEndpoint.getTools().size()
        );
    }

    /**
     * 获取工具列表
     *
     * @return 工具定义列表
     */
    @GetMapping("/tools")
    public List<ToolDefinition> getTools() {
        return httpEndpoint.getTools();
    }

    /**
     * 获取服务器信息
     *
     * @return 服务器信息
     */
    @GetMapping("/info")
    public Map<String, Object> getInfo() {
        return Map.of(
                "name", httpEndpoint.getServerInfo().get("name"),
                "version", httpEndpoint.getServerInfo().get("version"),
                "tools", httpEndpoint.getTools().stream()
                        .map(tool -> Map.of(
                                "name", tool.getName(),
                                "description", tool.getDescription()))
                        .toList());
    }
}
