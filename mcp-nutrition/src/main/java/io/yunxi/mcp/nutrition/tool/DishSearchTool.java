package io.yunxi.mcp.nutrition.tool;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.*;
import io.yunxi.mcp.nutrition.service.DishQueryService;
import io.yunxi.mcp.nutrition.service.DishQueryService.DishDto;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 菜品搜索 MCP 工具
 *
 * <p>提供按关键词搜索和按分类查询菜品的能力。<br>
 * 继承 {@link ToolHandler} 接口，由 mcp-common 的 JSON-RPC 协议自动暴露。</p>
 *
 * <p>向 AI Agent 暴露两个操作：</p>
 * <ul>
 *   <li>searchDishes — 按关键词模糊搜索菜品名称</li>
 *   <li>getDishesByCategory — 按分类编码查询菜品列表</li>
 * </ul>
 *
 * @author yunxi-agent-platform
 */
@Component
public class DishSearchTool implements ToolHandler {

    private final DishQueryService dishQueryService;

    public DishSearchTool(DishQueryService dishQueryService) {
        this.dishQueryService = dishQueryService;
    }

    @Override
    public ToolDefinition getDefinition() {
        return ToolDefinition.builder()
                .name("searchDishes")
                .description("搜索菜品，支持按关键词搜索或按分类查询，返回菜品名称、分类、营养成分等信息")
                .parameters(List.of(
                        ToolParameter.builder()
                                .name("keyword")
                                .type("string")
                                .description("搜索关键词，按菜品名称模糊匹配，如\"红烧肉\"、\"番茄炒蛋\"")
                                .required(false)
                                .build(),
                        ToolParameter.builder()
                                .name("category")
                                .type("string")
                                .description("菜品分类编码：1=荤菜 2=素菜 3=汤品 4=主食 5=点心 6=饮品")
                                .required(false)
                                .build(),
                        ToolParameter.builder()
                                .name("limit")
                                .type("number")
                                .description("返回数量上限，默认 20")
                                .required(false)
                                .build()))
                .returnType("array")
                .metadata(ToolMetadata.builder()
                        .category("nutrition")
                        .tags(List.of("dish", "search", "nutrition"))
                        .build())
                .build();
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        String keyword = (String) arguments.get("keyword");
        String category = (String) arguments.get("category");
        int limit = arguments.containsKey("limit") ? ((Number) arguments.get("limit")).intValue() : 20;

        List<DishDto> results;
        if (keyword != null && !keyword.isBlank()) {
            // 按关键词搜索
            results = dishQueryService.searchDishes(keyword, limit);
        } else if (category != null && !category.isBlank()) {
            // 按分类查询
            results = dishQueryService.getDishesByCategory(category, limit);
        } else {
            return ToolResult.error("请提供 keyword（关键词搜索）或 category（分类查询）参数");
        }

        if (results.isEmpty()) {
            return ToolResult.text("未找到匹配的菜品");
        }

        // 格式化为可读文本
        String text = results.stream()
                .map(d -> String.format("- %s（%s）| 蛋白质%.1fg 脂肪%.1fg 碳水%.1fg 热量%.0fkcal",
                        d.getName(), d.getCategory(),
                        d.getProtein(), d.getFat(), d.getCarb(), d.getCalories()))
                .collect(Collectors.joining("\n"));

        return ToolResult.text("找到 " + results.size() + " 个菜品：\n" + text);
    }
}