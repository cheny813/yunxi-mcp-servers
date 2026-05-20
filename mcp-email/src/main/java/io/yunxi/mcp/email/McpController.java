package io.yunxi.mcp.email;

import io.yunxi.mcp.common.server.McpHttpEndpoint;
import io.yunxi.mcp.common.server.McpSseEndpoint;
import io.yunxi.mcp.email.tool.EmailTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MCP Email 控制器
 * <p>
 * 提供邮件发送的 MCP 端点，支持 HTTP 和 SSE 两种调用模式。
 * </p>
 *
 * <h3>提供的工具</h3>
 * <ul>
 * <li>send_email - 发送邮件</li>
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

    /**
     * 构造函数
     * <p>
     * 初始化 HTTP 和 SSE 端点，并注册邮件发送工具。
     * </p>
     */
    public McpController(JavaMailSender mailSender,
            @Value("${spring.mail.username:}") String fromAddress) {
        log.info("初始化 MCP Email 控制器...");

        String from = fromAddress != null ? fromAddress : "noreply@example.com";
        log.info("发件人地址: {}", from);

        this.httpEndpoint = new McpHttpEndpoint("yunxi-mcp-email", "1.0.0");
        this.sseEndpoint = new McpSseEndpoint("yunxi-mcp-email", "1.0.0");

        log.info("初始化 HTTP 和 SSE 端点完成");

        // 注册邮件发送工具
        httpEndpoint.registerTool(new EmailTools.SendEmailTool(mailSender, from));
        sseEndpoint.registerTool(new EmailTools.SendEmailTool(mailSender, from));

        log.info("MCP Email 控制器初始化完成，可用工具: {}",
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
                log.info("SSE 连接建立成功");
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
        log.info("返回服务器信息: name={}, version={}", info.get("name"), info.get("version"));
        return info;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        log.debug("收到健康检查请求");
        return Map.of("status", "UP");
    }

    @GetMapping("/tools")
    public Map<String, Object> getTools() {
        log.debug("收到获取工具列表请求");
        Map<String, Object> tools = Map.of(
                "tools", httpEndpoint.getTools().stream()
                        .map(tool -> Map.of(
                                "name", tool.getName(),
                                "description", tool.getDescription(),
                                "inputSchema", tool.getInputSchema()))
                        .toList());
        log.info("返回工具列表: {}", httpEndpoint.getTools().stream().map(t -> t.getName()).toList());
        return tools;
    }
}
