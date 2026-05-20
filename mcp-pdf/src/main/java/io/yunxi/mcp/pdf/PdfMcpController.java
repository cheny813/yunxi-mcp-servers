package io.yunxi.mcp.pdf;

import io.yunxi.mcp.common.server.McpHttpEndpoint;
import io.yunxi.mcp.common.server.McpSseEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MCP PDF Controller
 */
@Slf4j
@RestController
@RequestMapping("/mcp")
public class PdfMcpController {

    private final McpHttpEndpoint httpEndpoint;
    private final McpSseEndpoint sseEndpoint;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final PdfTools.PdfTool pdfTool;

    public PdfMcpController(PdfTools pdfTools) {
        log.info("初始化 MCP PDF 控制器...");

        this.httpEndpoint = new McpHttpEndpoint("yunxi-mcp-pdf", "1.0.0");
        this.sseEndpoint = new McpSseEndpoint("yunxi-mcp-pdf", "1.0.0");

        this.pdfTool = new PdfTools.PdfTool(pdfTools);

        httpEndpoint.registerTool(pdfTool);
        sseEndpoint.registerTool(pdfTool);

        log.info("MCP PDF 控制器初始化完成，已注册工具: pdf_operation");
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