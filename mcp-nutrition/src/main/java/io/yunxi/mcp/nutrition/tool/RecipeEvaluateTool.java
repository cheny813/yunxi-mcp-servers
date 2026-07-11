package io.yunxi.mcp.nutrition.tool;

import io.yunxi.mcp.common.handler.ToolHandler;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class RecipeEvaluateTool implements ToolHandler {

    private static final Map<String, Double> MEAL_RATIOS = Map.of(
            "早餐", 0.30, "早点", 0.06,
            "午餐", 0.38, "午点", 0.06,
            "晚餐", 0.30, "晚点", 0.06);

    private static final Map<String, Double> TOLERANCE = Map.of(
            "优质城市", 1.10, "普通城市", 1.20, "农村", 1.35);

    private static final Map<String, double[]> STANDARDS = Map.of(
            "学龄前", new double[]{1000, 1200, 30, 35, 33, 40, 150, 180},
            "小学低年级", new double[]{1200, 1500, 35, 45, 40, 50, 180, 220},
            "小学高年级", new double[]{1500, 1800, 45, 55, 50, 60, 220, 270},
            "初中生", new double[]{2000, 2400, 55, 70, 60, 75, 300, 360},
            "高中生", new double[]{2200, 2600, 65, 80, 70, 85, 330, 400},
            "教师", new double[]{1800, 2200, 50, 65, 50, 65, 250, 320});

    @Override
    public String getName() { return "evaluateRecipe"; }

    @Override
    public String getDescription() {
        return "评估食谱营养评分，基于《中国居民膳食指南》标准，从热量、蛋白质、脂肪、碳水、多样性五个维度加权评分";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "crowdType", Map.of("type", "string", "description", "人群类型：学龄前/小学低年级/小学高年级/初中生/高中生/教师"),
                        "calorie", Map.of("type", "number", "description", "食谱总热量(kcal)"),
                        "protein", Map.of("type", "number", "description", "总蛋白质(g)"),
                        "fat", Map.of("type", "number", "description", "总脂肪(g)"),
                        "carbs", Map.of("type", "number", "description", "总碳水化合物(g)"),
                        "dishCount", Map.of("type", "number", "description", "菜品数量"),
                        "mealTypes", Map.of("type", "array", "description", "餐次列表"),
                        "schoolType", Map.of("type", "string", "description", "学校类型：优质城市/普通城市/农村"),
                        "dayCount", Map.of("type", "number", "description", "天数，默认1")),
                "required", List.of("crowdType", "calorie", "protein", "fat", "carbs", "dishCount", "mealTypes"));
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        try {
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

            double[] std = findStandard(crowdType);
            if (std == null) {
                throw new RuntimeException("未知的人群类型: " + crowdType);
            }

            double tolerance = TOLERANCE.getOrDefault(schoolType, 1.20);
            double mealRatio = Math.min(1.0, mealTypes.stream()
                    .mapToDouble(m -> MEAL_RATIOS.getOrDefault(m, 0.15)).sum());

            double calorieScore = calcScore(calorie, std[0] * dayCount * mealRatio, std[1] * dayCount * mealRatio, tolerance);
            double proteinScore = calcScore(protein, std[2] * dayCount * mealRatio, std[3] * dayCount * mealRatio, tolerance);
            double fatScore = calcScore(fat, std[4] * dayCount * mealRatio * 0.8, std[5] * dayCount * mealRatio * 1.2, tolerance);
            double carbsScore = calcScore(carbs, std[6] * dayCount * mealRatio, std[7] * dayCount * mealRatio, tolerance);
            double varietyScore = calcVarietyScore(dishCount, Math.max(2, mealTypes.size() * dayCount), tolerance);

            double total = calorieScore * 0.30 + proteinScore * 0.25 + fatScore * 0.15
                    + carbsScore * 0.15 + varietyScore * 0.15;
            String grade = total >= 90 ? "A" : total >= 80 ? "B" : total >= 70 ? "C" : total >= 60 ? "D" : "F";

            return String.format("""
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

        } catch (Exception e) {
            throw new RuntimeException("评分计算失败: " + e.getMessage());
        }
    }

    private double calcScore(double value, double min, double max, double tolerance) {
        double adjMin = min / tolerance;
        double adjMax = max * tolerance;
        if (value >= adjMin && value <= adjMax) return 100;
        if (value < adjMin) return Math.max(0, 100 - (adjMin - value) / adjMin * 100);
        return Math.max(0, 100 - (value - adjMax) / adjMax * 100);
    }

    private double calcVarietyScore(int dishCount, double minDishCount, double tolerance) {
        double adjustedMin = minDishCount / tolerance;
        if (dishCount >= adjustedMin * 2) return 100;
        if (dishCount >= adjustedMin) return 80 + (dishCount - adjustedMin) / adjustedMin * 20;
        return Math.max(0, dishCount / adjustedMin * 80);
    }

    private double[] findStandard(String crowdType) {
        if (crowdType == null) return null;
        String t = crowdType.toLowerCase();
        if (t.contains("学龄前") || t.contains("3-6")) return STANDARDS.get("学龄前");
        if (t.contains("小学低") || t.contains("1-3")) return STANDARDS.get("小学低年级");
        if (t.contains("小学高") || t.contains("4-6")) return STANDARDS.get("小学高年级");
        if (t.contains("初中") || t.contains("13-15")) return STANDARDS.get("初中生");
        if (t.contains("高中") || t.contains("16-18")) return STANDARDS.get("高中生");
        if (t.contains("教师")) return STANDARDS.get("教师");
        return STANDARDS.get("教师");
    }
}
