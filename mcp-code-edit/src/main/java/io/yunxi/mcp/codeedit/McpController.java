package io.yunxi.mcp.codeedit;

import io.yunxi.mcp.common.server.McpHttpEndpoint;
import io.yunxi.mcp.common.server.McpSseEndpoint;
import io.yunxi.mcp.codeedit.service.CodeEditService;
import io.yunxi.mcp.codeedit.tool.EditCodeTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MCP 代码编辑控制器
 * <p>
 * 提供代码文件编辑的 MCP 端点，支持 HTTP 和 SSE 两种调用模式。
 * </p>
 *
 * <h3>提供工具</h3>
 * <ul>
 * <li>edit_code - 编辑代码文件</li>
 * </ul>
 *
 * @author yunxi-mcp-servers
 */
@RestController
@RequestMapping("/mcp")
public class McpController {

    private static final Logger log = LoggerFactory.getLogger(McpController.class);

    private final McpHttpEndpoint httpEndpoint;
    private final McpSseEndpoint sseEndpoint;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Autowired
    public McpController(CodeEditService codeEditService) {
        log.info("初始化 MCP Code Edit 控制器...");

        this.httpEndpoint = new McpHttpEndpoint("yunxi-mcp-code-edit", "1.0.0");
        this.sseEndpoint = new McpSseEndpoint("yunxi-mcp-code-edit", "1.0.0");

        // 注册工具
        httpEndpoint.registerTool(new EditCodeTool(codeEditService));
        sseEndpoint.registerTool(new EditCodeTool(codeEditService));

        log.info("MCP Code Edit 控制器初始化完成");
    }

    @PostMapping
    public String handleHttpRequest(@RequestBody String requestJson) {
        log.debug("收到 HTTP MCP 请求: {}", requestJson);
        String response = httpEndpoint.handleRequest(requestJson);
        log.debug("HTTP MCP 响应: {}", response);
        return response;
    }

    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter handleSseConnection() {
        SseEmitter emitter = new SseEmitter(0L);

        executor.execute(() -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("endpoint")
                        .data("/mcp/message"));
                log.info("SSE 连接建立成功");

                emitter.onCompletion(() -> log.info("SSE 连接完成"));
                emitter.onTimeout(() -> log.warn("SSE 连接超时"));
                emitter.onError(e -> log.error("SSE 连接错误", e));
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

    @GetMapping("/info")
    public Map<String, Object> getInfo() {
        return Map.of(
                "name", httpEndpoint.getServerInfo().get("name"),
                "version", httpEndpoint.getServerInfo().get("version"),
                "tools", httpEndpoint.getTools());
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP");
    }

    @GetMapping("/tools")
    public Map<String, Object> getTools() {
        return Map.of(
                "tools", httpEndpoint.getTools().stream()
                        .map(tool -> Map.of(
                                "name", tool.getName(),
                                "description", tool.getDescription()))
                        .toList());
    }
}