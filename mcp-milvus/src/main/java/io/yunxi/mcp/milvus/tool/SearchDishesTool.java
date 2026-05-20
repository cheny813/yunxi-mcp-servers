package io.yunxi.mcp.milvus.tool;

import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.response.SearchResp;
import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import io.yunxi.mcp.milvus.config.MilvusClientHolder;
import io.yunxi.mcp.milvus.embedding.EmbeddingService;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 向量搜索菜品工具
 * <p>
 * 通过 Milvus 向量数据库搜索菜品：
 * <ul>
 * <li>有 schoolId：搜索学校专属菜品库 (school_dishes_{schoolId})</li>
 * <li>无 schoolId 或搜索结果不足：回退到公共菜品库 (school_common_dishes)</li>
 * </ul>
 * </p>
 */
@Slf4j
public class SearchDishesTool implements ToolHandler {

    private final MilvusClientHolder clientHolder;
    private final EmbeddingService embeddingService;

    private static final String COLLECTION_PREFIX = "school_dishes_";
    private static final String COMMON_DISHES_COLLECTION = "school_common_dishes";

    public SearchDishesTool(MilvusClientHolder clientHolder, EmbeddingService embeddingService) {
        this.clientHolder = clientHolder;
        this.embeddingService = embeddingService;
    }

