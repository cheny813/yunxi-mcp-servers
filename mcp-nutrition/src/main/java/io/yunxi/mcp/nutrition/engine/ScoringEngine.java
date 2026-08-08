package io.yunxi.mcp.nutrition.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 声明式评分引擎 —— 依赖 {@link ScoringConfigLoader} 加载 YAML 规则，
 * 对输入数据进行加权评分计算。
 *
 * <h3>架构</h3>
 * <pre>
 *   RecipeEvaluateTool → ScoringEngine → ScoringConfigLoader → scoring-rules.yml
 * </pre>
 *
 * <p>配置加载与评分计算职责分离：
 * {@link ScoringConfigLoader} 负责 YAML 解析与默认值兜底；
 * 本类只负责评分算法与维度计算。</p>
 *
 * @author yunxi-agent-platform
 */
@Component
public class ScoringEngine {

    private static final Logger log = LoggerFactory.getLogger(ScoringEngine.class);

    private final ScoringConfigLoader configLoader;

    /** 餐次全天能量占比（中国居民膳食指南） */
    private static final Map<String, Double> MEAL_RATIOS = Map.of(
            "早餐", 0.30, "早点", 0.06,
            "午餐", 0.38, "午点", 0.06,
            "晚餐", 0.30, "晚点", 0.06);

    public ScoringEngine(ScoringConfigLoader configLoader) {
        this.configLoader = configLoader;
    }

    /**
     * 执行评分。
     */
    public ScoringResult evaluate(ScoringRequest request) {
        ScoringConfig config = configLoader.getConfig();
        double tolerance = calculateTolerance(config, request.getSchoolType(), request.getMealPrice());
        double totalMealRatio = calculateTotalMealRatio(request.getMealTypes());
        int dayCount = Math.max(1, request.getDayCount());

        ScoringConfigLoader.NutritionStandard std = findStandard(config, request.getCrowdType());
        if (std == null) {
            return ScoringResult.error("未找到人群标准: " + request.getCrowdType());
        }

        Map<String, ScoringEngine.DimensionScore> dimensions = new LinkedHashMap<>();
        dimensions.put("calorie", scoreDimension(request.getCalorie(), std.calorieMin(), std.calorieMax(), tolerance, totalMealRatio, dayCount));
        dimensions.put("protein", scoreDimension(request.getProtein(), std.proteinMin(), std.proteinMax(), tolerance, totalMealRatio, dayCount));
        dimensions.put("fat", scoreFatDimension(request.getFat(), std.fatMin(), std.fatMax(), tolerance, totalMealRatio, dayCount));
        dimensions.put("carbs", scoreDimension(request.getCarbs(), std.carbsMin(), std.carbsMax(), tolerance, totalMealRatio, dayCount));
        dimensions.put("variety", scoreVariety(request.getDishCount(), request.getMealTypes().size() * dayCount, tolerance));

        // ── 微量元素维度：按实际摄入 / 每日推荐摄入量 的达标率计分 ──
        // 优先采用居民人均级 RDA（micro-rda-resident），命中人群则用，否则用默认 micro-rda
        Map<String, Double> crowdRda = resolveCrowdRda(config, std.name());
        double microScore = scoreMicro(request, config, crowdRda);
        Map<String, ScoringEngine.DimensionScore> microDims = buildMicroDimensions(request, config, crowdRda);
        if (config.weights().containsKey("micro")) {
            dimensions.put("micro", new ScoringEngine.DimensionScore(
                    Math.round(microScore * 10) / 10.0, microScore, 60, 100));
        }

        double totalScore = 0;
        for (Map.Entry<String, ScoringEngine.DimensionScore> entry : dimensions.entrySet()) {
            Double weight = config.weights().getOrDefault(entry.getKey(), 0.0);
            totalScore += entry.getValue().score() * weight;
        }
        totalScore = Math.round(totalScore * 10) / 10.0;

        return ScoringResult.builder()
                .totalScore(totalScore)
                .grade(getGrade(config, totalScore))
                .tolerance(tolerance)
                .dimensions(dimensions)
                .microDimensions(microDims)
                .targetScore(calculateTargetScore(request.getSchoolType()))
                .build();
    }

    /**
     * 计算微量元素综合分：各子项（钙/铁/锌/维A/B1/B2）按 实际/推荐 达标率计分，
     * 再按 micro_items 子权重加权汇总。
     */
    private double scoreMicro(ScoringRequest request, ScoringConfig config, Map<String, Double> rda) {
        if (rda == null || rda.isEmpty()) return 0;
        Map<String, Double> itemWeights = config.microWeights();
        double sum = 0, wsum = 0;
        for (Map.Entry<String, Double> e : rda.entrySet()) {
            String key = e.getKey();
            double actual = getMicroActual(request, key);
            double rec = e.getValue();
            double itemScore = rec > 0 ? Math.min(100, actual / rec * 100) : 0;
            double w = itemWeights.getOrDefault(key, 1.0 / rda.size());
            sum += itemScore * w;
            wsum += w;
        }
        return wsum > 0 ? Math.round(sum / wsum * 10) / 10.0 : 0;
    }

    /** 生成各微量元素独立评分明细（用于前端逐条展示）。 */
    private Map<String, ScoringEngine.DimensionScore> buildMicroDimensions(ScoringRequest request, ScoringConfig config, Map<String, Double> rda) {
        Map<String, ScoringEngine.DimensionScore> map = new LinkedHashMap<>();
        if (rda == null) return map;
        for (Map.Entry<String, Double> e : rda.entrySet()) {
            String key = e.getKey();
            double actual = getMicroActual(request, key);
            double rec = e.getValue();
            double score = rec > 0 ? Math.min(100, actual / rec * 100) : 0;
            map.put(key, new ScoringEngine.DimensionScore(Math.round(score * 10) / 10.0, actual, 0, rec));
        }
        return map;
    }

