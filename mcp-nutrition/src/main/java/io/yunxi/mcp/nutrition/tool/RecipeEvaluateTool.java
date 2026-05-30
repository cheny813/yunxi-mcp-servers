package io.yunxi.mcp.nutrition.tool;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 食谱营养评估 MCP 工具
 *
 * <p>对食谱进行营养评分，评估热量、蛋白质、脂肪、碳水和食材多样性五个维度。<br>
 * 评分算法基于《中国居民膳食指南》学校营养餐标准。</p>
 *
 * <p>评分维度权重：</p>
 * <ul>
 *   <li>热量 30% — 能量供给是否达标</li>
 *   <li>蛋白质 25% — 宏量营养素</li>
 *   <li>脂肪 15% — 脂肪摄入是否合理</li>
 *   <li>碳水 15% — 碳水化合物供给</li>
 *   <li>多样性 15% — 食材种类是否丰富</li>
 * </ul>
 *
 * @author yunxi-agent-platform
 */
@Component
public class RecipeEvaluateTool implements ToolHandler {

    /** 各餐次占全天比例 */
    private static final Map<String, Double> MEAL_RATIOS = Map.of(
            "早餐", 0.30, "早点", 0.06,
            "午餐", 0.38, "午点", 0.06,
            "晚餐", 0.30, "晚点", 0.06);

    /** 学校类型宽松度（越宽松评分标准越宽） */
    private static final Map<String, Double> TOLERANCE = Map.of(
            "优质城市", 1.10, "普通城市", 1.20, "农村", 1.35);

    /** 各人群每日营养标准 [热量Min, 热量Max, 蛋白质Min, 蛋白质Max, 脂肪Min, 脂肪Max, 碳水Min, 碳水Max] */
    private static final Map<String, double[]> STANDARDS = Map.of(
            "学龄前",    new double[]{1000, 1200, 30, 35, 33, 40, 150, 180},
            "小学低年级", new double[]{1200, 1500, 35, 45, 40, 50, 180, 220},
            "小学高年级", new double[]{1500, 1800, 45, 55, 50, 60, 220, 270},
            "初中生",    new double[]{2000, 2400, 55, 70, 60, 75, 300, 360},
            "高中生",    new double[]{2200, 2600, 65, 80, 70, 85, 330, 400},
            "教师",      new double[]{1800, 2200, 50, 65, 50, 65, 250, 320});

