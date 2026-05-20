package io.yunxi.mcp.database.tool;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/**
 * 批量查询菜品食材工具
 * <p>
 * 一次性查询多个菜品的食材详情，避免逐个查询。
 * 将 dishId 列表拼接为 IN 子句，一次 SQL 返回所有结果。
 * </p>
 */
public class BatchQueryDishIngredientsTool implements ToolHandler {

    private final DataSource dataSource;

    public BatchQueryDishIngredientsTool(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public ToolDefinition getDefinition() {
        return ToolDefinition.builder()
                .name("batch_query_dish_ingredients")
                .description(
                        "批量查询多个菜品的食材详情（含食材ID、名称、克重）。" +
                                "替代逐个调用 query 工具查询 dish_food_ingredient 表，一次查询返回所有结果。" +
                                "参数：dishIds（菜品ID列表，逗号分隔或JSON数组）。" +
                                "返回按 dishId 分组的食材列表。")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "dishIds", Map.of(
                                        "type", "string",
                                        "description",
                                        "菜品ID列表，逗号分隔。示例: '1899417015935889409,1899417015935889410,1899417015935889411'")),
                        "required", List.of("dishIds")))
                .build();
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        String dishIdsStr = (String) arguments.get("dishIds");

        if (dishIdsStr == null || dishIdsStr.isBlank()) {
            return ToolResult.error("dishIds is required");
        }

        // 解析 dishId 列表（支持逗号分隔）
        List<Long> dishIdList = parseDishIds(dishIdsStr);
        if (dishIdList.isEmpty()) {
            return ToolResult.error("No valid dishIds provided");
        }
        if (dishIdList.size() > 50) {
            return ToolResult.error("Too many dishIds (max 50 per call)");
        }

        // 构建 IN 子句占位符
        String placeholders = String.join(",", Collections.nCopies(dishIdList.size(), "?"));

        // 构建 SQL
        // 注意：dish_food_ingredient 表中没有 sort_order 字段，使用 id 作为排序依据
        String sql = "SELECT dfi.d_id AS dishId, dfi.fi_id AS id, fi.name, dfi.dosage " +
                "FROM dish_food_ingredient dfi " +
                "LEFT JOIN food_ingredient fi ON dfi.fi_id = fi.id " +
                "WHERE dfi.d_id IN (" + placeholders + ") AND fi.deleted = 0 " +
                "ORDER BY dfi.d_id, dfi.id, fi.name";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // 设置参数
            for (int i = 0; i < dishIdList.size(); i++) {
                stmt.setLong(i + 1, dishIdList.get(i));
            }

            ResultSet rs = stmt.executeQuery();

            // 按 dishId 分组
            Map<Long, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
            for (Long dishId : dishIdList) {
                grouped.put(dishId, new ArrayList<>());
            }

            while (rs.next()) {
                long dishId = rs.getLong("dishId");
                Map<String, Object> ingredient = new LinkedHashMap<>();
                ingredient.put("dishId", dishId);
                ingredient.put("id", rs.getLong("id"));
                ingredient.put("name", rs.getString("name"));
                ingredient.put("dosage", rs.getInt("dosage"));

                grouped.computeIfAbsent(dishId, k -> new ArrayList<>()).add(ingredient);
            }

            // 构建结果
            StringBuilder sb = new StringBuilder();
            sb.append("Batch Dish Ingredients Result:\n");
            sb.append("- Queried ").append(dishIdList.size()).append(" dishes\n\n");

            for (Map.Entry<Long, List<Map<String, Object>>> entry : grouped.entrySet()) {
                sb.append("Dish ").append(entry.getKey()).append(" (").append(entry.getValue().size()).append(" ingredients):\n");
                for (Map<String, Object> ing : entry.getValue()) {
                    sb.append("  - id: ").append(ing.get("id"))
                            .append(", name: ").append(ing.get("name"))
                            .append(", dosage: ").append(ing.get("dosage")).append("g\n");
                }
                sb.append("\n");
            }

            return ToolResult.text(sb.toString());

        } catch (SQLException e) {
            return ToolResult.error("SQL Error: " + e.getMessage());
        }
    }

    private List<Long> parseDishIds(String input) {
        List<Long> result = new ArrayList<>();
        // 支持逗号、空格、分号分隔
        String[] parts = input.split("[,;\\s]+");
        for (String part : parts) {
            try {
                result.add(Long.parseLong(part.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }
}
