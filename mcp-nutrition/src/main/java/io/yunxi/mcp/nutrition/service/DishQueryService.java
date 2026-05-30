package io.yunxi.mcp.nutrition.service;

import lombok.Data;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 菜品查询服务
 *
 * <p>从营养数据库（school_nutrition）查询菜品基础信息和营养成分，
 * 供 MCP 工具 {@link io.yunxi.mcp.nutrition.tool.DishSearchTool} 调用。</p>
 *
 * @author yunxi-agent-platform
 */
@Service
public class DishQueryService {

    private final JdbcTemplate jdbc;

    public DishQueryService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 按关键词搜索菜品（名称模糊匹配）
     *
     * @param keyword 搜索关键词，如"红烧肉"、"番茄"
     * @param limit   返回数量上限
     * @return 匹配的菜品列表，含名称、分类、营养成分
     */
    public List<DishDto> searchDishes(String keyword, int limit) {
        String sql = """
                SELECT d.id, d.name, d.category, d.description, d.cuisine_type,
                       n.protein, n.fat, n.carb, n.calories, n.fiber
                FROM dishes d
                LEFT JOIN dish_nutrients n ON d.id = n.dish_id
                WHERE d.name LIKE ? AND d.deleted = 0
                LIMIT ?
                """;
        return jdbc.query(sql, (rs, row) -> {
            DishDto dto = new DishDto();
            dto.setId(rs.getLong("id"));
            dto.setName(rs.getString("name"));
            dto.setCategory(mapCategory(rs.getString("category")));
            dto.setDescription(rs.getString("description"));
            dto.setCuisineType(rs.getString("cuisine_type"));
            dto.setProtein(rs.getDouble("protein"));
            dto.setFat(rs.getDouble("fat"));
            dto.setCarb(rs.getDouble("carb"));
            dto.setCalories(rs.getDouble("calories"));
            dto.setFiber(rs.getDouble("fiber"));
            return dto;
        }, "%" + keyword + "%", limit);
    }

    /**
     * 按分类查询菜品
     *
     * @param category 分类编码：1=荤菜 2=素菜 3=汤品 4=主食
     * @param limit    返回数量上限
     * @return 分类下的菜品列表
     */
    public List<DishDto> getDishesByCategory(String category, int limit) {
        String sql = """
                SELECT d.id, d.name, d.category, d.description, d.cuisine_type,
                       n.protein, n.fat, n.carb, n.calories, n.fiber
                FROM dishes d
                LEFT JOIN dish_nutrients n ON d.id = n.dish_id
                WHERE d.category = ? AND d.deleted = 0
                LIMIT ?
                """;
        return jdbc.query(sql, (rs, row) -> {
            DishDto dto = new DishDto();
            dto.setId(rs.getLong("id"));
            dto.setName(rs.getString("name"));
            dto.setCategory(mapCategory(rs.getString("category")));
            dto.setDescription(rs.getString("description"));
            dto.setCuisineType(rs.getString("cuisine_type"));
            dto.setProtein(rs.getDouble("protein"));
            dto.setFat(rs.getDouble("fat"));
            dto.setCarb(rs.getDouble("carb"));
            dto.setCalories(rs.getDouble("calories"));
            dto.setFiber(rs.getDouble("fiber"));
            return dto;
        }, category, limit);
    }

    /**
     * 将分类编码映射为中文名称
     */
    private String mapCategory(String code) {
        if (code == null) return "未知";
        return switch (code) {
            case "1" -> "荤菜";
            case "2" -> "素菜";
            case "3" -> "汤品";
            case "4" -> "主食";
            case "5" -> "点心";
            case "6" -> "饮品";
            default -> code;
        };
    }

    /** 菜品数据传输对象 */
    @Data
    public static class DishDto {
        private Long id;           // 菜品ID
        private String name;       // 菜品名称
        private String category;   // 菜品分类（中文）
        private String description;// 菜品描述
        private String cuisineType;// 菜系类型
        private double protein;    // 蛋白质(g)
        private double fat;        // 脂肪(g)
        private double carb;       // 碳水化合物(g)
        private double calories;   // 热量(kcal)
        private double fiber;      // 膳食纤维(g)
    }
}