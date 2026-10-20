package io.yunxi.mcp.nutrition.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.*;

/**
 * 评分规则配置加载器 —— 从 YAML 文件读取评分规则并存入 {@link ScoringConfig}。
 *
 * <p>职责分离：本类只负责配置加载与解析，不涉及评分计算逻辑。
 * {@link ScoringEngine} 通过注入本类获取配置，实现关注点分离。</p>
 *
 * <p>配置路径支持 classpath:/file:/http: 前缀，默认 {@code classpath:rules/scoring-rules.yml}。</p>
 */
@Component
public class ScoringConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(ScoringConfigLoader.class);

    private final ResourceLoader resourceLoader;

    @Value("${framework.scoring.config:classpath:rules/scoring-rules.yml}")
    private String configPath;

    private volatile ScoringConfig config;

    public ScoringConfigLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * 获取评分配置（懒加载，首次访问时从 YAML 加载）。
     */
    public ScoringConfig getConfig() {
        if (config == null) {
            synchronized (this) {
                if (config == null) {
                    config = loadConfig();
                }
            }
        }
        return config;
    }

    /** 手动强制重新加载配置（用于热更新场景）。 */
    public ScoringConfig reload() {
        synchronized (this) {
            config = loadConfig();
            return config;
        }
    }

    // ─── 内部实现 ──────────────────────────

    private ScoringConfig loadConfig() {
        try {
            String resolvedPath = resolveConfigPath(configPath);
            Resource resource = resourceLoader.getResource(resolvedPath);
            if (!resource.exists()) {
                log.warn("评分规则文件未找到: {}，使用内置默认值", resolvedPath);
                return ScoringConfig.defaults();
            }
            Yaml yaml = new Yaml();
            try (InputStream input = resource.getInputStream()) {
                Map<String, Object> raw = yaml.load(input);
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) raw.get("scoring");
                if (map == null) {
                    log.warn("评分规则 YAML 缺少 'scoring' 根键，使用内置默认值");
                    return ScoringConfig.defaults();
                }
                ScoringConfig cfg = parseConfig(map);
                log.info("评分配置加载完成: {} 维度, {} 人群标准, {} 等级 (路径: {})",
                        cfg.weights().size(), cfg.standards().size(), cfg.grades().size(), resolvedPath);
                return cfg;
            }
        } catch (Exception e) {
            log.warn("评分配置加载失败: {}，使用内置默认值", e.getMessage());
            return ScoringConfig.defaults();
        }
    }

    private String resolveConfigPath(String path) {
        if (path.startsWith("classpath:")) {
            return "classpath:" + path.substring("classpath:".length()).replaceFirst("^/*", "");
        }
        if (path.startsWith("file:") || path.startsWith("http:") || path.startsWith("https:")) {
            return path;
        }
        return "classpath:" + path.replaceFirst("^/*", "");
    }

    @SuppressWarnings("unchecked")
    private ScoringConfig parseConfig(Map<String, Object> config) {
        Map<String, Double> weights = new LinkedHashMap<>();
        Map<String, Object> w = (Map<String, Object>) config.get("weights");
        if (w != null) w.forEach((k, v) -> {
            Double d = toDoubleOrNull(v, "weights." + k);
            if (d != null) weights.put(k, d);
        });

        Map<String, Double> schoolTypeTolerance = new LinkedHashMap<>();
        List<double[]> priceTiers = new ArrayList<>();
        Map<String, Object> tol = (Map<String, Object>) config.get("tolerances");
        if (tol != null) {
            Map<String, Object> schoolTol = (Map<String, Object>) tol.get("school-type");
            if (schoolTol != null) schoolTol.forEach((k, v) -> {
                Double d = toDoubleOrNull(v, "tolerances.school-type." + k);
                if (d != null) schoolTypeTolerance.put(k, d);
            });
            List<List<Object>> priceTol = (List<List<Object>>) tol.get("price-tiers");
            if (priceTol != null) {
                for (List<Object> tier : priceTol) {
                    if (tier == null || tier.size() < 3) {
                        log.warn("评分配置 tolerances.price-tiers 存在非法区间（需 3 个元素），已跳过");
                        continue;
                    }
                    Double lo = toDoubleOrNull(tier.get(0), "tolerances.price-tiers[].0");
                    Double hi = toDoubleOrNull(tier.get(1), "tolerances.price-tiers[].1");
                    Double f = toDoubleOrNull(tier.get(2), "tolerances.price-tiers[].2");
                    if (lo != null && hi != null && f != null) {
                        priceTiers.add(new double[]{lo, hi, f});
                    }
                }
            }
        }

        List<NutritionStandard> standards = new ArrayList<>();
        List<Map<String, Object>> stds = (List<Map<String, Object>>) config.get("standards");
        if (stds != null) {
            for (Map<String, Object> s : stds) {
                standards.add(NutritionStandard.fromMap(s));
            }
        }

        List<GradeThreshold> grades = new ArrayList<>();
        Map<String, Object> g = (Map<String, Object>) config.get("grades");
        if (g != null) g.forEach((k, v) -> {
            Double d = toDoubleOrNull(v, "grades." + k);
            if (d != null) grades.add(new GradeThreshold(k, d));
        });
        grades.sort((a, b) -> Double.compare(b.threshold, a.threshold));

        // ── 微量元素权重与每日推荐摄入量 ──
        Map<String, Double> microWeights = new LinkedHashMap<>();
        Object microItems = config.get("micro_items");
        if (microItems instanceof Map) {
            ((Map<String, Object>) microItems).forEach((k, v) -> {
                Double d = toDoubleOrNull(v, "micro_items." + k);
                if (d != null) microWeights.put(k, d);
            });
        }
        Map<String, Double> microRda = new LinkedHashMap<>();
        Object rda = config.get("micro-rda");
        if (rda instanceof Map) {
            ((Map<String, Object>) rda).forEach((k, v) -> {
                Double d = toDoubleOrNull(v, "micro-rda." + k);
                if (d != null) microRda.put(k, d);
            });
        }

        // 居民人均级 RDA 覆盖（按人群名 → 微量元素 → 数值）
        Map<String, Map<String, Double>> microRdaResident = new LinkedHashMap<>();
        Object residentRda = config.get("micro-rda-resident");
        if (residentRda instanceof Map) {
            ((Map<String, Object>) residentRda).forEach((crowd, val) -> {
                if (val instanceof Map) {
                    Map<String, Double> inner = new LinkedHashMap<>();
                    ((Map<String, Object>) val).forEach((k, v) -> {
                        Double d = toDoubleOrNull(v, "micro-rda-resident." + crowd + "." + k);
                        if (d != null) inner.put(k, d);
                    });
                    microRdaResident.put(crowd, inner);
                }
            });
        }

        return new ScoringConfig(weights, schoolTypeTolerance, priceTiers, standards, grades, microWeights, microRda, microRdaResident);
    }

    // ─── 内部类型 ──────────────────────────

    /**
     * 安全地把配置值转成 double：非数值类型（如误嵌套的 LinkedHashMap）返回 null 并记 WARN，
     * 交由调用方跳过该条目，避免单点畸形拖垮整份配置回退默认值。
     */
    private static Double toDoubleOrNull(Object v, String path) {
        if (v instanceof Number n) return n.doubleValue();
        log.warn("评分配置字段 '{}' 不是数值（实际类型 {}），该条目已跳过",
                path, v == null ? "null" : v.getClass().getSimpleName());
        return null;
    }

    public record NutritionStandard(String name, List<String> keywords,
                                    double calorieMin, double calorieMax,
                                    double proteinMin, double proteinMax,
                                    double fatMin, double fatMax,
                                    double carbsMin, double carbsMax) {
        @SuppressWarnings("unchecked")
        public static NutritionStandard fromMap(Map<String, Object> m) {
            List<String> kw = m.containsKey("keywords") ? (List<String>) m.get("keywords") : List.of();
            return new NutritionStandard(
                    (String) m.get("crowd"), kw,
                    getDouble(m, "calorie", 0), getDouble(m, "calorie", 1),
                    getDouble(m, "protein", 0), getDouble(m, "protein", 1),
                    getDouble(m, "fat", 0), getDouble(m, "fat", 1),
                    getDouble(m, "carbs", 0), getDouble(m, "carbs", 1));
        }
        private static double getDouble(Map<String, Object> m, String key, int idx) {
            Object val = m.get(key);
            if (val instanceof List<?> list) {
                Object item = list.get(idx);
                if (item instanceof Number n) return n.doubleValue();
                log.warn("评分配置 standards 字段 '{}[{}]' 不是数值，按 0 处理", key, idx);
                return 0;
            }
            if (val instanceof Number n) return n.doubleValue();
            log.warn("评分配置 standards 字段 '{}' 不是数值，按 0 处理", key);
            return 0;
        }
    }

    public record GradeThreshold(String grade, double threshold) {}
}