    /**
     * 解析当前人群使用的微量元素 RDA：
     * 若 micro-rda-resident 配置了与标准人群名（std.name()）匹配的键，则优先使用；
     * 否则回退到默认 micro-rda（校园口径）。
     */
    private Map<String, Double> resolveCrowdRda(ScoringConfig config, String crowdName) {
        Map<String, Map<String, Double>> resident = config.microRdaResident();
        if (resident != null && crowdName != null) {
            for (Map.Entry<String, Map<String, Double>> e : resident.entrySet()) {
                String key = e.getKey().toLowerCase();
                if (crowdName.toLowerCase().contains(key) || key.contains(crowdName.toLowerCase())) {
                    return e.getValue();
                }
            }
        }
        return config.microRda();
    }

    private double getMicroActual(ScoringRequest request, String key) {
        return switch (key) {
            case "calcium" -> request.getCalcium();
            case "iron" -> request.getIron();
            case "zinc" -> request.getZinc();
            case "vitA" -> request.getVitA();
            case "vitB1" -> request.getVitB1();
            case "vitB2" -> request.getVitB2();
            case "vitC" -> request.getVitC();
            default -> 0;
        };
    }

    // ==================== 内部计算 ====================

    private double calculateTolerance(ScoringConfig config, String schoolType, Double mealPrice) {
        double baseTol = config.schoolTypeTolerance().getOrDefault(schoolType, 1.05);
        double priceTol = 1.0;
        if (mealPrice != null && mealPrice > 0) {
            for (double[] tier : config.priceTiers()) {
                if (mealPrice >= tier[0] && mealPrice < tier[1]) {
                    priceTol = tier[2];
                    break;
                }
            }
        }
        return Math.max(baseTol, priceTol);
    }

    private double calculateTotalMealRatio(List<String> mealTypes) {
        double total = 0;
        for (String meal : mealTypes) {
            total += MEAL_RATIOS.getOrDefault(meal, 0.15);
        }
        return Math.min(1.0, total);
    }

    private ScoringConfigLoader.NutritionStandard findStandard(ScoringConfig config, String crowdType) {
        String t = crowdType.toLowerCase();
        for (ScoringConfigLoader.NutritionStandard s : config.standards()) {
            for (String keyword : s.keywords()) {
                if (t.contains(keyword)) return s;
            }
        }
        return config.standards().isEmpty() ? null : config.standards().get(config.standards().size() - 1);
    }

    private ScoringEngine.DimensionScore scoreDimension(double value, double min, double max,
                                                        double tolerance, double mealRatio, int dayCount) {
        double adjMin = (min * dayCount * mealRatio) / tolerance;
        double adjMax = (max * dayCount * mealRatio) * tolerance;
        double score;
        if (value >= adjMin && value <= adjMax) score = 100;
        else if (value < adjMin) score = Math.max(0, 100 - (adjMin - value) / adjMin * 100);
        else score = Math.max(0, 100 - (value - adjMax) / adjMax * 100);
        return new ScoringEngine.DimensionScore(Math.round(score * 10) / 10.0, value, adjMin, adjMax);
    }

    private ScoringEngine.DimensionScore scoreFatDimension(double value, double min, double max,
                                                           double tolerance, double mealRatio, int dayCount) {
        return scoreDimension(value, min * 0.8, max * 1.2, tolerance, mealRatio, dayCount);
    }

    private ScoringEngine.DimensionScore scoreVariety(int dishCount, int minDishCount, double tolerance) {
        double adjustedMin = minDishCount / tolerance;
        double score;
        if (dishCount >= adjustedMin * 2) score = 100;
        else if (dishCount >= adjustedMin) score = 80 + (dishCount - adjustedMin) / adjustedMin * 20;
        else score = Math.max(0, dishCount / adjustedMin * 80);
        return new ScoringEngine.DimensionScore(Math.round(score * 10) / 10.0, dishCount, adjustedMin, adjustedMin * 2);
    }

    private String getGrade(ScoringConfig config, double score) {
        for (ScoringConfigLoader.GradeThreshold g : config.grades()) {
            if (score >= g.threshold()) return g.grade();
        }
        return "F";
    }

    private int calculateTargetScore(String schoolType) {
        return switch (schoolType != null ? schoolType : "") {
            case "优质城市" -> 95;
            case "普通城市" -> 90;
            case "农村" -> 80;
            default -> 80;
        };
    }

    // ==================== 内部类型 ====================

    public record DimensionScore(double score, double actualValue, double rangeMin, double rangeMax) {}

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ScoringRequest {
        private String crowdType;
        private double calorie;
        private double protein;
        private double fat;
        private double carbs;
        private int dishCount;
        private List<String> mealTypes;
        private int dayCount;
        private String schoolType;
        private Double mealPrice;
        // 微量元素实际摄入（来自菜品营养汇总；单位见 DishDto）
        private double calcium;
        private double iron;
        private double zinc;
        private double vitA;
        private double vitB1;
        private double vitB2;
        private double vitC;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ScoringResult {
        private double totalScore;
        private String grade;
        private double tolerance;
        private int targetScore;
        private Map<String, DimensionScore> dimensions;
        private Map<String, DimensionScore> microDimensions;
        private String error;

        static ScoringResult error(String msg) {
            return ScoringResult.builder().error(msg).build();
        }
    }
}
