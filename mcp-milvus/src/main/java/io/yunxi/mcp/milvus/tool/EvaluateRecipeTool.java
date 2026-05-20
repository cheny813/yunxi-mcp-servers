package io.yunxi.mcp.milvus.tool;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import io.yunxi.mcp.milvus.config.MilvusClientHolder;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 食谱评分工具
 * <p>
 * 根据营养标准对食谱进行评分，支持按实际餐次占比动态计算推荐量。
 * 支持根据学校类型和餐费标准灵活调整评分宽松度。
 * </p>
 *
 * <p>评分原则：
 * <ul>
 * <li>只评估已启用且有菜品的餐次</li>
 * <li>推荐量按已配置餐次的餐次占比折算</li>
 * <li>每个维度按达标程度评分（0-100分）</li>
 * <li>学校类型和餐费影响评分宽松度</li>
 * </ul>
 * </p>
 */
@Slf4j
public class EvaluateRecipeTool implements ToolHandler {

    private final MilvusClientHolder clientHolder;

    // 各营养素权重
    private static final double CALORIE_WEIGHT = 0.30;
    private static final double PROTEIN_WEIGHT = 0.25;
    private static final double FAT_WEIGHT = 0.15;
    private static final double CARBS_WEIGHT = 0.15;
    private static final double VARIETY_WEIGHT = 0.15;

    // 标准餐次占比
    private static final Map<String, Double> MEAL_RATIO = Map.of(
            "早餐", 0.30,
            "早点", 0.06,
            "午餐", 0.38,
            "午点", 0.06,
            "晚餐", 0.30,
            "晚点", 0.06
    );

    // 学校类型与评分宽松度配置
    // 设计原则：让各类型学校都能获得较高分数，体现商业价值
    // 优质城市学校：放宽10%，目标95+分（体现优势，易获高分）
    // 普通城市学校：放宽20%，目标90+分（合理预期）
    // 农村学校：放宽35%，目标80+分（经济条件限制）
    private static final Map<String, Double> SCHOOL_TYPE_TOLERANCE = Map.of(
            "优质城市", 1.10,   // 放宽10%，易获高分
            "普通城市", 1.20,   // 放宽20%
            "农村", 1.35       // 放宽35%
    );

    // 学校类型对应的目标分数（各学校都能达到的合理目标）
    private static final Map<String, Integer> SCHOOL_TYPE_TARGET = Map.of(
            "优质城市", 95,     // 优质学校应能轻松达到95+
            "普通城市", 90,     // 普通学校目标90+
            "农村", 80         // 农村学校目标80+
    );

    // 每餐价格区间与评分宽松度（价格越低，越宽松）
    // 单位：元/人/餐
    // 设计思路：餐费低不是学校的问题，评分应更宽容
    private static final double[][] PRICE_TIERS = {
            {0, 5, 1.30},      // 5元以下，放宽30%
            {5, 8, 1.20},      // 5-8元，放宽20%
            {8, 12, 1.10},     // 8-12元，放宽10%
            {12, Double.MAX_VALUE, 1.0}  // 12元以上，标准评分
    };

    public EvaluateRecipeTool(MilvusClientHolder clientHolder) {
        this.clientHolder = clientHolder;
    }

