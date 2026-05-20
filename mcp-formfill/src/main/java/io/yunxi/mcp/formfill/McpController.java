package io.yunxi.mcp.formfill;

import io.yunxi.mcp.common.server.McpHttpEndpoint;
import io.yunxi.mcp.formfill.tool.FormFillerTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MCP 表单填写服务器控制器
 *
 * 提供以下工具：
 * - formfill_recipe: 填写营养食谱表单
 * - formfill_weekly_recipe: 批量填写周计划食谱
 * - formfill_safety: 填写食品安全检测表单
 * - formfill_budget: 填写经费管理表单
 * - formfill_generic: 通用表单填写
 * - formfill_get_structure: 获取表单结构
 */
@Slf4j
@RestController
@RequestMapping("/mcp")
public class McpController {

    private final FormFillerTools formFillerTools;
    private final McpHttpEndpoint httpEndpoint;

    public McpController(FormFillerTools formFillerTools) {
        this.formFillerTools = formFillerTools;
        this.httpEndpoint = new McpHttpEndpoint("yunxi-mcp-formfill", "1.0.0");

        // 注册工具到 HTTP 端点
        httpEndpoint.registerTool(formFillerTools.fillRecipeForm());
        httpEndpoint.registerTool(formFillerTools.fillWeeklyRecipePlan());
        httpEndpoint.registerTool(formFillerTools.fillSafetyForm());
        httpEndpoint.registerTool(formFillerTools.fillBudgetForm());
        httpEndpoint.registerTool(formFillerTools.fillGenericForm());
        httpEndpoint.registerTool(formFillerTools.getFormStructure());

        log.info("MCP FormFill 控制器初始化完成，已注册 {} 个工具", 6);
    }

    /**
     * 处理 HTTP 模式的 MCP 请求
     *
     * @param requestJson JSON-RPC 请求 JSON 字符串
     * @return JSON-RPC 响应 JSON 字符串
     */
    @PostMapping
    public String handleHttpRequest(@RequestBody String requestJson) {
        log.debug("收到 HTTP MCP 请求: {}",
                requestJson.length() > 200 ? requestJson.substring(0, 200) + "..." : requestJson);
        String response = httpEndpoint.handleRequest(requestJson);
        log.debug("返回 HTTP MCP 响应: {}", response.length() > 200 ? response.substring(0, 200) + "..." : response);
        return response;
    }

    /**
     * 获取所有可用工具
     */
    @GetMapping("/tools")
    public Map<String, Object> getTools() {
        log.debug("收到获取工具列表请求");
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

        log.info("返回工具列表，共 {} 个工具: {}", toolNames.size(), toolNames);
        return tools;
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP");
    }
}
