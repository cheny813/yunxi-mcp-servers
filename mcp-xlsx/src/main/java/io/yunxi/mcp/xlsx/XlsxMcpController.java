package io.yunxi.mcp.xlsx;

import io.yunxi.mcp.common.server.McpHttpEndpoint;
import io.yunxi.mcp.common.server.McpSseEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MCP XLSX Controller
 */
@Slf4j
@RestController
@RequestMapping("/mcp")
public class XlsxMcpController {

    private final McpHttpEndpoint httpEndpoint;
    private final McpSseEndpoint sseEndpoint;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final XlsxTools.XlsxTool xlsxTool;

    public XlsxMcpController(XlsxTools xlsxTools) {
        log.info("初始化 MCP XLSX 控制器...");

        this.httpEndpoint = new McpHttpEndpoint("yunxi-mcp-xlsx", "1.0.0");
        this.sseEndpoint = new McpSseEndpoint("yunxi-mcp-xlsx", "1.0.0");

        this.xlsxTool = new XlsxTools.XlsxTool(xlsxTools);

        httpEndpoint.registerTool(xlsxTool);
        sseEndpoint.registerTool(xlsxTool);

        log.info("MCP XLSX 控制器初始化完成，已注册工具: xlsx_operation");
    }

    @PostMapping
    public String handleHttpRequest(@RequestBody String requestJson) {
        return httpEndpoint.handleRequest(requestJson);
    }

    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter handleSseConnection() {
        SseEmitter emitter = new SseEmitter(0L);

        executor.execute(() -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("endpoint")
                        .data("/mcp/message"));

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
        return sseEndpoint.handleMessage(requestJson);
    }

    @GetMapping("/tools")
    public Map<String, Object> getTools() {
        return Map.of("tools", httpEndpoint.getTools().stream()
                .map(tool -> Map.of(
                        "name", tool.getName(),
                        "description", tool.getDescription(),
                        "inputSchema", tool.getInputSchema()))
                .toList());
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP");
    }
}