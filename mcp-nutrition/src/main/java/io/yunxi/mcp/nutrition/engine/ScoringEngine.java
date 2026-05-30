package io.yunxi.mcp.nutrition.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.*;

/**
 * 声明式评分引擎
 *
 * <p>读取 YAML 评分规则配置，对输入数据进行加权评分计算。
 * 替代手写的 RecipeEvaluator 等 Java 评分算法。</p>
 *
 * <p>配置示例：</p>
 * <pre>
 * scoring:
 *   name: "食谱营养评分"
 *   weights:
 *     calorie: 0.30
 *     protein: 0.25
 *     fat: 0.15
 *     carbs: 0.15
 *     variety: 0.15
 *   tolerances:
 *     school-type:
 *       优质城市: 1.10
 *       普通城市: 1.20
 *       农村: 1.35
 *   standards:
 *     - crowd: "学龄前"
 *       calorie: [1000, 1200]
 *       protein: [30, 35]
 *       fat: [33, 40]
 *       carbs: [150, 180]
 *     - crowd: "小学低年级"
 *       calorie: [1200, 1500]
 *       ...
 *   grades:
 *     A: 90
 *     B: 80
 *     C: 70
 *     D: 60
 * </pre>
 *
 * @author yunxi-agent-platform
 */
@Component
public class ScoringEngine implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(ScoringEngine.class);

    /** 评分规则配置路径 */
    @Value("${framework.scoring.config:classpath:rules/scoring-rules.yml}")
    private String configPath;

    /** 权重映射 */
    private Map<String, Double> weights = new LinkedHashMap<>();

    /** 学校类型宽松度 */
    private Map<String, Double> schoolTypeTolerance = new LinkedHashMap<>();

    /** 餐费价格区间宽松度: [minPrice, maxPrice, tolerance] */
    private List<double[]> priceTiers = new ArrayList<>();

    /** 各人群营养标准 */
    private List<NutritionStandard> standards = new ArrayList<>();

    /** 等级阈值 */
    private List<GradeThreshold> grades = new ArrayList<>();

    @Override
    @PostConstruct
    public void afterPropertiesSet() {
        try {
            Yaml yaml = new Yaml();
            InputStream input = getClass().getClassLoader().getResourceAsStream("rules/scoring-rules.yml");
            if (input == null) {
                log.warn("评分规则文件未找到: rules/scoring-rules.yml，使用内置默认值");
                loadDefaults();
                return;
            }
            Map<String, Object> config = yaml.load(input);
            parseConfig(config);
            log.info("评分引擎初始化完成: {} 个评分维度, {} 个人群标准, {} 个等级",
                    weights.size(), standards.size(), grades.size());
        } catch (Exception e) {
            log.warn("评分引擎初始化失败，使用默认值: {}", e.getMessage());
            loadDefaults();
        }
    }

    /**
     * 执行评分
     *
     * @param request 评分请求
     * @return 评分结果
     */
    public ScoringResult evaluate(ScoringRequest request) {
        double tolerance = calculateTolerance(request.getSchoolType(), request.getMealPrice());
        double totalMealRatio = calculateTotalMealRatio(request.getMealTypes());
        int dayCount = Math.max(1, request.getDayCount());

        NutritionStandard std = findStandard(request.getCrowdType());
        if (std == null) {
            return ScoringResult.error("未找到人群标准: " + request.getCrowdType());
        }

        // 计算各维度得分
        Map<String, DimensionScore> dimensions = new LinkedHashMap<>();
        dimensions.put("calorie", scoreDimension(request.getCalorie(), std.calorieMin, std.calorieMax, tolerance, totalMealRatio, dayCount));
        dimensions.put("protein", scoreDimension(request.getProtein(), std.proteinMin, std.proteinMax, tolerance, totalMealRatio, dayCount));
        dimensions.put("fat", scoreFatDimension(request.getFat(), std.fatMin, std.fatMax, tolerance, totalMealRatio, dayCount));
        dimensions.put("carbs", scoreDimension(request.getCarbs(), std.carbsMin, std.carbsMax, tolerance, totalMealRatio, dayCount));
        dimensions.put("variety", scoreVariety(request.getDishCount(), request.getMealTypes().size() * dayCount, tolerance));

        // 加权总分
        double totalScore = 0;
        for (Map.Entry<String, DimensionScore> entry : dimensions.entrySet()) {
            Double weight = weights.getOrDefault(entry.getKey(), 0.0);
            totalScore += entry.getValue().score() * weight;
        }
        totalScore = Math.round(totalScore * 10) / 10.0;

        return ScoringResult.builder()
                .totalScore(totalScore)
                .grade(getGrade(totalScore))
                .tolerance(tolerance)
                .dimensions(dimensions)
                .targetScore(calculateTargetScore(request.getSchoolType()))
                .build();
    }

    // ==================== 内部计算 ====================

    private double calculateTolerance(String schoolType, Double mealPrice) {
        double baseTol = schoolTypeTolerance.getOrDefault(schoolType, 1.05);
        double priceTol = 1.0;
        if (mealPrice != null && mealPrice > 0) {
            for (double[] tier : priceTiers) {
                if (mealPrice >= tier[0] && mealPrice < tier[1]) {
                    priceTol = tier[2];
                    break;
                }
            }
        }
        return Math.max(baseTol, priceTol);
    }

    private double calculateTotalMealRatio(List<String> mealTypes) {
        Map<String, Double> ratios = Map.of(
                "早餐", 0.30, "早点", 0.06,
                "午餐", 0.38, "午点", 0.06,
                "晚餐", 0.30, "晚点", 0.06);
        double total = 0;
        for (String meal : mealTypes) {
            total += ratios.getOrDefault(meal, 0.15);
        }
        return Math.min(1.0, total);
    }

    private NutritionStandard findStandard(String crowdType) {
        String t = crowdType.toLowerCase();
        for (NutritionStandard s : standards) {
            for (String keyword : s.keywords) {
                if (t.contains(keyword)) return s;
            }
        }
        return standards.isEmpty() ? null : standards.get(standards.size() - 1);
    }

    private DimensionScore scoreDimension(double value, double min, double max,
                                           double tolerance, double mealRatio, int dayCount) {
        double adjMin = (min * dayCount * mealRatio) / tolerance;
        double adjMax = (max * dayCount * mealRatio) * tolerance;
        double score;
        if (value >= adjMin && value <= adjMax) score = 100;
        else if (value < adjMin) score = Math.max(0, 100 - (adjMin - value) / adjMin * 100);
        else score = Math.max(0, 100 - (value - adjMax) / adjMax * 100);
        return new DimensionScore(Math.round(score * 10) / 10.0, value, adjMin, adjMax);
    }

    private DimensionScore scoreFatDimension(double value, double min, double max,
                                              double tolerance, double mealRatio, int dayCount) {
        return scoreDimension(value, min * 0.8, max * 1.2, tolerance, mealRatio, dayCount);
    }

    private DimensionScore scoreVariety(int dishCount, int minDishCount, double tolerance) {
        double adjustedMin = minDishCount / tolerance;
        double score;
        if (dishCount >= adjustedMin * 2) score = 100;
        else if (dishCount >= adjustedMin) score = 80 + (dishCount - adjustedMin) / adjustedMin * 20;
        else score = Math.max(0, dishCount / adjustedMin * 80);
        return new DimensionScore(Math.round(score * 10) / 10.0, dishCount, adjustedMin, adjustedMin * 2);
    }

    private String getGrade(double score) {
        for (GradeThreshold g : grades) {
            if (score >= g.threshold) return g.grade;
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

    // ==================== 配置解析 ====================

    @SuppressWarnings("unchecked")
    private void parseConfig(Map<String, Object> config) {
        // 权重
        Map<String, Object> w = (Map<String, Object>) config.get("weights");
        if (w != null) w.forEach((k, v) -> weights.put(k, ((Number) v).doubleValue()));

        // 学校类型宽松度
        Map<String, Object> tol = (Map<String, Object>) config.get("tolerances");
        if (tol != null) {
            Map<String, Object> schoolTol = (Map<String, Object>) tol.get("school-type");
            if (schoolTol != null) schoolTol.forEach((k, v) -> schoolTypeTolerance.put(k, ((Number) v).doubleValue()));

            List<List<Object>> priceTol = (List<List<Object>>) tol.get("price-tiers");
            if (priceTol != null) {
                for (List<Object> tier : priceTol) {
                    priceTiers.add(new double[]{
                            ((Number) tier.get(0)).doubleValue(),
                            ((Number) tier.get(1)).doubleValue(),
                            ((Number) tier.get(2)).doubleValue()
                    });
                }
            }
        }

        // 人群标准
        List<Map<String, Object>> stds = (List<Map<String, Object>>) config.get("standards");
        if (stds != null) {
            for (Map<String, Object> s : stds) {
                standards.add(NutritionStandard.fromMap(s));
            }
        }

        // 等级
        Map<String, Object> g = (Map<String, Object>) config.get("grades");
        if (g != null) g.forEach((k, v) -> grades.add(new GradeThreshold(k, ((Number) v).doubleValue())));
        grades.sort((a, b) -> Double.compare(b.threshold, a.threshold));
    }

    private void loadDefaults() {
        weights.putAll(Map.of("calorie", 0.30, "protein", 0.25, "fat", 0.15, "carbs", 0.15, "variety", 0.15));
        schoolTypeTolerance.putAll(Map.of("优质城市", 1.10, "普通城市", 1.20, "农村", 1.35));
        priceTiers.addAll(List.of(
                new double[]{0, 5, 1.30}, new double[]{5, 8, 1.20},
                new double[]{8, 12, 1.10}, new double[]{12, Double.MAX_VALUE, 1.0}));
        grades.addAll(List.of(
                new GradeThreshold("A", 90), new GradeThreshold("B", 80),
                new GradeThreshold("C", 70), new GradeThreshold("D", 60)));
    }

    // ==================== 内部类 ====================

    public record DimensionScore(double score, double actualValue, double rangeMin, double rangeMax) {}
    public record GradeThreshold(String grade, double threshold) {}
    public record NutritionStandard(String name, List<String> keywords,
                                    double calorieMin, double calorieMax,
                                    double proteinMin, double proteinMax,
                                    double fatMin, double fatMax,
                                    double carbsMin, double carbsMax) {
        static NutritionStandard fromMap(Map<String, Object> m) {
            List<String> kw = m.containsKey("keywords") ? (List<String>) m.get("keywords") : List.of();
            return new NutritionStandard(
                    (String) m.get("crowd"), kw,
                    getDouble(m, "calorie", 0), getDouble(m, "calorie", 1),
                    getDouble(m, "protein", 0), getDouble(m, "protein", 1),
                    getDouble(m, "fat", 0), getDouble(m, "fat", 1),
                    getDouble(m, "carbs", 0), getDouble(m, "carbs", 1));
        }
        static double getDouble(Map<String, Object> m, String key, int idx) {
            Object val = m.get(key);
            if (val instanceof List) return ((Number) ((List<?>) val).get(idx)).doubleValue();
            if (val instanceof Number) return ((Number) val).doubleValue();
            return 0;
        }
    }

    /** 评分请求 */
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
    }

    /** 评分结果 */
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
        private String error;

        static ScoringResult error(String msg) {
            return ScoringResult.builder().error(msg).build();
        }
    }
}