package io.yunxi.mcp.nutrition.tool;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.nutrition.service.DishQueryService;
import io.yunxi.mcp.nutrition.service.DishQueryService.DishDto;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DishSearchTool implements ToolHandler {

    private final DishQueryService dishQueryService;

    public DishSearchTool(DishQueryService dishQueryService) {
        this.dishQueryService = dishQueryService;
    }

    @Override
    public String getName() { return "searchDishes"; }

    @Override
    public String getDescription() {
        return "搜索菜品，支持按关键词搜索或按分类查询，返回菜品名称、分类、营养成分等信息";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "keyword", Map.of("type", "string", "description", "搜索关键词，按菜品名称模糊匹配"),
                        "category", Map.of("type", "string", "description", "菜品分类编码：1=荤菜 2=素菜 3=汤品 4=主食 5=点心 6=饮品"),
                        "limit", Map.of("type", "number", "description", "返回数量上限，默认 20")),
                "required", List.of());
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        String keyword = (String) arguments.get("keyword");
        String category = (String) arguments.get("category");
        int limit = arguments.containsKey("limit") ? ((Number) arguments.get("limit")).intValue() : 20;

        List<DishDto> results;
        if (keyword != null && !keyword.isBlank()) {
            results = dishQueryService.searchDishes(keyword, limit);
        } else if (category != null && !category.isBlank()) {
            results = dishQueryService.getDishesByCategory(category, limit);
        } else {
            throw new RuntimeException("请提供 keyword（关键词搜索）或 category（分类查询）参数");
        }

        if (results.isEmpty()) {
            return "未找到匹配的菜品";
        }

        String text = results.stream()
                .map(d -> String.format("- %s（%s）| 蛋白质%.1fg 脂肪%.1fg 碳水%.1fg 热量%.0fkcal",
                        d.getName(), d.getCategory(),
                        d.getProtein(), d.getFat(), d.getCarb(), d.getCalories()))
                .collect(Collectors.joining("\n"));

        return "找到 " + results.size() + " 个菜品：\n" + text;
    }
}
