package io.yunxi.mcp.baiduasr;

import io.yunxi.mcp.baiduasr.tool.BaiduAsrTools;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 百度 ASR MCP 控制器
 */
@RestController
@RequestMapping("/mcp")
@ConditionalOnProperty(name = "baidu-asr.enabled", havingValue = "true", matchIfMissing = true)
public class BaiduAsrMcpController {

    private final BaiduAsrTools.SpeechRecognitionTool speechRecognitionTool;

    public BaiduAsrMcpController(BaiduAsrTools.SpeechRecognitionTool speechRecognitionTool) {
        this.speechRecognitionTool = speechRecognitionTool;
    }

    @GetMapping("/tools")
    public List<ToolDefinition> listTools() {
        return List.of(speechRecognitionTool.getDefinition());
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
            case "baidu_asr" -> speechRecognitionTool.execute(args != null ? args : Map.of());
            default -> ToolResult.error("Error: unknown tool: " + toolName);
        };
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "healthy",
                "service", "baidu-asr",
                "tools", List.of("baidu_asr")
        );
    }
}