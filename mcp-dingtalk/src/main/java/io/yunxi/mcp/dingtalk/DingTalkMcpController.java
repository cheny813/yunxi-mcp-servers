package io.yunxi.mcp.dingtalk;

import io.yunxi.mcp.dingtalk.tool.DingTalkTools;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 钉钉 MCP 控制器
 */
@RestController
@RequestMapping("/mcp")
@ConditionalOnProperty(name = "dingtalk.enabled", havingValue = "true", matchIfMissing = true)
public class DingTalkMcpController {

    private final DingTalkTools.SendTextTool sendTextTool;
    private final DingTalkTools.SendMarkdownTool sendMarkdownTool;
    private final DingTalkTools.SendLinkTool sendLinkTool;

    public DingTalkMcpController(
            DingTalkTools.SendTextTool sendTextTool,
            DingTalkTools.SendMarkdownTool sendMarkdownTool,
            DingTalkTools.SendLinkTool sendLinkTool) {
        this.sendTextTool = sendTextTool;
        this.sendMarkdownTool = sendMarkdownTool;
        this.sendLinkTool = sendLinkTool;
    }

    @GetMapping("/tools")
    public List<ToolDefinition> listTools() {
        return List.of(
                sendTextTool.getDefinition(),
                sendMarkdownTool.getDefinition(),
                sendLinkTool.getDefinition()
        );
    }

    @PostMapping("/execute")
    public ToolResult execute(@RequestBody Map<String, Object> request) {
        String toolName = (String) request.get("name");
        @SuppressWarnings("unchecked")
        Map<String, Object> args = (Map<String, Object>) request.get("arguments");

        if (toolName == null) {
            return ToolResult.error("Error: tool name is required");
        }

        return switch (toolName) {
            case "dingtalk_send_text" -> sendTextTool.execute(args != null ? args : Map.of());
            case "dingtalk_send_markdown" -> sendMarkdownTool.execute(args != null ? args : Map.of());
            case "dingtalk_send_link" -> sendLinkTool.execute(args != null ? args : Map.of());
            default -> ToolResult.error("Error: unknown tool: " + toolName);
        };
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "healthy",
                "service", "dingtalk",
                "tools", List.of("dingtalk_send_text", "dingtalk_send_markdown", "dingtalk_send_link")
        );
    }
}