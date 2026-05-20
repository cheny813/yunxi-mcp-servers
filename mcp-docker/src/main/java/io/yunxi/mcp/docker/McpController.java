package io.yunxi.mcp.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
import io.yunxi.mcp.common.server.McpHttpEndpoint;
import io.yunxi.mcp.common.server.McpSseEndpoint;
import io.yunxi.mcp.docker.tool.DockerTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MCP Docker 控制器
 * <p>
 * 提供 Docker 操作的 MCP 端点，支持 HTTP 和 SSE 两种调用模式。
 * </p>
 *
 * <h3>提供的工具</h3>
 * <ul>
 * <li>list_containers - 列出容器</li>
 * <li>list_images - 列出镜像</li>
 * <li>pull_image - 拉取镜像</li>
 * <li>start_container - 启动容器</li>
 * <li>stop_container - 停止容器</li>
 * </ul>
 *
 * <h3>测试端点</h3>
 * <ul>
 * <li>GET /mcp/health - 健康检查</li>
 * <li>GET /mcp/info - 服务器信息</li>
 * <li>GET /mcp/tools - 工具列表</li>
 * </ul>
 *
 * @see McpHttpEndpoint
 * @see McpSseEndpoint
 */
@RestController
@RequestMapping("/mcp")
public class McpController {

    private static final Logger log = LoggerFactory.getLogger(McpController.class);

    private final McpHttpEndpoint httpEndpoint;
    private final McpSseEndpoint sseEndpoint;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final DockerClient dockerClient;

    /**
     * 构造函数
     * <p>
     * 初始化 Docker 客户端和 MCP 端点，注册所有工具。
     * </p>
     */
    public McpController() {
        log.info("初始化 MCP Docker 控制器...");

        this.dockerClient = DockerClientBuilder.getInstance().build();

        this.httpEndpoint = new McpHttpEndpoint("yunxi-mcp-docker", "1.0.0");
        this.sseEndpoint = new McpSseEndpoint("yunxi-mcp-docker", "1.0.0");

        log.info("初始化 HTTP 和 SSE 端点完成");

        // 注册 Docker 操作工具到 HTTP 端点
        httpEndpoint.registerTool(new DockerTools.ListContainersTool(dockerClient));
        httpEndpoint.registerTool(new DockerTools.ListImagesTool(dockerClient));
        httpEndpoint.registerTool(new DockerTools.PullImageTool(dockerClient));
        httpEndpoint.registerTool(new DockerTools.StartContainerTool(dockerClient));
        httpEndpoint.registerTool(new DockerTools.StopContainerTool(dockerClient));
        log.info("已注册 {} 个工具到 HTTP 端点", httpEndpoint.getTools().size());

        // 注册 Docker 操作工具到 SSE 端点
        sseEndpoint.registerTool(new DockerTools.ListContainersTool(dockerClient));
        sseEndpoint.registerTool(new DockerTools.ListImagesTool(dockerClient));
        sseEndpoint.registerTool(new DockerTools.PullImageTool(dockerClient));
        sseEndpoint.registerTool(new DockerTools.StartContainerTool(dockerClient));
        sseEndpoint.registerTool(new DockerTools.StopContainerTool(dockerClient));
        log.info("已注册 {} 个工具到 SSE 端点", sseEndpoint.getTools().size());

        log.info("MCP Docker 控制器初始化完成，可用工具: {}",
                httpEndpoint.getTools().stream().map(tool -> tool.getName()).toList());
    }

    // ============ HTTP 模式 ============

    @PostMapping
    public String handleHttpRequest(@RequestBody String requestJson) {
        log.debug("收到 HTTP MCP 请求: {}", requestJson);
        String response = httpEndpoint.handleRequest(requestJson);
        log.debug("HTTP MCP 响应: {}", response);
        return response;
    }

    // ============ SSE 模式 ============

    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter handleSseConnection() {
        log.info("收到新的 SSE 连接请求");
        SseEmitter emitter = new SseEmitter(0L);

        executor.execute(() -> {
            try {
                emitter.send(SseEmitter.event().name("endpoint").data("/mcp/message"));
                log.info("SSE 连接建立成功，端点信息已发送: /mcp/message");
                emitter.onCompletion(() -> log.info("SSE 连接完成"));
                emitter.onTimeout(() -> log.warn("SSE 连接超时"));
                emitter.onError(e -> log.error("SSE 连接发生错误", e));
            } catch (Exception e) {
                log.error("发送 SSE 端点信息失败", e);
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    @PostMapping("/message")
    public String handleSseMessage(@RequestBody String requestJson) {
        log.debug("收到 SSE MCP 消息: {}", requestJson);
        String response = sseEndpoint.handleMessage(requestJson);
        log.debug("SSE MCP 响应: {}", response);
        return response;
    }

    // ============ 通用端点 ============

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

    @GetMapping("/health")
    public Map<String, Object> health() {
        log.debug("收到健康检查请求");
        Map<String, Object> healthStatus = Map.of("status", "UP");
        log.info("健康检查状态: {}", healthStatus);
        return healthStatus;
    }

    /**
     * 获取所有可用工具列表
     */
    @GetMapping("/tools")
    public Map<String, Object> getTools() {
        log.debug("收到获取工具列表请求");

        java.util.List<String> toolNames = httpEndpoint.getTools().stream()
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
}
