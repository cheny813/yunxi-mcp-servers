package io.yunxi.mcp.milvus.tool;

import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp;
import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import io.yunxi.mcp.milvus.config.MilvusClientHolder;
import io.yunxi.mcp.milvus.embedding.EmbeddingService;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 获取菜品详情工具
 * <p>
 * 根据菜品ID获取菜品的详细信息。
 * 支持通过 dishId 精确查询（如果 Milvus 支持过滤）或通过搜索匹配获取。
 * </p>
 */
@Slf4j
public class GetDishDetailsTool implements ToolHandler {

    private final MilvusClientHolder clientHolder;
    private final EmbeddingService embeddingService;

    private static final String COLLECTION_PREFIX = "school_dishes_";
    private static final String COMMON_DISHES_COLLECTION = "school_common_dishes";

    public GetDishDetailsTool(MilvusClientHolder clientHolder, EmbeddingService embeddingService) {
        this.clientHolder = clientHolder;
        this.embeddingService = embeddingService;
    }

    @Override
    public ToolDefinition getDefinition() {
        return ToolDefinition.builder()
                .name("get_dish_details")
                .description(
                        "【已修复】根据菜品ID获取菜品详细信息。" +
                                "支持精确查询，返回包含 nutrients、ingredientIds 等完整信息。" +
                                "参数：schoolId(学校ID), dishId(菜品ID)。")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "schoolId", Map.of(
                                        "type", "integer",
                                        "description", "学校ID"),
                                "dishId", Map.of(
                                        "type", "integer",
                                        "description", "菜品ID")),
                        "required", Arrays.asList("schoolId", "dishId")))
                .build();
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        Long schoolId = getLongArgument(arguments, "schoolId");
        Long dishId = getLongArgument(arguments, "dishId");

        if (schoolId == null) {
            return ToolResult.error("schoolId is required");
        }
        if (dishId == null) {
            return ToolResult.error("dishId is required");
        }

        log.info("【获取菜品详情】schoolId={}, dishId={}", schoolId, dishId);

        MilvusClientV2 milvusClient = clientHolder.getClient();
        if (milvusClient == null) {
            return ToolResult.error("Milvus 服务暂不可用，正在尝试重连");
        }

        try {
            // 1. 先尝试从学校专属菜品库查询
            Map<String, Object> dish = queryDishById(milvusClient, COLLECTION_PREFIX + schoolId, dishId);

            // 2. 如果学校库中没有，尝试从公共菜品库查询
            if (dish == null) {
                log.info("学校专属菜品库未找到，尝试从公共菜品库查询: dishId={}", dishId);
                dish = queryDishById(milvusClient, COMMON_DISHES_COLLECTION, dishId);
            }

            if (dish == null) {
                return ToolResult.error("未找到菜品ID为 " + dishId + " 的数据，请检查菜品是否存在");
            }

            // 3. 构建结果
            return ToolResult.text(formatDishDetails(dish));

        } catch (Exception e) {
            log.error("获取菜品详情失败: schoolId={}, dishId={}", schoolId, dishId, e);
            return ToolResult.error("获取菜品详情失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID查询菜品
     */
    private Map<String, Object> queryDishById(MilvusClientV2 client, String collectionName, Long dishId) {
        try {
            // 生成查询向量（使用dishId作为query）
            List<Float> queryVector = embeddingService.embed(String.valueOf(dishId));
            if (queryVector == null || queryVector.isEmpty()) {
                log.warn("生成查询向量失败: dishId={}", dishId);
                return null;
            }

            // 构建搜索请求，使用 filter 精确匹配 ID
            SearchReq searchReq = SearchReq.builder()
                    .collectionName(collectionName)
                    .annsField("embedding")
                    .data(Collections.singletonList(new FloatVec(queryVector)))
                    .topK(10) // 返回多个结果，然后精确匹配
                    .outputFields(Arrays.asList(
                            "id", "name", "type", "ingredients", "ingredient_ids",
                            "nutrients", "school_id", "update_time"))
                    .filter("id == " + dishId) // 精确匹配 dishId
                    .build();

            SearchResp response = client.search(searchReq);

            // 解析结果，找到精确匹配的 dishId
            List<List<SearchResp.SearchResult>> searchResults = response.getSearchResults();
            if (searchResults != null && !searchResults.isEmpty()) {
                for (SearchResp.SearchResult hit : searchResults.get(0)) {
                    Map<String, Object> entity = hit.getEntity();
                    Long hitId = getLongValue(entity, "id");
                    if (dishId.equals(hitId)) {
                        return entity;
                    }
                }
            }

        } catch (Exception e) {
            log.warn("查询集合 {} 失败: {}", collectionName, e.getMessage());
        }

        return null;
    }

    /**
     * 格式化菜品详情
     */
    private String formatDishDetails(Map<String, Object> dish) {
        StringBuilder sb = new StringBuilder();
        sb.append("菜品详情:\n");
        sb.append("- dishId: ").append(getLongValue(dish, "id")).append("\n");
        sb.append("- name: ").append(getStringValue(dish, "name")).append("\n");
        sb.append("- type: ").append(getStringValue(dish, "type")).append("\n");
        sb.append("- ingredients: ").append(getStringValue(dish, "ingredients")).append("\n");
        sb.append("- ingredientIds: ").append(getStringValue(dish, "ingredient_ids")).append("\n");

        // 特殊处理 nutrients 字段
        Object nutrients = dish.get("nutrients");
        if (nutrients != null) {
            sb.append("- nutrients: ").append(nutrients).append("\n");
        } else {
            sb.append("- nutrients: ⚠️ [数据缺失] 该菜品缺少营养成分数据\n");
            sb.append("  影响: 无法进行营养配平计算和评分\n");
            sb.append("  建议: 请补充该菜品的营养成分（能量、蛋白质、脂肪、碳水化合物等）\n");
        }

        sb.append("- schoolId: ").append(getLongValue(dish, "school_id")).append("\n");
        sb.append("- updateTime: ").append(getStringValue(dish, "update_time")).append("\n");

        return sb.toString();
    }

    /**
     * 从Map中获取Long值
     */
    private Long getLongValue(Map<String, Object> entity, String key) {
        Object value = entity.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    /**
     * 从Map中获取String值
     */
    private String getStringValue(Map<String, Object> entity, String key) {
        Object value = entity.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 获取 Long 类型参数
     */
    private Long getLongArgument(Map<String, Object> arguments, String key) {
        Object value = arguments.get(key);
        if (value == null)
            return null;
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
