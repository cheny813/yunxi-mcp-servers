package io.yunxi.mcp.monitoring;

import io.yunxi.mcp.common.server.McpHttpEndpoint;
import io.yunxi.mcp.common.server.McpSseEndpoint;
import io.yunxi.mcp.monitoring.service.AlertService;
import io.yunxi.mcp.monitoring.service.MetricsService;
import io.yunxi.mcp.monitoring.tool.*;
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
 * 系统监控 MCP 控制器
 *
 * <p>
 * 注册所有监控工具，提供 HTTP 和 SSE 两种 MCP 协议接入方式。
 * </p>
 *
 * <h3>注册工具</h3>
 * <ul>
 * <li>get_system_metrics - 获取系统指标</li>
 * <li>get_jvm_metrics - 获取 JVM 指标</li>
 * <li>check_health - 执行健康检查</li>
 * <li>list_alerts - 列出告警</li>
 * <li>set_alert_rule - 设置告警规则</li>
 * </ul>
 *
 * @author yunxi-mcp-servers
 * @version 1.0.0
 */
@RestController
@RequestMapping("/mcp")
public class MonitoringMcpController {

    private static final Logger log = LoggerFactory.getLogger(MonitoringMcpController.class);

    private final McpHttpEndpoint httpEndpoint;
    private final McpSseEndpoint sseEndpoint;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Autowired
    public MonitoringMcpController(MetricsService metricsService, AlertService alertService) {
        log.info("初始化 MCP Monitoring 控制器...");

        this.httpEndpoint = new McpHttpEndpoint("yunxi-mcp-monitoring", "1.0.0");
        this.sseEndpoint = new McpSseEndpoint("yunxi-mcp-monitoring", "1.0.0");

        // 注册工具
        httpEndpoint.registerTool(new GetSystemMetricsTool(metricsService));
        httpEndpoint.registerTool(new GetJvmMetricsTool(metricsService));
        httpEndpoint.registerTool(new CheckHealthTool(alertService));
        httpEndpoint.registerTool(new ListAlertsTool(alertService));
        httpEndpoint.registerTool(new SetAlertRuleTool(alertService));

        sseEndpoint.registerTool(new GetSystemMetricsTool(metricsService));
        sseEndpoint.registerTool(new GetJvmMetricsTool(metricsService));
        sseEndpoint.registerTool(new CheckHealthTool(alertService));
        sseEndpoint.registerTool(new ListAlertsTool(alertService));
        sseEndpoint.registerTool(new SetAlertRuleTool(alertService));

        log.info("MCP Monitoring 控制器初始化完成，已注册 5 个工具");
    }

    @PostMapping
    public String handleHttpRequest(@RequestBody String requestJson) {
        log.debug("收到 HTTP MCP 请求: {}", requestJson);
        return httpEndpoint.handleRequest(requestJson);
    }

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

    @PostMapping("/message")
    public String handleSseMessage(@RequestBody String requestJson) {
        log.debug("收到 SSE MCP 消息: {}", requestJson);
        return sseEndpoint.handleMessage(requestJson);
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
