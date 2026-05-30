package io.yunxi.mcp.nutrition;

import io.yunxi.mcp.common.controller.AbstractMcpController;
import io.yunxi.mcp.nutrition.service.DishQueryService;
import io.yunxi.mcp.nutrition.tool.DishSearchTool;
import io.yunxi.mcp.nutrition.tool.RecipeEvaluateTool;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 营养数据 MCP 服务端
 *
 * <p>独立部署的 MCP 服务，通过 JSON-RPC 2.0 over HTTP 与 yunxi-agent-platform 通信。
 * 提供学校营养餐领域的菜品搜索、食材查询、营养标准评估等工具。</p>
 *
 * <p>注册到平台的方式（在 agent-definitions YAML 中）：</p>
 * <pre>
 * tools:
 *   mcpServers:
 *     - name: nutrition-mcp
 *       type: http
 *       url: http://localhost:40112
 * </pre>
 *
 * @author yunxi-agent-platform
 */
@SpringBootApplication
public class NutritionMcpServerApplication extends AbstractMcpController {

    private final DishQueryService dishQueryService;

    public NutritionMcpServerApplication(DishQueryService dishQueryService) {
        this.dishQueryService = dishQueryService;
    }

    public static void main(String[] args) {
        SpringApplication.run(NutritionMcpServerApplication.class, args);
    }

    @Override
    protected String getServerName() {
        return "nutrition-mcp";
    }

    @Override
    protected void registerTools() {
        // 注册菜品搜索工具（按关键词/分类查询）
        registerTool(new DishSearchTool(dishQueryService));

        // 注册食谱营养评估工具（基于 ScoringEngine 算法）
        registerTool(new RecipeEvaluateTool());
    }
}