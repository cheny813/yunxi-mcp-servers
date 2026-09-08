package io.yunxi.mcp.nutrition.service;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 菜品查询服务 —— 结构化 SQL 查询（非向量检索）。
 *
 * <h3>数据来源说明</h3>
 * <p>菜品主表为 {@code dish_library}（字段：id / name / type / school_id / deleted / update_time），
 * 营养库连接统一通过 {@code SyncDatasourceRegistry} 获取。菜品本身不存储营养素列，
 * 每道菜的营养素需通过 {@code dish_food_ingredient → food_ingredient → food_ingredient_nutrient → nutrient}
 * 关联聚合得到（食材用量 dosage 单位克，food_ingredient_nutrient.nutrient_content 按每 100g 计，
 * 故单菜某营养素 = Σ(nutrient_content × dosage / 100)）。</p>
 *
 * <p><b>健壮性</b>：营养素聚合为 best-effort，任何关联查询异常（表/列缺失、数据缺失）均被捕获并降级为
 * 0，绝不影响菜品主列表的返回，避免调用方因单点 SQL 错误陷入重试循环。</p>
 *
 * @see io.yunxi.mcp.nutrition.tool.DishSearchTool
 */
@Service
public class DishQueryService {

    private static final Logger log = LoggerFactory.getLogger(DishQueryService.class);

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
                SELECT id, name, type
                FROM dish_library
                WHERE name LIKE ? AND deleted = 0
                ORDER BY id
                LIMIT ?
                """;
        return jdbc.query(sql, (rs, row) -> {
            DishDto dto = new DishDto();
            dto.setId(rs.getLong("id"));
            dto.setName(rs.getString("name"));
            dto.setCategory(mapCategory(rs.getString("type")));
            enrichNutrients(dto);
            return dto;
        }, "%" + keyword + "%", limit);
    }

    /**
     * 按分类查询菜品
     *
     * @param category 分类编码：1=荤菜 2=素菜 3=汤品 4=主食 5=点心 6=饮品
     * @param limit    返回数量上限
     * @return 分类下的菜品列表
     */
    public List<DishDto> getDishesByCategory(String category, int limit) {
        String sql = """
                SELECT id, name, type
                FROM dish_library
                WHERE type = ? AND deleted = 0
                ORDER BY id
                LIMIT ?
                """;
        return jdbc.query(sql, (rs, row) -> {
            DishDto dto = new DishDto();
            dto.setId(rs.getLong("id"));
            dto.setName(rs.getString("name"));
            dto.setCategory(mapCategory(rs.getString("type")));
            enrichNutrients(dto);
            return dto;
        }, category, limit);
    }

    /**
     * best-effort 聚合菜品营养素。任何异常（关联表/列缺失、无食材映射）均降级为 0，不向上抛出，
     * 保证菜品检索主流程始终能返回结果。
     */
    private void enrichNutrients(DishDto dto) {
        try {
            String sql = """
                    SELECT n.name AS nutrient_name,
                           SUM(fin.nutrient_content * dfi.dosage / 100.0) AS total
                    FROM dish_food_ingredient dfi
                    JOIN food_ingredient fi ON dfi.fi_id = fi.id
                    JOIN food_ingredient_nutrient fin ON fi.id = fin.fi_id
                    JOIN nutrient n ON fin.nutrient_id = n.id
                    WHERE dfi.dish_id = ?
                    GROUP BY n.name
                    """;
            List<Map<String, Object>> rows = jdbc.queryForList(sql, dto.getId());
            for (Map<String, Object> r : rows) {
                Object nameObj = r.get("nutrient_name");
                Object totalObj = r.get("total");
                if (nameObj == null || totalObj == null) {
                    continue;
                }
                String nn = String.valueOf(nameObj);
                double tot = ((Number) totalObj).doubleValue();
                mapNutrient(dto, nn, tot);
            }
        } catch (Exception e) {
            log.warn("菜品 {} 营养素聚合失败，降级返回基础信息: {}", dto.getId(), e.getMessage());
        }
    }

    /**
     * 将营养素中文名映射到 DishDto 字段（按子串模糊匹配，未知营养素忽略）。
     */
    private void mapNutrient(DishDto dto, String name, double value) {
        if (name.contains("蛋白质")) {
            dto.setProtein(value);
        } else if (name.contains("脂肪")) {
            dto.setFat(value);
        } else if (name.contains("碳水")) {
            dto.setCarb(value);
        } else if (name.contains("能量") || name.contains("热量")) {
            dto.setCalories(value);
        } else if (name.contains("膳食纤维")) {
            dto.setFiber(value);
        } else if (name.contains("钙")) {
            dto.setCalcium(value);
        } else if (name.contains("铁")) {
            dto.setIron(value);
        } else if (name.contains("锌")) {
            dto.setZinc(value);
        } else if (name.contains("维生素A") || name.contains("VA")) {
            dto.setVitA(value);
        } else if (name.contains("维生素B1")) {
            dto.setVitB1(value);
        } else if (name.contains("维生素B2")) {
            dto.setVitB2(value);
        } else if (name.contains("维生素C")) {
            dto.setVitC(value);
        }
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
