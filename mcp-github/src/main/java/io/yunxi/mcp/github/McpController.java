package io.yunxi.mcp.github;

import io.yunxi.mcp.common.server.McpHttpEndpoint;
import io.yunxi.mcp.common.server.McpSseEndpoint;
import io.yunxi.mcp.github.tool.GitHubTools;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MCP GitHub 控制器
 * <p>
 * 支持 HTTP 和 SSE 两种模式的 GitHub 操作控制器。
 * </p>
 *
 * <h3>提供的功能</h3>
 * <ul>
 * <li>列出仓库</li>
 * <li>获取仓库信息</li>
 * <li>列出 Issue</li>
 * <li>创建 Issue</li>
 * <li>列出 Pull Request</li>
 * </ul>
 *
 * <h3>测试端点</h3>
 * <ul>
 * <li>{@code GET /mcp/tools} - 获取可用工具列表（含参数Schema）</li>
 * <li>{@code GET /mcp/health} - 健康检查</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/mcp")
public class McpController {

    /**
     * HTTP 模式的 MCP 端点
     */
    private final McpHttpEndpoint httpEndpoint;

    /**
     * SSE 模式的 MCP 端点
     */
    private final McpSseEndpoint sseEndpoint;

    /**
     * 线程池
     */
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * GitHub API 客户端
     */
    private final GitHub github;

    /**
     * 构造函数
     */
    public McpController(
            @Value("${github.token:}") String token,
            @Value("${github.api-url:}") String apiUrl) throws Exception {

        log.info("初始化 MCP GitHub 控制器...");

        if (token != null && !token.isBlank()) {
            this.github = new GitHubBuilder().withOAuthToken(token).build();
            log.debug("GitHub API 客户端已初始化");
        } else {
            log.error("GitHub token 未配置，请设置 github.token 属性");
            throw new IllegalStateException("GitHub token is required. Set github.token property.");
        }

        this.httpEndpoint = new McpHttpEndpoint("yunxi-mcp-github", "1.0.0");
        this.sseEndpoint = new McpSseEndpoint("yunxi-mcp-github", "1.0.0");

        // 注册工具
        httpEndpoint.registerTool(new GitHubTools.ListRepositoriesTool(github));
        httpEndpoint.registerTool(new GitHubTools.GetRepositoryTool(github));
        httpEndpoint.registerTool(new GitHubTools.ListIssuesTool(github));
        httpEndpoint.registerTool(new GitHubTools.CreateIssueTool(github));
        httpEndpoint.registerTool(new GitHubTools.ListPullRequestsTool(github));

        sseEndpoint.registerTool(new GitHubTools.ListRepositoriesTool(github));
        sseEndpoint.registerTool(new GitHubTools.GetRepositoryTool(github));
        sseEndpoint.registerTool(new GitHubTools.ListIssuesTool(github));
        sseEndpoint.registerTool(new GitHubTools.CreateIssueTool(github));
        sseEndpoint.registerTool(new GitHubTools.ListPullRequestsTool(github));

        log.info("MCP GitHub 控制器初始化完成，已注册 {} 个工具", 5);
    }

    // ============ HTTP 模式 ============

    @PostMapping
    public String handleHttpRequest(@RequestBody String requestJson) {
        log.debug("收到 HTTP MCP 请求: {}",
                requestJson.length() > 200 ? requestJson.substring(0, 200) + "..." : requestJson);
        String response = httpEndpoint.handleRequest(requestJson);
        log.debug("返回 HTTP MCP 响应: {}", response.length() > 200 ? response.substring(0, 200) + "..." : response);
        return response;
    }

    // ============ SSE 模式 ============

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

    @GetMapping("/info")
    public Map<String, Object> getInfo() {
        return Map.of(
                "name", httpEndpoint.getServerInfo().get("name"),
                "version", httpEndpoint.getServerInfo().get("version"),
                "tools", httpEndpoint.getTools(),
                "modes", new String[] { "http", "sse" });
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP");
    }
}
