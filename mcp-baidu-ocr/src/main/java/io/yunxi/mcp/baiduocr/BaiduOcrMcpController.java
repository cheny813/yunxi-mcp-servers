package io.yunxi.mcp.baiduocr;

import io.yunxi.mcp.baiduocr.tool.BaiduOcrTools;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 百度 OCR MCP 控制器
 */
@RestController
@RequestMapping("/mcp")
@ConditionalOnProperty(name = "baidu-ocr.enabled", havingValue = "true", matchIfMissing = true)
public class BaiduOcrMcpController {

    private final BaiduOcrTools.GeneralOcrTool generalOcrTool;
    private final BaiduOcrTools.AccurateOcrTool accurateOcrTool;

    public BaiduOcrMcpController(
            BaiduOcrTools.GeneralOcrTool generalOcrTool,
            BaiduOcrTools.AccurateOcrTool accurateOcrTool) {
        this.generalOcrTool = generalOcrTool;
        this.accurateOcrTool = accurateOcrTool;
    }

    /**
     * 获取工具列表
     */
    @GetMapping("/tools")
    public List<ToolDefinition> listTools() {
        return List.of(
                generalOcrTool.getDefinition(),
                accurateOcrTool.getDefinition()
        );
    }

    /**
     * 调用工具
     */
    @PostMapping("/execute")
    public ToolResult execute(@RequestBody Map<String, Object> request) {
        String toolName = (String) request.get("name");
        @SuppressWarnings("unchecked")
        Map<String, Object> args = (Map<String, Object>) request.get("arguments");

        if (toolName == null) {
            return ToolResult.error("Error: tool name is required");
        }

        return switch (toolName) {
            case "baidu_ocr_general" -> generalOcrTool.execute(args != null ? args : Map.of());
            case "baidu_ocr_accurate" -> accurateOcrTool.execute(args != null ? args : Map.of());
            default -> ToolResult.error("Error: unknown tool: " + toolName);
        };
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "healthy",
                "service", "baidu-ocr",
                "tools", List.of("baidu_ocr_general", "baidu_ocr_accurate")
        );
    }
}