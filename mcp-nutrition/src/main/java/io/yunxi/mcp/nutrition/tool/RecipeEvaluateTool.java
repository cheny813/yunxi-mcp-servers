package io.yunxi.mcp.nutrition.tool;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.nutrition.engine.ScoringEngine;
import io.yunxi.mcp.nutrition.engine.ScoringEngine.ScoringRequest;
import io.yunxi.mcp.nutrition.engine.ScoringEngine.ScoringResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 营养食谱评分工具 —— ScoringEngine 的 MCP Tool 适配层（v2.0 ToolHandler 接口）。
 *
 * <p>本类仅做参数映射和结果格式化，评分逻辑全部委托
 * {@link ScoringEngine}（YAML 驱动，可热更新），
 * 确保所有评分路径共享同一套规则。</p>
 *
 * <h3>架构</h3>
 * <pre>
 *   AI Agent → MCP Tool → RecipeEvaluateTool (thin adapter)
 *                           │
 *                           └→ ScoringEngine (single source of truth)
 *                                └→ ScoringConfigLoader → scoring-rules.yml
 * </pre>
 */
@Component
public class RecipeEvaluateTool implements ToolHandler {

    private static final Logger log = LoggerFactory.getLogger(RecipeEvaluateTool.class);

    private final ScoringEngine scoringEngine;

    public RecipeEvaluateTool(ScoringEngine scoringEngine) {
        this.scoringEngine = scoringEngine;
    }

    @Override
    public String getName() {
        return "evaluateRecipe";
    }

    @Override
    public String getDescription() {
        return "评估学生营养餐食谱的营养均衡度和食材多样性，返回评分等级和改进建议。"
                + "参数包括热量(kcal)、蛋白质(g)、脂肪(g)、碳水化合物(g)、人群类型(crowdType)、"
                + "学校类型(schoolType)、餐费价格(mealPrice)，可选参数菜品数(dishCount)、餐次列表(mealTypes)、天数(dayCount)";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("calorie", Map.of("type", "number", "description", "实际摄入热量（kcal/人/餐）"));
        props.put("protein", Map.of("type", "number", "description", "实际摄入蛋白质（g/人/餐）"));
        props.put("fat", Map.of("type", "number", "description", "实际摄入脂肪（g/人/餐）"));
        props.put("carbs", Map.of("type", "number", "description", "实际摄入碳水化合物（g/人/餐）"));
        props.put("crowdType", Map.of("type", "string", "description", "人群类型（学龄前/小学低年级/小学高年级/初中生/高中生/教师）"));
        props.put("schoolType", Map.of("type", "string", "description", "学校类型（优质城市/普通城市/农村）"));
        props.put("mealPrice", Map.of("type", "number", "description", "餐费价格（元）"));
        props.put("dishCount", Map.of("type", "integer", "description", "当日菜品总数（可选）"));
        props.put("mealTypes", Map.of("type", "string", "description", "餐次列表，逗号分隔（可选，如：早餐,午餐,晚餐）"));
        props.put("dayCount", Map.of("type", "integer", "description", "评估天数（可选，默认1）"));
        props.put("calcium", Map.of("type", "number", "description", "钙实际摄入量(mg，可选)"));
        props.put("iron", Map.of("type", "number", "description", "铁实际摄入量(mg，可选)"));
        props.put("zinc", Map.of("type", "number", "description", "锌实际摄入量(mg，可选)"));
        props.put("vitA", Map.of("type", "number", "description", "维生素A实际摄入量(μg RE，可选)"));
        props.put("vitB1", Map.of("type", "number", "description", "维生素B1实际摄入量(mg，可选)"));
        props.put("vitB2", Map.of("type", "number", "description", "维生素B2实际摄入量(mg，可选)"));
        props.put("vitC", Map.of("type", "number", "description", "维生素C实际摄入量(mg，可选)"));
        return Map.of(
                "type", "object",
                "properties", props,
                "required", List.of("calorie", "protein", "fat", "carbs", "crowdType", "schoolType", "mealPrice"));
    }

