package io.yunxi.mcp.wechat;

import io.yunxi.mcp.wechat.tool.WeChatTools;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 企业微信 MCP 控制器
 *
 * <p>
 * 提供标准 MCP HTTP 接口，供 NotificationService 调用。
 * </p>
 *
 * @author yunxi-mcp-servers
 * @version 1.0.0
 */
@RestController
@RequestMapping("/mcp")
@ConditionalOnProperty(name = "wechat.enabled", havingValue = "true", matchIfMissing = true)
public class WeChatMcpController {

    private final WeChatTools.SendTextTool sendTextTool;
    private final WeChatTools.SendMarkdownTool sendMarkdownTool;

    public WeChatMcpController(
            WeChatTools.SendTextTool sendTextTool,
            WeChatTools.SendMarkdownTool sendMarkdownTool) {
        this.sendTextTool = sendTextTool;
        this.sendMarkdownTool = sendMarkdownTool;
    }

    @GetMapping("/tools")
    public List<ToolDefinition> listTools() {
        return List.of(
                sendTextTool.getDefinition(),
                sendMarkdownTool.getDefinition()
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
            case "wechat_send_text" -> sendTextTool.execute(args != null ? args : Map.of());
            case "wechat_send_markdown" -> sendMarkdownTool.execute(args != null ? args : Map.of());
            default -> ToolResult.error("Error: unknown tool: " + toolName);
        };
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "healthy",
                "service", "wechat",
                "tools", List.of("wechat_send_text", "wechat_send_markdown")
        );
    }
}
