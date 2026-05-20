package io.yunxi.mcp.milvus.tool;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import io.yunxi.mcp.milvus.config.MilvusClientHolder;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 获取营养标准工具
 * <p>
 * 获取不同人群的营养素推荐标准，用于食谱评分和营养分析。
 * </p>
 */
@Slf4j
public class GetNutrientStandardTool implements ToolHandler {

    private final MilvusClientHolder clientHolder;

    // 默认营养标准（基于中国居民膳食指南）
    private static final Map<String, Map<String, Object>> DEFAULT_STANDARDS = new LinkedHashMap<>();

    static {
        // 小学生（7-12岁）
        DEFAULT_STANDARDS.put("小学生", Map.of(
                "calorieMin", 1400, "calorieMax", 2000,
                "proteinMin", 40, "proteinMax", 65,
                "fatMin", 45, "fatMax", 70,
                "carbsMin", 180, "carbsMax", 280,
                "description", "7-12岁小学生"
        ));

        // 初中生（12-15岁）
        DEFAULT_STANDARDS.put("初中生", Map.of(
                "calorieMin", 1800, "calorieMax", 2400,
                "proteinMin", 55, "proteinMax", 75,
                "fatMin", 50, "fatMax", 80,
                "carbsMin", 220, "carbsMax", 320,
                "description", "12-15岁初中生"
        ));

        // 高中生（15-18岁）
        DEFAULT_STANDARDS.put("高中生", Map.of(
                "calorieMin", 2000, "calorieMax", 2800,
                "proteinMin", 60, "proteinMax", 85,
                "fatMin", 55, "fatMax", 90,
                "carbsMin", 250, "carbsMax", 380,
                "description", "15-18岁高中生"
        ));

        // 教师
        DEFAULT_STANDARDS.put("教师", Map.of(
                "calorieMin", 1800, "calorieMax", 2400,
                "proteinMin", 50, "proteinMax", 70,
                "fatMin", 50, "fatMax", 75,
                "carbsMin", 200, "carbsMax", 300,
                "description", "成人教师"
        ));
    }

    public GetNutrientStandardTool(MilvusClientHolder clientHolder) {
        this.clientHolder = clientHolder;
    }

    @Override
    public ToolDefinition getDefinition() {
        return ToolDefinition.builder()
                .name("get_nutrient_standard")
                .description(
                        "【重要】获取营养素推荐标准，用于食谱评分和营养分析。" +
                                "参数：crowdType(人群类型，如'小学生'、'初中生'、'高中生'、'教师')。" +
                                "返回每日营养素推荐量：热量(kcal)、蛋白质(g)、脂肪(g)、碳水化合物(g)的最小和最大值。")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "crowdType", Map.of(
                                        "type", "string",
                                        "description", "人群类型：小学生、初中生、高中生、教师"),
                                "mealCount", Map.of(
                                        "type", "integer",
                                        "description", "餐次数量（1=每日, 2=每日两餐, 3=每日三餐），用于计算每餐推荐量",
                                        "default", 3)),
                        "required", List.of("crowdType")))
                .build();
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        String crowdType = (String) arguments.get("crowdType");
        Integer mealCount = getIntArgument(arguments, "mealCount", 3);

        if (crowdType == null || crowdType.isBlank()) {
            return ToolResult.error("crowdType is required");
        }

        log.info("【营养标准】crowdType={}, mealCount={}", crowdType, mealCount);

        try {
            // 匹配人群类型
            String matchedType = matchCrowdType(crowdType);
            Map<String, Object> standard = DEFAULT_STANDARDS.get(matchedType);

            if (standard == null) {
                return ToolResult.error("未找到人群类型: " + crowdType + "，可选: " + DEFAULT_STANDARDS.keySet());
            }

            // 计算每餐推荐量
            int calorieMin = ((Number) standard.get("calorieMin")).intValue() / mealCount;
            int calorieMax = ((Number) standard.get("calorieMax")).intValue() / mealCount;
            int proteinMin = ((Number) standard.get("proteinMin")).intValue() / mealCount;
            int proteinMax = ((Number) standard.get("proteinMax")).intValue() / mealCount;
            int fatMin = ((Number) standard.get("fatMin")).intValue() / mealCount;
            int fatMax = ((Number) standard.get("fatMax")).intValue() / mealCount;
            int carbsMin = ((Number) standard.get("carbsMin")).intValue() / mealCount;
            int carbsMax = ((Number) standard.get("carbsMax")).intValue() / mealCount;

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("crowdType", matchedType);
            result.put("description", standard.get("description"));
            result.put("mealCount", mealCount);
            result.put("dailyStandard", standard);
            result.put("perMealStandard", Map.of(
                    "calorie", Map.of("min", calorieMin, "max", calorieMax, "unit", "kcal"),
                    "protein", Map.of("min", proteinMin, "max", proteinMax, "unit", "g"),
                    "fat", Map.of("min", fatMin, "max", fatMax, "unit", "g"),
                    "carbs", Map.of("min", carbsMin, "max", carbsMax, "unit", "g")
            ));

            log.info("【营养标准】返回标准: {}", matchedType);
            return ToolResult.text(formatResult(result));

        } catch (Exception e) {
            log.error("【营养标准】获取失败: crowdType={}", crowdType, e);
            return ToolResult.error("获取失败: " + e.getMessage());
        }
    }

    private String matchCrowdType(String input) {
        String lower = input.toLowerCase();
        if (lower.contains("小学")) return "小学生";
        if (lower.contains("初中")) return "初中生";
        if (lower.contains("高中")) return "高中生";
        if (lower.contains("教师") || lower.contains("老师") || lower.contains("成人")) return "教师";
        return "小学生"; // 默认
    }

    private String formatResult(Map<String, Object> result) {
        StringBuilder sb = new StringBuilder();
        sb.append("营养素推荐标准:\n");
        sb.append("- 人群: ").append(result.get("crowdType")).append("\n");
        sb.append("- 描述: ").append(result.get("description")).append("\n");
        sb.append("- 餐次: ").append(result.get("mealCount")).append("餐/日\n\n");

        @SuppressWarnings("unchecked")
        Map<String, Object> perMeal = (Map<String, Object>) result.get("perMealStandard");

        sb.append("【每餐推荐量】\n");
        sb.append("- 热量: ").append(((Map<String, Object>)perMeal.get("calorie")).get("min"))
                .append("-").append(((Map<String, Object>)perMeal.get("calorie")).get("max"))
                .append(" kcal\n");
        sb.append("- 蛋白质: ").append(((Map<String, Object>)perMeal.get("protein")).get("min"))
                .append("-").append(((Map<String, Object>)perMeal.get("protein")).get("max"))
                .append(" g\n");
        sb.append("- 脂肪: ").append(((Map<String, Object>)perMeal.get("fat")).get("min"))
                .append("-").append(((Map<String, Object>)perMeal.get("fat")).get("max"))
                .append(" g\n");
        sb.append("- 碳水: ").append(((Map<String, Object>)perMeal.get("carbs")).get("min"))
                .append("-").append(((Map<String, Object>)perMeal.get("carbs")).get("max"))
                .append(" g\n");

        return sb.toString();
    }

    private Integer getIntArgument(Map<String, Object> arguments, String key, int defaultValue) {
        Object value = arguments.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(value.toString());
    }
}
