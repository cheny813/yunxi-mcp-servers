package io.yunxi.mcp.pagegen;

import io.yunxi.mcp.pagegen.service.PageGenService;
import io.yunxi.mcp.pagegen.tool.PageGenTools;
import io.yunxi.mcp.common.server.McpHttpEndpoint;
import io.yunxi.mcp.common.server.McpSseEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * MCP Page Generator Controller
 */
@Slf4j
@RestController
@RequestMapping("/mcp")
public class McpController {

    private final McpHttpEndpoint httpEndpoint;
    private final McpSseEndpoint sseEndpoint;

    public McpController(PageGenService pageGenService) {
        log.info("Initializing MCP Page Generator Controller...");

        this.httpEndpoint = new McpHttpEndpoint("yunxi-mcp-pagegen", "1.0.0");
        this.sseEndpoint = new McpSseEndpoint("yunxi-mcp-pagegen", "1.0.0");

        PageGenTools tools = new PageGenTools(pageGenService);

        // Register tools
        httpEndpoint.registerTool(tools.generateDashboard());
        httpEndpoint.registerTool(tools.generateList());
        httpEndpoint.registerTool(tools.generateDetail());

        sseEndpoint.registerTool(tools.generateDashboard());
        sseEndpoint.registerTool(tools.generateList());
        sseEndpoint.registerTool(tools.generateDetail());

        log.info("MCP Page Generator Controller initialized. Registered 3 tools.");
    }

    @PostMapping
    public String handleHttpRequest(@RequestBody String requestJson) {
        return httpEndpoint.handleRequest(requestJson);
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
                "service", "yunxi-mcp-pagegen");
    }
}