    @Override
    public String execute(Map<String, Object> args) {
        ScoringRequest request = ScoringRequest.builder()
                .crowdType(getString(args, "crowdType"))
                .calorie(getDouble(args, "calorie"))
                .protein(getDouble(args, "protein"))
                .fat(getDouble(args, "fat"))
                .carbs(getDouble(args, "carbs"))
                .schoolType(getString(args, "schoolType"))
                .mealPrice(args.containsKey("mealPrice") ? getDouble(args, "mealPrice") : null)
                .dishCount(args.containsKey("dishCount") ? getInt(args, "dishCount") : 0)
                .mealTypes(args.containsKey("mealTypes")
                        ? List.of(getString(args, "mealTypes").split(","))
                        : List.of("午餐"))
                .dayCount(args.containsKey("dayCount") ? getInt(args, "dayCount") : 1)
                .calcium(optDouble(args, "calcium"))
                .iron(optDouble(args, "iron"))
                .zinc(optDouble(args, "zinc"))
                .vitA(optDouble(args, "vitA"))
                .vitB1(optDouble(args, "vitB1"))
                .vitB2(optDouble(args, "vitB2"))
                .vitC(optDouble(args, "vitC"))
                .build();

        ScoringResult result = scoringEngine.evaluate(request);
        String formatted = formatResult(result);
        log.info("RecipeEvaluateTool: crowdType={} → grade={} score={}",
                request.getCrowdType(), result.getGrade(), result.getTotalScore());
        return formatted;
    }

    private String formatResult(ScoringResult r) {
        if (r.getError() != null) {
            return "评分失败: " + r.getError();
        }
        var dims = r.getDimensions();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("总分: %.1f / 100  等级: %s\n", r.getTotalScore(), r.getGrade()));
        sb.append(String.format("目标分: %d（%s学校，宽松系数=%.2f）\n\n",
                r.getTargetScore(),
                "优质城市".equals(getString(Map.of(), "")) ? "优质城市" : "参考",
                r.getTolerance()));
        sb.append("维度得分:\n");
        formatDim(sb, "能量", dims.get("calorie"), 0.30);
        formatDim(sb, "蛋白质", dims.get("protein"), 0.25);
        formatDim(sb, "脂肪", dims.get("fat"), 0.15);
        formatDim(sb, "碳水化合物", dims.get("carbs"), 0.15);
        formatDim(sb, "食材多样性", dims.get("variety"), 0.15);

        // 微量元素 / 维生素明细（按 实际摄入 / 每日推荐 达标率）
        var micro = r.getMicroDimensions();
        if (micro != null && !micro.isEmpty()) {
            Map<String, String> labels = Map.of(
                    "calcium", "钙(mg)", "iron", "铁(mg)", "zinc", "锌(mg)",
                    "vitA", "维A(μg)", "vitB1", "维B1(mg)", "vitB2", "维B2(mg)", "vitC", "维C(mg)");
            sb.append("\n微量元素 / 维生素评分:\n");
            micro.forEach((k, dim) -> {
                String label = labels.getOrDefault(k, k);
                double rec = dim.rangeMax();
                double pct = rec > 0 ? Math.min(100, dim.actualValue() / rec * 100) : 0;
                sb.append(String.format("  %s: %.1f分 (实际%.1f / 推荐%.1f, 达标%.0f%%)\n",
                        label, dim.score(), dim.actualValue(), rec, pct));
            });
        }
        return sb.toString();
    }

    private void formatDim(StringBuilder sb, String name,
                           ScoringEngine.DimensionScore dim, double weight) {
        if (dim == null) {
            sb.append(String.format("  %s: 无数据\n", name));
            return;
        }
        sb.append(String.format("  %s: %.1f / %.0f (%s, 实际=%.1f, 区间[%.1f-%.1f])\n",
                name, dim.score(), weight * 100,
                dim.score() >= 80 ? "达标" : dim.score() >= 60 ? "接近" : "需改进",
                dim.actualValue(), dim.rangeMin(), dim.rangeMax()));
    }

    private double getDouble(Map<String, Object> m, String k) {
        Object v = m.get(k);
        if (v instanceof Number n) return n.doubleValue();
        if (v == null) return 0;
        try { return Double.parseDouble(v.toString()); } catch (NumberFormatException e) { return 0; }
    }

    private int getInt(Map<String, Object> m, String k) {
        Object v = m.get(k);
        if (v instanceof Number n) return n.intValue();
        if (v == null) return 0;
        try { return Integer.parseInt(v.toString()); } catch (NumberFormatException e) { return 0; }
    }

    private double optDouble(Map<String, Object> m, String k) {
        Object v = m.get(k);
        if (v instanceof Number n) return n.doubleValue();
        if (v == null) return 0;
        try { return Double.parseDouble(v.toString()); } catch (NumberFormatException e) { return 0; }
    }

    private String getString(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v != null ? v.toString() : "";
    }
}