    @Override
    public ToolDefinition getDefinition() {
        return ToolDefinition.builder()
                .name("evaluateRecipe")
                .description("评估食谱营养评分，基于《中国居民膳食指南》标准，从热量、蛋白质、脂肪、碳水、多样性五个维度加权评分")
                .parameters(List.of(
                        ToolParameter.builder()
                                .name("crowdType")
                                .type("string")
                                .description("人群类型：学龄前 / 小学低年级 / 小学高年级 / 初中生 / 高中生 / 教师")
                                .required(true)
                                .build(),
                        ToolParameter.builder()
                                .name("calorie")
                                .type("number")
                                .description("食谱总热量(kcal)")
                                .required(true)
                                .build(),
                        ToolParameter.builder()
                                .name("protein")
                                .type("number")
                                .description("总蛋白质(g)")
                                .required(true)
                                .build(),
                        ToolParameter.builder()
                                .name("fat")
                                .type("number")
                                .description("总脂肪(g)")
                                .required(true)
                                .build(),
                        ToolParameter.builder()
                                .name("carbs")
                                .type("number")
                                .description("总碳水化合物(g)")
                                .required(true)
                                .build(),
                        ToolParameter.builder()
                                .name("dishCount")
                                .type("number")
                                .description("菜品数量")
                                .required(true)
                                .build(),
                        ToolParameter.builder()
                                .name("mealTypes")
                                .type("array")
                                .description("餐次列表，如 [\"早餐\",\"午餐\",\"晚餐\"]")
                                .required(true)
                                .build(),
                        ToolParameter.builder()
                                .name("schoolType")
                                .type("string")
                                .description("学校类型：优质城市 / 普通城市 / 农村，默认普通城市")
                                .required(false)
                                .build(),
                        ToolParameter.builder()
                                .name("dayCount")
                                .type("number")
                                .description("天数，默认 1")
                                .required(false)
                                .build()))
                .returnType("object")
                .metadata(ToolMetadata.builder()
                        .category("nutrition")
                        .tags(List.of("recipe", "evaluate", "scoring"))
                        .build())
                .build();
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        try {
            // 解析参数
            String crowdType = (String) arguments.get("crowdType");
            double calorie = ((Number) arguments.get("calorie")).doubleValue();
            double protein = ((Number) arguments.get("protein")).doubleValue();
            double fat = ((Number) arguments.get("fat")).doubleValue();
            double carbs = ((Number) arguments.get("carbs")).doubleValue();
            int dishCount = ((Number) arguments.get("dishCount")).intValue();
            @SuppressWarnings("unchecked")
            List<String> mealTypes = (List<String>) arguments.get("mealTypes");
            String schoolType = (String) arguments.getOrDefault("schoolType", "普通城市");
            int dayCount = arguments.containsKey("dayCount") ? ((Number) arguments.get("dayCount")).intValue() : 1;

            // 查找人群标准
            double[] std = findStandard(crowdType);
            if (std == null) {
                return ToolResult.error("未知的人群类型: " + crowdType + "，支持：学龄前/小学低年级/小学高年级/初中生/高中生/教师");
            }

            // 计算宽松度
            double tolerance = TOLERANCE.getOrDefault(schoolType, 1.20);
            double mealRatio = Math.min(1.0, mealTypes.stream()
                    .mapToDouble(m -> MEAL_RATIOS.getOrDefault(m, 0.15)).sum());

            // 计算各维度得分（满分100）
            double calorieScore = calcScore(calorie, std[0] * dayCount * mealRatio, std[1] * dayCount * mealRatio, tolerance);
            double proteinScore = calcScore(protein, std[2] * dayCount * mealRatio, std[3] * dayCount * mealRatio, tolerance);
            double fatScore = calcScore(fat, std[4] * dayCount * mealRatio * 0.8, std[5] * dayCount * mealRatio * 1.2, tolerance);
            double carbsScore = calcScore(carbs, std[6] * dayCount * mealRatio, std[7] * dayCount * mealRatio, tolerance);
            double varietyScore = calcVarietyScore(dishCount, Math.max(2, mealTypes.size() * dayCount), tolerance);

            // 加权总分
            double total = calorieScore * 0.30 + proteinScore * 0.25 + fatScore * 0.15
                    + carbsScore * 0.15 + varietyScore * 0.15;

            // 等级
            String grade = total >= 90 ? "A" : total >= 80 ? "B" : total >= 70 ? "C" : total >= 60 ? "D" : "F";

            String result = String.format("""
                    食谱营养评分结果：
                    总分：%.1f（等级 %s）
                    
                    维度得分：
                    - 热量：%.1f/100（权重30%%）
                    - 蛋白质：%.1f/100（权重25%%）
                    - 脂肪：%.1f/100（权重15%%）
                    - 碳水化合物：%.1f/100（权重15%%）
                    - 食材多样性：%.1f/100（权重15%%）
                    
                    宽松度：%.2f（学校类型：%s）
                    餐次占比：%.0f%%
                    """, total, grade,
                    calorieScore, proteinScore, fatScore, carbsScore, varietyScore,
                    tolerance, schoolType, mealRatio * 100);

            return ToolResult.text(result);

        } catch (Exception e) {
            return ToolResult.error("评分计算失败: " + e.getMessage());
        }
    }

    /** 计算单维度得分 */
    private double calcScore(double value, double min, double max, double tolerance) {
        double adjMin = min / tolerance;
        double adjMax = max * tolerance;
        if (value >= adjMin && value <= adjMax) return 100;
        if (value < adjMin) return Math.max(0, 100 - (adjMin - value) / adjMin * 100);
        return Math.max(0, 100 - (value - adjMax) / adjMax * 100);
    }

    /** 计算多样性得分 */
    private double calcVarietyScore(int dishCount, double minDishCount, double tolerance) {
        double adjustedMin = minDishCount / tolerance;
        if (dishCount >= adjustedMin * 2) return 100;
        if (dishCount >= adjustedMin) return 80 + (dishCount - adjustedMin) / adjustedMin * 20;
        return Math.max(0, dishCount / adjustedMin * 80);
    }

    /** 根据人群类型查找对应的营养标准 */
    private double[] findStandard(String crowdType) {
        if (crowdType == null) return null;
        String t = crowdType.toLowerCase();
        if (t.contains("学龄前") || t.contains("3-6")) return STANDARDS.get("学龄前");
        if (t.contains("小学低") || t.contains("1-3")) return STANDARDS.get("小学低年级");
        if (t.contains("小学高") || t.contains("4-6")) return STANDARDS.get("小学高年级");
        if (t.contains("初中") || t.contains("13-15")) return STANDARDS.get("初中生");
        if (t.contains("高中") || t.contains("16-18")) return STANDARDS.get("高中生");
        if (t.contains("教师")) return STANDARDS.get("教师");
        return STANDARDS.get("教师"); // 默认
    }
}