    @Override
    public ToolDefinition getDefinition() {
        return ToolDefinition.builder()
                .name("search_dishes")
                .description(
                        "【关键工具】通过向量搜索查询菜品。" +
                                "当用户要求生成食谱、配餐、推荐菜品时，必须首先调用此工具获取 dishId。" +
                                "搜索策略：先搜索学校专属菜品库，若无结果或无schoolId则搜索公共菜品库。" +
                                "参数：schoolId(学校ID，可选), query(搜索关键词), topK(返回数量，默认20)。" +
                                "返回结果包含：id(dishId), name, type, ingredients, ingredientIds, nutrients, isCommon(是否公共菜品)。")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "schoolId", Map.of(
                                        "type", "integer",
                                        "description", "学校ID（可选）。如有则优先搜索学校专属菜品，否则搜索公共菜品库"),
                                "query", Map.of(
                                        "type", "string",
                                        "description", "搜索关键词，如'鸭'、'肉'、'蔬菜'、'紫苏焖鸭'"),
                                "topK", Map.of(
                                        "type", "integer",
                                        "description", "返回数量，默认20",
                                        "default", 20)),
                        "required", Arrays.asList("query")))
                .build();
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        Long schoolId = getLongArgument(arguments, "schoolId");
        String query = (String) arguments.get("query");
        Integer topK = getIntArgument(arguments, "topK", 20);

        if (query == null || query.isBlank()) {
            return ToolResult.error("query is required");
        }

        log.info("【向量搜索】schoolId={}, query={}, topK={}", schoolId, query, topK);

        MilvusClientV2 milvusClient = clientHolder.getClient();
        if (milvusClient == null) {
            log.warn("【向量搜索】Milvus 客户端不可用，请稍后重试");
            return ToolResult.error("Milvus 服务暂不可用，正在尝试重连，请稍后重试");
        }

        try {
            // 生成查询向量
            List<Float> queryVector = embeddingService.embed(query);
            if (queryVector == null || queryVector.isEmpty()) {
                log.error("生成查询向量失败");
                return ToolResult.error("生成查询向量失败，请检查 Embedding 服务配置");
            }

            List<Map<String, Object>> dishes = new ArrayList<>();
            boolean fromCommon = false;

            // 1. 如果有 schoolId，先搜索学校专属菜品
            if (schoolId != null) {
                String collectionName = COLLECTION_PREFIX + schoolId;
                log.info("【向量搜索】搜索学校专属菜品: {}", collectionName);
                dishes = searchCollection(milvusClient, collectionName, queryVector, topK, schoolId);
            }

            // 2. 如果学校专属菜品不足或没有 schoolId，搜索公共菜品库
            if (dishes.isEmpty()) {
                log.info("【向量搜索】搜索公共菜品库: {}", COMMON_DISHES_COLLECTION);
                dishes = searchCollection(milvusClient, COMMON_DISHES_COLLECTION, queryVector, topK, null);
                fromCommon = true;
            }

            // 构建结果
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("query", query);
            result.put("schoolId", schoolId);
            result.put("fromCommon", fromCommon);
            result.put("count", dishes.size());
            result.put("data", dishes);

            log.info("【向量搜索】搜索完成，找到 {} 条菜品（来源：{}）", 
                    dishes.size(), fromCommon ? "公共菜品库" : "学校专属菜品库");
            return ToolResult.text(formatResult(result));

        } catch (Exception e) {
            log.error("【向量搜索】搜索失败: schoolId={}, query={}", schoolId, query, e);
            return ToolResult.error("搜索失败: " + e.getMessage());
        }
    }

    /**
     * 搜索指定集合
     */
    private List<Map<String, Object>> searchCollection(MilvusClientV2 milvusClient, String collectionName, List<Float> queryVector,
                                                        int topK, Long filterSchoolId) {
        List<Map<String, Object>> dishes = new ArrayList<>();

        try {
            // 构建搜索请求
            SearchReq.SearchReqBuilder searchBuilder = SearchReq.builder()
                    .collectionName(collectionName)
                    .annsField("embedding")
                    .data(Collections.singletonList(new FloatVec(queryVector)))
                    .topK(topK)
                    .outputFields(Arrays.asList(
                            "id", "name", "type", "ingredients", "ingredient_ids",
                            "nutrients", "school_id", "update_time"));

            // 如果有 schoolId 过滤条件
            if (filterSchoolId != null) {
                searchBuilder.filter("school_id == " + filterSchoolId);
            }

            SearchResp response = milvusClient.search(searchBuilder.build());

            // 解析结果
            List<List<SearchResp.SearchResult>> searchResults = response.getSearchResults();
            if (searchResults != null && !searchResults.isEmpty()) {
                for (SearchResp.SearchResult hit : searchResults.get(0)) {
                    Map<String, Object> entity = hit.getEntity();
                    Map<String, Object> dish = new HashMap<>();

                    dish.put("id", getLongValue(entity, "id"));
                    dish.put("name", getStringValue(entity, "name"));
                    dish.put("type", getStringValue(entity, "type"));
                    dish.put("ingredients", getStringValue(entity, "ingredients"));
                    dish.put("ingredientIds", getStringValue(entity, "ingredient_ids"));
                    dish.put("nutrients", getStringValue(entity, "nutrients"));
                    dish.put("schoolId", getLongValue(entity, "school_id"));
                    dish.put("updateTime", getStringValue(entity, "update_time"));
                    dish.put("isCommon", filterSchoolId == null);

                    dishes.add(dish);
                }
            }

            log.debug("从集合 {} 搜索到 {} 条菜品", collectionName, dishes.size());

        } catch (Exception e) {
            log.warn("搜索集合 {} 失败: {}", collectionName, e.getMessage());
        }

        return dishes;
    }

    /**
     * 格式化搜索结果
     */
    private String formatResult(Map<String, Object> result) {
        StringBuilder sb = new StringBuilder();
        sb.append("向量搜索结果:\n");
        sb.append("- 查询词: ").append(result.get("query")).append("\n");
        sb.append("- 学校ID: ").append(result.get("schoolId") != null ? result.get("schoolId") : "无(使用公共菜品库)").append("\n");
        sb.append("- 数据来源: ").append(Boolean.TRUE.equals(result.get("fromCommon")) ? "公共菜品库" : "学校专属菜品库").append("\n");
        sb.append("- 找到 ").append(result.get("count")).append(" 条菜品\n\n");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dishes = (List<Map<String, Object>>) result.get("data");

        for (int i = 0; i < dishes.size(); i++) {
            Map<String, Object> dish = dishes.get(i);
            sb.append("菜品[").append(i + 1).append("]:\n");
            sb.append("  - id (dishId): ").append(dish.get("id")).append("\n");
            sb.append("  - name: ").append(dish.get("name")).append("\n");
            sb.append("  - type: ").append(dish.get("type")).append("\n");
            sb.append("  - ingredients: ").append(dish.get("ingredients")).append("\n");
            sb.append("  - ingredientIds: ").append(dish.get("ingredientIds")).append("\n");
            sb.append("  - nutrients: ").append(dish.get("nutrients")).append("\n");
            sb.append("  - isCommon: ").append(dish.get("isCommon")).append("\n");
            sb.append("\n");
        }

        sb.append("【重要】ingredientIds 格式: 'id1:name1,id2:name2'");
        sb.append("\n例如: '123456:鸭(代表值),123457:紫背天葵[红风菜、血皮菜]'");
        sb.append("\n使用时需要解析为: [{id: 123456, name: '鸭(代表值)'}, {id: 123457, name: '紫背天葵[红风菜、血皮菜]'}]");

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
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 获取 Integer 类型参数
     */
    private Integer getIntArgument(Map<String, Object> arguments, String key, int defaultValue) {
        Object value = arguments.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
