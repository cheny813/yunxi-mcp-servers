package io.yunxi.mcp.mqtt;

import io.yunxi.mcp.mqtt.service.MqttService;
import io.yunxi.mcp.mqtt.tool.MqttTools;
import io.yunxi.mcp.common.server.McpHttpEndpoint;
import io.yunxi.mcp.common.server.McpSseEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MCP MQTT 控制器
 * 
 * 提供 MQTT 实时订阅/发布工具。
 * 历史数据查询请使用 mcp-database 查询 MySQL。
 */
@Slf4j
@RestController
@RequestMapping("/mcp")
public class McpController {

    private final McpHttpEndpoint httpEndpoint;
    private final McpSseEndpoint sseEndpoint;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public McpController(MqttService mqttService) {
        log.info("Initializing MCP MQTT Controller...");
        
        this.httpEndpoint = new McpHttpEndpoint("yunxi-mcp-mqtt", "1.0.0");
        this.sseEndpoint = new McpSseEndpoint("yunxi-mcp-mqtt", "1.0.0");

        MqttTools mqttTools = new MqttTools(mqttService);

        // 注册 MQTT 工具
        httpEndpoint.registerTool(mqttTools.subscribe());
        httpEndpoint.registerTool(mqttTools.unsubscribe());
        httpEndpoint.registerTool(mqttTools.publish());
        httpEndpoint.registerTool(mqttTools.queryRealtime());
        httpEndpoint.registerTool(mqttTools.status());

        sseEndpoint.registerTool(mqttTools.subscribe());
        sseEndpoint.registerTool(mqttTools.unsubscribe());
        sseEndpoint.registerTool(mqttTools.publish());
        sseEndpoint.registerTool(mqttTools.queryRealtime());
        sseEndpoint.registerTool(mqttTools.status());

        log.info("MCP MQTT Controller initialized. Registered 5 tools: subscribe, unsubscribe, publish, queryRealtime, status");
    }

    @PostMapping
    public String handleHttpRequest(@RequestBody String requestJson) {
        log.debug("Received HTTP MCP request: {}", 
                requestJson.length() > 200 ? requestJson.substring(0, 200) + "..." : requestJson);
        return httpEndpoint.handleRequest(requestJson);
    }

    @GetMapping("/tools")
    public Map<String, Object> getTools() {
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
        
        log.info("Returning tool list: {} tools: {}", toolNames.size(), toolNames);
        return tools;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "yunxi-mcp-mqtt",
                "description", "MQTT real-time subscribe/publish service. For historical data, use mcp-database."
        );
    }
}