    @Override
    public ToolDefinition getDefinition() {
        return ToolDefinition.builder()
                .name("evaluate_recipe")
                .description(
                        "【重要】对食谱进行营养评分。支持按学校类型、餐费标准灵活评分。" +
                                "\n\n【评分模式说明】" +
                                "\n- 优质城市学校：放宽10%，目标95+分（易获高分，体现优势）" +
                                "\n- 普通城市学校：放宽20%，目标90+分（合理预期）" +
                                "\n- 农村学校：放宽35%，目标80+分（经济条件限制）" +
                                "\n- 餐费越低，评分越宽松（考虑经济条件）" +
                                "\n\n【参数说明】" +
                                "\n- crowdType: 人群类型" +
                                "\n- calorie/protein/fat/carbs: 食谱总营养值" +
                                "\n- mealTypes: 已配置餐次列表" +
                                "\n- dayCount: 配置天数" +
                                "\n- schoolType: 学校类型（优质城市/普通城市/农村）" +
                                "\n- mealPrice: 每餐人均费用（元），可选")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "crowdType", Map.of(
                                        "type", "string",
                                        "description", "人群类型：学龄前、小学低年级、小学高年级、初中生、高中生、教师"),
                                "calorie", Map.of(
                                        "type", "number",
                                        "description", "食谱总热量(kcal)"),
                                "protein", Map.of(
                                        "type", "number",
                                        "description", "食谱总蛋白质(g)"),
                                "fat", Map.of(
                                        "type", "number",
                                        "description", "食谱总脂肪(g)"),
                                "carbs", Map.of(
                                        "type", "number",
                                        "description", "食谱总碳水化合物(g)"),
                                "dishCount", Map.of(
                                        "type", "integer",
                                        "description", "菜品数量（用于多样性评分）"),
                                "mealTypes", Map.of(
                                        "type", "array",
                                        "description", "已配置餐次列表，如[\"早餐\",\"午餐\"]。用于计算实际推荐量占比",
                                        "items", Map.of("type", "string")),
                                "dayCount", Map.of(
                                        "type", "integer",
                                        "description", "配置的天数。标准周5天，补课周7天，部分放假按实际天数。默认1天",
                                        "default", 1),
                                "schoolType", Map.of(
                                        "type", "string",
                                        "description", "学校类型：优质城市（放宽10%，目标95+）、普通城市（放宽20%，目标90+）、农村（放宽35%，目标80+）。默认普通城市",
                                        "enum", List.of("优质城市", "普通城市", "农村")),
                                "mealPrice", Map.of(
                                        "type", "number",
                                        "description", "每餐人均费用（元）。餐费越低，评分越宽松：<5元放宽20%、5-8元放宽10%、8-12元放宽5%、>12元标准评分")),
                        "required", List.of("crowdType", "calorie", "protein", "fat", "carbs")))
                .build();
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        String crowdType = (String) arguments.get("crowdType");
        double calorie = getNumberArgument(arguments, "calorie", 0);
        double protein = getNumberArgument(arguments, "protein", 0);
        double fat = getNumberArgument(arguments, "fat", 0);
        double carbs = getNumberArgument(arguments, "carbs", 0);
        int dishCount = getIntArgument(arguments, "dishCount", 3);

        // 获取已配置餐次
        @SuppressWarnings("unchecked")
        List<String> mealTypes = (List<String>) arguments.get("mealTypes");
        if (mealTypes == null || mealTypes.isEmpty()) {
            mealTypes = List.of("早餐", "午餐", "晚餐"); // 默认三餐
        }

        // 获取配置天数
        int dayCount = getIntArgument(arguments, "dayCount", 1);
        if (dayCount < 1) dayCount = 1;

        // 获取学校类型（默认普通城市）
        String schoolType = (String) arguments.get("schoolType");
        if (schoolType == null || schoolType.isBlank()) {
            schoolType = "普通城市";
        }

        // 获取每餐价格（可选）
        Double mealPrice = null;
        Object priceArg = arguments.get("mealPrice");
        if (priceArg != null) {
            if (priceArg instanceof Number) {
                mealPrice = ((Number) priceArg).doubleValue();
            } else {
                try {
                    mealPrice = Double.parseDouble(priceArg.toString());
                } catch (NumberFormatException ignored) {}
            }
        }

        if (crowdType == null || crowdType.isBlank()) {
            return ToolResult.error("crowdType is required");
        }

        // 计算综合宽松度
        double tolerance = calculateTolerance(schoolType, mealPrice);

        log.info("【食谱评分】crowdType={}, calorie={}, protein={}, fat={}, carbs={}, mealTypes={}, dayCount={}, schoolType={}, mealPrice={}, tolerance={}",
                crowdType, calorie, protein, fat, carbs, mealTypes, dayCount, schoolType, mealPrice, tolerance);

        try {
            // 计算已配置餐次的总占比
            double totalMealRatio = calculateTotalMealRatio(mealTypes);

            // 获取全天营养标准
            Map<String, Object> dailyStandard = getDailyStandard(crowdType);

            // 按天数和餐次占比折算推荐量
            // 公式：推荐量 = 全天推荐量 × 天数 × 餐次占比
            double recommendedCalorieMin = ((Number) dailyStandard.get("calorieMin")).doubleValue() * dayCount * totalMealRatio;
            double recommendedCalorieMax = ((Number) dailyStandard.get("calorieMax")).doubleValue() * dayCount * totalMealRatio;
            double recommendedProteinMin = ((Number) dailyStandard.get("proteinMin")).doubleValue() * dayCount * totalMealRatio;
            double recommendedProteinMax = ((Number) dailyStandard.get("proteinMax")).doubleValue() * dayCount * totalMealRatio;
            double recommendedFatMin = ((Number) dailyStandard.get("fatMin")).doubleValue() * dayCount * totalMealRatio;
            double recommendedFatMax = ((Number) dailyStandard.get("fatMax")).doubleValue() * dayCount * totalMealRatio;
            double recommendedCarbsMin = ((Number) dailyStandard.get("carbsMin")).doubleValue() * dayCount * totalMealRatio;
            double recommendedCarbsMax = ((Number) dailyStandard.get("carbsMax")).doubleValue() * dayCount * totalMealRatio;

            // 应用宽松度调整推荐范围（范围更宽，更容易达标）
            double adjustedCalorieMin = recommendedCalorieMin / tolerance;
            double adjustedCalorieMax = recommendedCalorieMax * tolerance;
            double adjustedProteinMin = recommendedProteinMin / tolerance;
            double adjustedProteinMax = recommendedProteinMax * tolerance;
            double adjustedFatMin = recommendedFatMin / tolerance;
            double adjustedFatMax = recommendedFatMax * tolerance;
            double adjustedCarbsMin = recommendedCarbsMin / tolerance;
            double adjustedCarbsMax = recommendedCarbsMax * tolerance;

            // 计算各项得分（使用调整后的范围）
            double calorieScore = calcScore(calorie, adjustedCalorieMin, adjustedCalorieMax);
            double proteinScore = calcScore(protein, adjustedProteinMin, adjustedProteinMax);
            double fatScore = calcFatScore(fat, adjustedFatMin, adjustedFatMax);
            double carbsScore = calcScore(carbs, adjustedCarbsMin, adjustedCarbsMax);

            // 多样性评分（根据天数和餐次数量调整，也受宽松度影响）
            int minDishCount = Math.max(2, mealTypes.size() * dayCount);
            double varietyScore = calcVarietyScore(dishCount, minDishCount, tolerance);

            // 总分
            double totalScore = calorieScore * CALORIE_WEIGHT +
                    proteinScore * PROTEIN_WEIGHT +
                    fatScore * FAT_WEIGHT +
                    carbsScore * CARBS_WEIGHT +
                    varietyScore * VARIETY_WEIGHT;

            totalScore = Math.round(totalScore * 10) / 10.0;

            // 获取目标分数
            int targetScore = SCHOOL_TYPE_TARGET.getOrDefault(schoolType, 80);

            // 生成建议
            List<String> suggestions = generateSuggestions(
                    calorie, protein, fat, carbs,
                    calorieScore, proteinScore, fatScore, carbsScore, varietyScore,
                    recommendedCalorieMin, recommendedCalorieMax,
                    mealTypes, dayCount, schoolType, mealPrice, tolerance, targetScore);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("totalScore", totalScore);
            result.put("grade", getGrade(totalScore));
            result.put("targetScore", targetScore);
            result.put("configuredMeals", mealTypes);
            result.put("dayCount", dayCount);
            result.put("schoolType", schoolType);
            result.put("mealPrice", mealPrice);
            result.put("tolerance", tolerance);
            result.put("mealRatioPercent", Math.round(totalMealRatio * 100) + "%");
            result.put("totalRatioPercent", Math.round(totalMealRatio * dayCount * 100) + "%（" + dayCount + "天）");
            result.put("recommendedRange", Map.of(
                    "calorie", Map.of("min", Math.round(recommendedCalorieMin), "max", Math.round(recommendedCalorieMax)),
                    "protein", Map.of("min", Math.round(recommendedProteinMin * 10) / 10.0, "max", Math.round(recommendedProteinMax * 10) / 10.0),
                    "fat", Map.of("min", Math.round(recommendedFatMin * 10) / 10.0, "max", Math.round(recommendedFatMax * 10) / 10.0),
                    "carbs", Map.of("min", Math.round(recommendedCarbsMin), "max", Math.round(recommendedCarbsMax))
            ));
            result.put("adjustedRange", Map.of(
                    "calorie", Map.of("min", Math.round(adjustedCalorieMin), "max", Math.round(adjustedCalorieMax)),
                    "protein", Map.of("min", Math.round(adjustedProteinMin * 10) / 10.0, "max", Math.round(adjustedProteinMax * 10) / 10.0),
                    "fat", Map.of("min", Math.round(adjustedFatMin * 10) / 10.0, "max", Math.round(adjustedFatMax * 10) / 10.0),
                    "carbs", Map.of("min", Math.round(adjustedCarbsMin), "max", Math.round(adjustedCarbsMax))
            ));
            result.put("breakdown", Map.of(
                    "calorie", Map.of("score", calorieScore, "value", calorie, "weight", CALORIE_WEIGHT),
                    "protein", Map.of("score", proteinScore, "value", protein, "weight", PROTEIN_WEIGHT),
                    "fat", Map.of("score", fatScore, "value", fat, "weight", FAT_WEIGHT),
                    "carbs", Map.of("score", carbsScore, "value", carbs, "weight", CARBS_WEIGHT),
                    "variety", Map.of("score", varietyScore, "value", dishCount, "weight", VARIETY_WEIGHT)
            ));
            result.put("suggestions", suggestions);

            log.info("【食谱评分】总分: {}, 等级: {}, 目标: {}, 配置: {}天, 学校类型: {}, 宽松度: {}", 
                    totalScore, getGrade(totalScore), targetScore, dayCount, schoolType, tolerance);
            return ToolResult.text(formatResult(result));

        } catch (Exception e) {
            log.error("【食谱评分】评分失败", e);
            return ToolResult.error("评分失败: " + e.getMessage());
        }
    }

    /**
     * 计算综合宽松度
     * 学校类型和餐费的综合效果
     */
    private double calculateTolerance(String schoolType, Double mealPrice) {
        // 基础宽松度来自学校类型
        double baseTolerance = SCHOOL_TYPE_TOLERANCE.getOrDefault(schoolType, 1.05);

        // 餐费宽松度
        double priceTolerance = 1.0;
        if (mealPrice != null && mealPrice > 0) {
            priceTolerance = getPriceTolerance(mealPrice);
        }

        // 综合宽松度：取较大值（更宽松）
        // 如果餐费很低，即使城市学校也应该放宽评分
        return Math.max(baseTolerance, priceTolerance);
    }

    /**
     * 根据餐费计算宽松度
     */
    private double getPriceTolerance(double mealPrice) {
        for (double[] tier : PRICE_TIERS) {
            if (mealPrice >= tier[0] && mealPrice < tier[1]) {
                return tier[2];
            }
        }
        return 1.0;
    }

    /**
     * 计算已配置餐次的总占比
     */
    private double calculateTotalMealRatio(List<String> mealTypes) {
        double total = 0;
        for (String meal : mealTypes) {
            Double ratio = MEAL_RATIO.get(meal);
            if (ratio != null) {
                total += ratio;
            } else {
                // 未知餐次，默认按平均处理
                total += 0.15;
            }
        }
        return Math.min(1.0, total); // 最大不超过100%
    }

    /**
     * 获取全天营养标准
     */
    private Map<String, Object> getDailyStandard(String crowdType) {
        String matchedType = crowdType.toLowerCase();
        if (matchedType.contains("学龄前") || matchedType.contains("3-6")) {
            // 学龄前儿童（3-6岁）
            return Map.of(
                    "calorieMin", 1000, "calorieMax", 1200,
                    "proteinMin", 30, "proteinMax", 35,
                    "fatMin", 33, "fatMax", 40,
                    "carbsMin", 150, "carbsMax", 180);
        } else if (matchedType.contains("小学低") || matchedType.contains("6-9") || matchedType.contains("1-3年级")) {
            // 小学低年级（6-9岁）
            return Map.of(
                    "calorieMin", 1200, "calorieMax", 1500,
                    "proteinMin", 35, "proteinMax", 45,
                    "fatMin", 40, "fatMax", 50,
                    "carbsMin", 180, "carbsMax", 220);
        } else if (matchedType.contains("小学高") || matchedType.contains("10-12") || matchedType.contains("4-6年级")) {
            // 小学高年级（10-12岁）
            return Map.of(
                    "calorieMin", 1500, "calorieMax", 1800,
                    "proteinMin", 45, "proteinMax", 55,
                    "fatMin", 50, "fatMax", 60,
                    "carbsMin", 220, "carbsMax", 270);
        } else if (matchedType.contains("初中") || matchedType.contains("13-15")) {
            // 初中生（13-15岁）
            return Map.of(
                    "calorieMin", 2000, "calorieMax", 2400,
                    "proteinMin", 55, "proteinMax", 70,
                    "fatMin", 60, "fatMax", 75,
                    "carbsMin", 300, "carbsMax", 360);
        } else if (matchedType.contains("高中") || matchedType.contains("16-18")) {
            // 高中生（16-18岁）
            return Map.of(
                    "calorieMin", 2200, "calorieMax", 2600,
                    "proteinMin", 65, "proteinMax", 80,
                    "fatMin", 70, "fatMax", 85,
                    "carbsMin", 330, "carbsMax", 400);
        } else {
            // 教师/默认（成人标准）
            return Map.of(
                    "calorieMin", 1800, "calorieMax", 2200,
                    "proteinMin", 50, "proteinMax", 65,
                    "fatMin", 50, "fatMax", 65,
                    "carbsMin", 250, "carbsMax", 320);
        }
    }

    private double calcScore(double value, double min, double max) {
        if (value >= min && value <= max) {
            return 100;
        } else if (value < min) {
            return Math.max(0, 100 - (min - value) / min * 100);
        } else {
            return Math.max(0, 100 - (value - max) / max * 100);
        }
    }

    private double calcFatScore(double value, double min, double max) {
        // 脂肪评分稍微宽松一些
        return calcScore(value, min * 0.8, max * 1.2);
    }

    /**
     * 多样性评分
     */
    private double calcVarietyScore(int dishCount, int minDishCount, double tolerance) {
        // 宽松度越高，对多样性的要求越低
        double adjustedMin = minDishCount / tolerance;
        if (dishCount >= adjustedMin * 2) {
            return 100;
        } else if (dishCount >= adjustedMin) {
            return 80 + (dishCount - adjustedMin) / adjustedMin * 20;
        } else {
            return Math.max(0, dishCount / adjustedMin * 80);
        }
    }

    private String getGrade(double score) {
        if (score >= 90) return "A";
        if (score >= 80) return "B";
        if (score >= 70) return "C";
        if (score >= 60) return "D";
        return "F";
    }

    private List<String> generateSuggestions(double calorie, double protein, double fat, double carbs,
                                              double calorieScore, double proteinScore, 
                                              double fatScore, double carbsScore, double varietyScore,
                                              double recCalorieMin, double recCalorieMax,
                                              List<String> mealTypes, int dayCount,
                                              String schoolType, Double mealPrice, 
                                              double tolerance, int targetScore) {
        List<String> suggestions = new ArrayList<>();

        // 配置信息
        String mealInfo = "📋 已配置: " + dayCount + "天 × " + String.join("、", mealTypes);
        suggestions.add(mealInfo);

        // 学校类型和评分标准说明
        String schoolInfo = "🏫 学校类型: " + schoolType;
        if (tolerance > 1.0) {
            schoolInfo += "（宽松度: " + Math.round((tolerance - 1) * 100) + "%）";
        }
        suggestions.add(schoolInfo);

        // 餐费信息
        if (mealPrice != null && mealPrice > 0) {
            String priceInfo = "💰 每餐预算: " + mealPrice + "元/人";
            if (mealPrice < 8) {
                priceInfo += "（经济型，评分已放宽）";
            } else if (mealPrice >= 12) {
                priceInfo += "（充裕型，标准评分）";
            }
            suggestions.add(priceInfo);
        }

        // 目标分数
        suggestions.add("🎯 目标评分: " + targetScore + "+分");

        suggestions.add(""); // 空行分隔

        // 营养建议
        if (calorieScore < 70) {
            if (calorie < recCalorieMin) {
                suggestions.add("⚠️ 热量偏低（实际" + Math.round(calorie) + "kcal，推荐" + Math.round(recCalorieMin) + "-" + Math.round(recCalorieMax) + "kcal）");
            } else {
                suggestions.add("⚠️ 热量偏高（实际" + Math.round(calorie) + "kcal，推荐" + Math.round(recCalorieMin) + "-" + Math.round(recCalorieMax) + "kcal）");
            }
        } else if (calorieScore >= 90) {
            suggestions.add("✅ 热量达标（" + Math.round(calorie) + "kcal）");
        }

        if (proteinScore < 70) {
            suggestions.add("⚠️ 蛋白质不足，建议增加肉类、蛋类、豆制品");
        } else if (proteinScore >= 90) {
            suggestions.add("✅ 蛋白质达标（" + Math.round(protein) + "g）");
        }

        if (fatScore < 60) {
            suggestions.add("⚠️ 脂肪偏低，可适量增加坚果或油脂");
        } else if (fatScore < 80) {
            suggestions.add("💡 脂肪适中，继续保持");
        }

        if (carbsScore < 60) {
            suggestions.add("⚠️ 碳水不足，建议增加主食");
        } else if (carbsScore < 80) {
            suggestions.add("💡 碳水适中");
        }

        if (varietyScore < 60) {
            suggestions.add("⚠️ 菜品多样性不足，建议增加食材种类");
        } else if (varietyScore >= 80) {
            suggestions.add("✅ 菜品多样性良好");
        }

        // 根据学校类型的特殊建议
        suggestions.add(""); // 空行分隔
        if ("农村".equals(schoolType)) {
            suggestions.add("🌾 农村学校建议:");
            suggestions.add("  - 可优先使用当地时令蔬菜");
            suggestions.add("  - 豆制品是优质经济的蛋白质来源");
            suggestions.add("  - 评分标准已考虑经济条件，目标80+分即可");
        } else if ("普通城市".equals(schoolType)) {
            suggestions.add("🏙️ 普通城市学校建议:");
            suggestions.add("  - 合理搭配荤素，控制成本");
            suggestions.add("  - 评分标准已适度放宽，目标90+分");
        } else {
            suggestions.add("🌟 优质城市学校建议:");
            suggestions.add("  - 可增加优质蛋白（鱼虾、牛肉等）");
            suggestions.add("  - 注重食材多样性和营养均衡");
            suggestions.add("  - 优质学校应能轻松达到95+分");
        }

        // 餐费相关建议
        if (mealPrice != null && mealPrice > 0) {
            suggestions.add("");
            if (mealPrice < 5) {
                suggestions.add("💡 经济餐建议: 多选豆腐、鸡蛋、时令蔬菜，营养又实惠");
            } else if (mealPrice >= 12) {
                suggestions.add("💡 充裕预算: 可增加优质食材，如鱼虾、牛肉、有机蔬菜");
            }
        }

        return suggestions;
    }

    private String formatResult(Map<String, Object> result) {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 食谱营养评分报告\n");
        sb.append("====================\n\n");

        sb.append("【评分配置】\n");
        sb.append("- 学校类型: ").append(result.get("schoolType")).append("\n");
        sb.append("- 配置天数: ").append(result.get("dayCount")).append(" 天\n");
        sb.append("- 已配置餐次: ").append(result.get("configuredMeals")).append("\n");
        sb.append("- 餐次占比: ").append(result.get("mealRatioPercent")).append("\n");
        
        Double mealPrice = (Double) result.get("mealPrice");
        if (mealPrice != null && mealPrice > 0) {
            sb.append("- 每餐预算: ").append(mealPrice).append(" 元/人\n");
        }
        
        Double tolerance = (Double) result.get("tolerance");
        if (tolerance != null && tolerance > 1.0) {
            sb.append("- 评分宽松度: ").append(Math.round((tolerance - 1) * 100)).append("%\n");
        }
        sb.append("\n");

        sb.append("【总体评分】\n");
        sb.append("- 总分: ").append(result.get("totalScore")).append("/100\n");
        sb.append("- 等级: ").append(result.get("grade")).append("\n");
        sb.append("- 目标分数: ").append(result.get("targetScore")).append("+\n\n");

        sb.append("【推荐摄入范围】（标准）\n");
        @SuppressWarnings("unchecked")
        Map<String, Object> recRange = (Map<String, Object>) result.get("recommendedRange");
        @SuppressWarnings("unchecked")
        Map<String, Object> calRange = (Map<String, Object>) recRange.get("calorie");
        sb.append("- 热量: ").append(calRange.get("min")).append("-").append(calRange.get("max")).append(" kcal\n");
        @SuppressWarnings("unchecked")
        Map<String, Object> protRange = (Map<String, Object>) recRange.get("protein");
        sb.append("- 蛋白质: ").append(protRange.get("min")).append("-").append(protRange.get("max")).append(" g\n\n");

        // 如果有宽松度调整，显示调整后的范围
        if (tolerance != null && tolerance > 1.0) {
            sb.append("【调整后范围】（宽松模式）\n");
            @SuppressWarnings("unchecked")
            Map<String, Object> adjRange = (Map<String, Object>) result.get("adjustedRange");
            @SuppressWarnings("unchecked")
            Map<String, Object> adjCalRange = (Map<String, Object>) adjRange.get("calorie");
            sb.append("- 热量: ").append(adjCalRange.get("min")).append("-").append(adjCalRange.get("max")).append(" kcal\n");
            @SuppressWarnings("unchecked")
            Map<String, Object> adjProtRange = (Map<String, Object>) adjRange.get("protein");
            sb.append("- 蛋白质: ").append(adjProtRange.get("min")).append("-").append(adjProtRange.get("max")).append(" g\n\n");
        }

        sb.append("【各项得分】\n");
        @SuppressWarnings("unchecked")
        Map<String, Object> breakdown = (Map<String, Object>) result.get("breakdown");

        for (String key : breakdown.keySet()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> item = (Map<String, Object>) breakdown.get(key);
            sb.append("- ").append(key).append(": ")
                    .append(String.format("%.1f", item.get("score")))
                    .append(" (实际值: ").append(item.get("value")).append(")\n");
        }

        sb.append("\n【营养建议】\n");
        @SuppressWarnings("unchecked")
        List<String> suggestions = (List<String>) result.get("suggestions");
        for (String s : suggestions) {
            sb.append(s).append("\n");
        }

        return sb.toString();
    }

    private double getNumberArgument(Map<String, Object> arguments, String key, double defaultValue) {
        Object value = arguments.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return Double.parseDouble(value.toString());
    }

    private int getIntArgument(Map<String, Object> arguments, String key, int defaultValue) {
        Object value = arguments.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(value.toString());
    }
}
