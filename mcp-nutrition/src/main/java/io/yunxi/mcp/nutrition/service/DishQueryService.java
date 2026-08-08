package io.yunxi.mcp.nutrition.service;

import lombok.Data;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 菜品查询服务 —— 结构化 SQL 查询（非向量检索）。
 *
 * <h3>架构说明：为什么这里不用 Milvus？</h3>
 * <p>mcp-nutrition 的搜索体系采用 <b>双通道混合架构</b>：</p>
 * <ul>
 *   <li><b>语义搜索通道（Milvus）</b>：
 *       由 {@code SyncEngine} 将 MySQL 中的分类数据、营养标准等静态元数据
 *       ETL 到 Milvus 向量库，AI Agent 通过 {@code DishSearchTool} 的
 *       "searchDishClasses" 等语义接口进行向量相似度检索。
 *       适合"这道菜属于哪一类""类似的营养标准有哪些"等模糊匹配场景。</li>
 *   <li><b>结构化搜索通道（MySQL/JdbcTemplate）</b>：
 *       本类 {@code DishQueryService} 直接对 {@code dishes} 表做 SQL 精确查询
 *       （关键词 LIKE / 分类精确匹配 / 营养成分范围过滤）。
 *       适合"列出所有荤菜""搜索菜名包含红烧肉的菜品"等确定性查询场景，
 *       这些查询不需要语义理解，SQL 更高效且结果可预期。</li>
 * </ul>
 *
 * <p>选择 MySQL 而非 Milvus 的原因：</p>
 * <ol>
 *   <li>菜品关键词搜索和分类过滤是精确匹配，不需要向量相似度。</li>
 *   <li>菜品数据量通常在万级别，MySQL LIKE 查询性能完全足够。</li>
 *   <li>省去 MySQL→Milvus 的菜品维度同步维护成本，
 *       避免数据不一致问题。</li>
 * </ol>
 *
 * <p>如需对菜品做语义搜索（如"适合老年人的低脂菜品"），
 * 应在 sync-pipelines.yml 中新增 dishes-sync 管道将菜品 ETL 到 Milvus，
 * 并在此服务中添加 {@code vectorSearchDishes()} 方法。</p>
 *
 * @see io.yunxi.mcp.nutrition.tool.DishSearchTool
 * @see io.yunxi.mcp.nutrition.sync.SyncEngine
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
                       n.protein, n.fat, n.carb, n.calories, n.fiber,
                       COALESCE(n.calcium,0) AS calcium, COALESCE(n.iron,0) AS iron,
                       COALESCE(n.zinc,0) AS zinc, COALESCE(n.vit_a,0) AS vit_a,
                       COALESCE(n.vit_b1,0) AS vit_b1, COALESCE(n.vit_b2,0) AS vit_b2,
                       COALESCE(n.vit_c,0) AS vit_c
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
            dto.setCalcium(rs.getDouble("calcium"));
            dto.setIron(rs.getDouble("iron"));
            dto.setZinc(rs.getDouble("zinc"));
            dto.setVitA(rs.getDouble("vit_a"));
            dto.setVitB1(rs.getDouble("vit_b1"));
            dto.setVitB2(rs.getDouble("vit_b2"));
            dto.setVitC(rs.getDouble("vit_c"));
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
                       n.protein, n.fat, n.carb, n.calories, n.fiber,
                       COALESCE(n.calcium,0) AS calcium, COALESCE(n.iron,0) AS iron,
                       COALESCE(n.zinc,0) AS zinc, COALESCE(n.vit_a,0) AS vit_a,
                       COALESCE(n.vit_b1,0) AS vit_b1, COALESCE(n.vit_b2,0) AS vit_b2,
                       COALESCE(n.vit_c,0) AS vit_c
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
            dto.setCalcium(rs.getDouble("calcium"));
            dto.setIron(rs.getDouble("iron"));
            dto.setZinc(rs.getDouble("zinc"));
            dto.setVitA(rs.getDouble("vit_a"));
            dto.setVitB1(rs.getDouble("vit_b1"));
            dto.setVitB2(rs.getDouble("vit_b2"));
            dto.setVitC(rs.getDouble("vit_c"));
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
        // 微量元素 / 维生素（每日摄入量，单位见注释；列不存在时默认 0）
        private double calcium;    // 钙(mg)
        private double iron;       // 铁(mg)
        private double zinc;       // 锌(mg)
        private double vitA;       // 维生素A(μg RE)
        private double vitB1;      // 维生素B1(mg)
        private double vitB2;      // 维生素B2(mg)
        private double vitC;       // 维生素C(mg)
    }
}