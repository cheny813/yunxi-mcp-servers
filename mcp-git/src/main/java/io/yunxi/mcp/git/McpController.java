package io.yunxi.mcp.git;

import io.yunxi.mcp.common.server.McpHttpEndpoint;
import io.yunxi.mcp.common.server.McpSseEndpoint;
import io.yunxi.mcp.git.tool.GitTools;
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
 * MCP Git 控制器
 * <p>
 * 提供 Git 操作的 MCP 端点，支持 HTTP 和 SSE 两种调用模式。
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
 * <li>{@code git_status} - 获取 Git 仓库状态</li>
 * <li>{@code git_log} - 获取 Git 提交日志</li>
 * <li>{@code git_branches} - 获取分支列表</li>
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
     * <p>
     * 用于执行 SSE 连接建立后的异步任务。
     * 使用缓存线程池，可以根据需要创建新线程。
     * </p>
     */
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * 构造函数
     * <p>
     * 初始化 HTTP 和 SSE 端点，并注册 Git 操作工具。
     * </p>
     *
     * @param repoPath Git 仓库路径，可通过配置项 git.repo-path 指定，
     *                 如果未配置则使用当前工作目录
     */
    public McpController(@Value("${git.repo-path:}") String repoPath) {
        log.info("初始化 MCP Git 控制器...");

        // 如果未配置 repoPath，则使用当前工作目录
        String defaultPath = repoPath != null && !repoPath.isBlank() ? repoPath : System.getProperty("user.dir");
        log.debug("Git 仓库路径: {}", defaultPath);

        // 初始化 HTTP 和 SSE 端点
        this.httpEndpoint = new McpHttpEndpoint("yunxi-mcp-git", "1.0.0");
        this.sseEndpoint = new McpSseEndpoint("yunxi-mcp-git", "1.0.0");

        // 注册 Git 操作工具到 HTTP 端点
        httpEndpoint.registerTool(new GitTools.StatusTool(defaultPath));
        httpEndpoint.registerTool(new GitTools.LogTool(defaultPath));
        httpEndpoint.registerTool(new GitTools.BranchesTool(defaultPath));

        // 注册 Git 操作工具到 SSE 端点
        sseEndpoint.registerTool(new GitTools.StatusTool(defaultPath));
        sseEndpoint.registerTool(new GitTools.LogTool(defaultPath));
        sseEndpoint.registerTool(new GitTools.BranchesTool(defaultPath));

        log.info("MCP Git 控制器初始化完成，已注册 {} 个工具: git_status, git_log, git_branches", 3);
    }

    // ============ HTTP 模式 ============

    /**
     * 处理 HTTP 模式的 MCP 请求
     * <p>
     * 接收 JSON-RPC 请求，返回 JSON-RPC 响应。
     * 适用于同步的请求-响应场景。
     * </p>
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
     * <p>
     * 客户端通过此端点建立长连接，服务器返回 MCP 端点地址。
     * 后续消息通过 /mcp/message 端点发送。
     * </p>
     *
     * @return SSE 发射器，用于向客户端推送事件
     */
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter handleSseConnection() {
        log.info("建立新的 SSE 连接");

        // 创建无限超时的 SSE 发射器
        SseEmitter emitter = new SseEmitter(0L);

        // 异步执行 SSE 连接建立逻辑
        executor.execute(() -> {
            try {
                // 发送初始事件，包含消息端点地址
                emitter.send(SseEmitter.event()
                        .name("endpoint")
                        .data("/mcp/message"));
                log.debug("SSE 连接已建立，消息端点: /mcp/message");

                // 设置事件回调
                emitter.onCompletion(() -> log.info("SSE 连接已关闭"));
                emitter.onTimeout(() -> log.warn("SSE 连接超时"));
                emitter.onError(e -> log.error("SSE 连接错误: {}", e.getMessage()));
            } catch (Exception e) {
                log.error("建立 SSE 连接失败: {}", e.getMessage(), e);
                // 发生异常时关闭连接
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * 处理 SSE 模式的消息
     * <p>
     * 接收 JSON-RPC 请求并返回响应。
     * 此端点由 SSE 连接建立后客户端调用。
     * </p>
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
     * <h4>示例响应</h4>
     * 
     * <pre>{@code
     * {
     *   "tools": [
     *     {
     *       "name": "git_status",
     *       "description": "获取 Git 仓库状态",
     *       "inputSchema": { "type": "object", "properties": {} }
     *     }
     *   ]
     * }
     * }</pre>
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
     * <p>
     * 返回 MCP 服务器的名称、版本、可用工具列表和支持的调用模式。
     * </p>
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
     * <p>
     * 用于检查服务是否正常运行。
     * </p>
     *
     * @return 健康状态
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP");
    }
}
