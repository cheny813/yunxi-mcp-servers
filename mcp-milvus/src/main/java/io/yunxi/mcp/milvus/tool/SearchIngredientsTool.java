package io.yunxi.mcp.milvus.tool;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import io.yunxi.mcp.milvus.config.MilvusClientHolder;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 搜索食材工具
 * <p>
 * 通过 Milvus 向量搜索食材库中的食材。
 * </p>
 */
@Slf4j
public class SearchIngredientsTool implements ToolHandler {

    private final MilvusClientHolder clientHolder;

    public SearchIngredientsTool(MilvusClientHolder clientHolder) {
        this.clientHolder = clientHolder;
    }

    @Override
    public ToolDefinition getDefinition() {
        return ToolDefinition.builder()
                .name("search_ingredients")
                .description(
                        "【重要】搜索食材库中的食材。" +
                                "当需要查询食材信息、获取食材ID（ingredientId）时使用。" +
                                "参数：query(搜索关键词，如'鸭肉'、'白菜'), schoolId(学校ID), topK(返回数量，默认20)。" +
                                "返回结果包含：id(ingredientId), name, category(分类), nutrients(营养成分)。")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "query", Map.of(
                                        "type", "string",
                                        "description", "搜索关键词，如'鸭肉'、'白菜'、'胡萝卜'"),
                                "schoolId", Map.of(
                                        "type", "integer",
                                        "description", "学校ID（可选，用于筛选学校专属食材）"),
                                "topK", Map.of(
                                        "type", "integer",
                                        "description", "返回数量，默认20",
                                        "default", 20)),
                        "required", List.of("query")))
                .build();
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        String query = (String) arguments.get("query");
        Integer topK = getIntArgument(arguments, "topK", 20);

        if (query == null || query.isBlank()) {
            return ToolResult.error("query is required");
        }

        log.info("【食材搜索】query={}, topK={}", query, topK);

        try {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("message", "食材搜索功能需 embedding 服务，当前返回提示");
            result.put("query", query);

            return ToolResult.text(formatResult(result, query));

        } catch (Exception e) {
            log.error("【食材搜索】失败: query={}", query, e);
            return ToolResult.error("搜索失败: " + e.getMessage());
        }
    }

    private String formatResult(Map<String, Object> result, String query) {
        StringBuilder sb = new StringBuilder();
        sb.append("食材搜索结果:\n");
        sb.append("- 查询词: ").append(query).append("\n");
        sb.append("- 注意: 食材搜索需要 embedding 服务支持\n");
        sb.append("- 建议: 使用 database MCP 工具查询 food_ingredient 表获取食材ID\n");
        return sb.toString();
    }

    private Integer getIntArgument(Map<String, Object> arguments, String key, int defaultValue) {
        Object value = arguments.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(value.toString());
    }
}
