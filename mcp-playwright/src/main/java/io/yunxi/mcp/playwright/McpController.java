package io.yunxi.mcp.playwright;

import io.yunxi.mcp.playwright.service.PlaywrightService;
import io.yunxi.mcp.playwright.tool.PlaywrightTools;
import io.yunxi.mcp.common.server.McpHttpEndpoint;
import io.yunxi.mcp.common.server.McpSseEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.Executors;

/**
 * MCP Playwright Controller
 */
@Slf4j
@RestController
@RequestMapping("/mcp")
public class McpController {

    private final McpHttpEndpoint httpEndpoint;
    private final McpSseEndpoint sseEndpoint;

    public McpController(PlaywrightService playwrightService) {
        log.info("Initializing MCP Playwright Controller...");

        this.httpEndpoint = new McpHttpEndpoint("yunxi-mcp-playwright", "1.0.0");
        this.sseEndpoint = new McpSseEndpoint("yunxi-mcp-playwright", "1.0.0");

        PlaywrightTools tools = new PlaywrightTools(playwrightService);

        // Register tools
        httpEndpoint.registerTool(tools.startBrowser());
        httpEndpoint.registerTool(tools.navigate());
        httpEndpoint.registerTool(tools.getPageContent());
        httpEndpoint.registerTool(tools.click());
        httpEndpoint.registerTool(tools.type());
        httpEndpoint.registerTool(tools.closeBrowser());

        sseEndpoint.registerTool(tools.startBrowser());
        sseEndpoint.registerTool(tools.navigate());
        sseEndpoint.registerTool(tools.getPageContent());
        sseEndpoint.registerTool(tools.click());
        sseEndpoint.registerTool(tools.type());
        sseEndpoint.registerTool(tools.closeBrowser());

        log.info("MCP Playwright Controller initialized. Registered 6 tools.");
    }

    @PostMapping
    public String handleHttpRequest(@RequestBody String requestJson) {
        return httpEndpoint.handleRequest(requestJson);
    }

    /**
     * SSE 端点：建立 SSE 连接
     */
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter handleSse() {
        log.info("SSE 连接建立: /sse");
        SseEmitter emitter = new SseEmitter(0L);
        java.util.concurrent.Executors.newCachedThreadPool().execute(() -> {
            try {
                // 发送 endpoint 事件
                emitter.send(SseEmitter.event()
                        .name("endpoint")
                        .data(sseEndpoint.getMessageEndpoint()));
            } catch (Exception e) {
                log.error("SSE endpoint 事件发送失败", e);
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    /**
     * 消息端点：处理 JSON-RPC 请求
     */
    @PostMapping("/message")
    public String handleMessage(@RequestBody String requestJson) {
        return sseEndpoint.handleMessage(requestJson);
    }

    @GetMapping("/tools")
    public Map<String, Object> getTools() {
        return Map.of(
                "tools", httpEndpoint.getTools().stream()
                        .map(tool -> Map.of(
                                "name", tool.getName(),
                                "description", tool.getDescription(),
                                "inputSchema", tool.getInputSchema()))
                        .toList());
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "yunxi-mcp-playwright");
    }
}
