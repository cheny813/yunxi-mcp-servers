package io.yunxi.mcp.nutrition.engine;

import java.util.*;

/**
 * 评分规则配置对象 —— 不可变，从 YAML 加载后传入 {@link ScoringEngine}。
 *
 * <p>提供 {@link #defaults()} 工厂方法生成内置默认值，
 * 确保即使没有外部配置文件也能正常工作。</p>
 */
public record ScoringConfig(
        Map<String, Double> weights,
        Map<String, Double> schoolTypeTolerance,
        List<double[]> priceTiers,
        List<ScoringConfigLoader.NutritionStandard> standards,
        List<ScoringConfigLoader.GradeThreshold> grades) {

    /**
     * 创建带默认值的配置（YAML 缺失时使用）。
     */
    public static ScoringConfig defaults() {
        Map<String, Double> weights = new LinkedHashMap<>();
        weights.putAll(Map.of("calorie", 0.30, "protein", 0.25, "fat", 0.15, "carbs", 0.15, "variety", 0.15));

        Map<String, Double> schoolTypeTolerance = new LinkedHashMap<>();
        schoolTypeTolerance.putAll(Map.of("优质城市", 1.10, "普通城市", 1.20, "农村", 1.35));

        List<double[]> priceTiers = new ArrayList<>();
        priceTiers.addAll(List.of(
                new double[]{0, 5, 1.30}, new double[]{5, 8, 1.20},
                new double[]{8, 12, 1.10}, new double[]{12, Double.MAX_VALUE, 1.0}));

        List<ScoringConfigLoader.GradeThreshold> grades = new ArrayList<>();
        grades.addAll(List.of(
                new ScoringConfigLoader.GradeThreshold("A", 90),
                new ScoringConfigLoader.GradeThreshold("B", 80),
                new ScoringConfigLoader.GradeThreshold("C", 70),
                new ScoringConfigLoader.GradeThreshold("D", 60)));

        return new ScoringConfig(weights, schoolTypeTolerance, priceTiers, Collections.emptyList(), grades);
    }
}
