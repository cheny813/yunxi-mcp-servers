package io.yunxi.mcp.formfill.tool;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import io.yunxi.mcp.formfill.service.FormFillerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 表单填写 MCP 工具集
 * 
 * 提供智能表单填写能力，支持多种场景：
 * - 营养食谱表单（单日/周计划）
 * - 食品安全检测表单
 * - 经费管理表单
 * - 通用表单填写
 * 
 * 核心优势：
 * - AI 直接调用工具填写表单，无需前端解析
 * - 支持字段映射（中文 -> 英文 ID）
 * - 支持批量填写
 * - 支持场景自动识别
 */
@Slf4j
@Component
public class FormFillerTools {

    private final FormFillerService formFillerService;

    public FormFillerTools(FormFillerService formFillerService) {
        this.formFillerService = formFillerService;
    }

    /**
     * 填写营养食谱表单
     * 支持单日食谱和周计划食谱
     */
    public ToolHandler fillRecipeForm() {
        return new ToolHandler() {
            @Override
            public ToolDefinition getDefinition() {
                return ToolDefinition.builder()
                        .name("formfill_recipe")
                        .description("""
                                填写营养食谱表单。支持以下场景：
                                1. 单日食谱：填写一个食谱的信息
                                2. 周计划食谱：填写一周的食谱安排（周一到周日，每天三餐）
                                
                                字段说明：
                                - dishName: 菜谱名称
                                - category: 分类（main/side/soup/dessert/breakfast/lunch/dinner）
                                - targetAudience: 适用人群（students/teachers/all）
                                - ingredients: 主要配料
                                - calories: 热量(kcal)
                                - protein: 蛋白质(g)
                                - fat: 脂肪(g)
                                - carbs: 碳水(g)
                                - steps: 制作步骤
                                - nutritionNotes: 营养建议
                                - day: 星期几（用于周计划：monday/tuesday/wednesday/thursday/friday/saturday/sunday）
                                - meal: 餐次（breakfast/lunch/dinner）
                                """)
                        .inputSchema(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "recipeData", Map.of(
                                                "type", "object",
                                                "description", "食谱数据对象",
                                                "properties", Map.of(
                                                        "dishName", Map.of("type", "string", "description", "菜谱名称"),
                                                        "category", Map.of("type", "string", "enum",
                                                                List.of("main", "side", "soup", "dessert", "breakfast", "lunch", "dinner"),
                                                                "description", "分类"),
                                                        "targetAudience", Map.of("type", "string", "enum",
                                                                List.of("students", "teachers", "all"),
                                                                "description", "适用人群"),
                                                        "ingredients", Map.of("type", "string", "description", "主要配料"),
                                                        "calories", Map.of("type", "number", "description", "热量(kcal)"),
                                                        "protein", Map.of("type", "number", "description", "蛋白质(g)"),
                                                        "fat", Map.of("type", "number", "description", "脂肪(g)"),
                                                        "carbs", Map.of("type", "number", "description", "碳水(g)"),
                                                        "steps", Map.of("type", "string", "description", "制作步骤"),
                                                        "nutritionNotes", Map.of("type", "string", "description", "营养建议")
                                                )
                                        ),
                                        "day", Map.of(
                                                "type", "string",
                                                "enum", List.of("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"),
                                                "description", "星期几（用于周计划场景，可选）"
                                        ),
                                        "meal", Map.of(
                                                "type", "string",
                                                "enum", List.of("breakfast", "breakfast_snack", "lunch", "lunch_snack", "dinner", "dinner_snack"),
                                                "description", "餐次（三餐三点：breakfast早餐/breakfast_snack早点/lunch午餐/lunch_snack午点/dinner晚餐/dinner_snack晚点）"
                                        ),
                                        "targetPage", Map.of(
                                                "type", "string",
                                                "description", "目标页面URL（可选，默认为当前页面）"
                                        )
                                ),
                                "required", List.of("recipeData")
                        ))
                        .build();
            }

            @Override
            public ToolResult execute(Map<String, Object> arguments) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> recipeData = (Map<String, Object>) arguments.get("recipeData");
                    String day = (String) arguments.get("day");
                    String meal = (String) arguments.get("meal");
                    String targetPage = (String) arguments.get("targetPage");

                    log.info("填写食谱表单: recipeData={}, day={}, meal={}", recipeData, day, meal);

                    String result = formFillerService.fillRecipeForm(recipeData, day, meal, targetPage);
                    return ToolResult.text(result);
                } catch (Exception e) {
                    log.error("填写食谱表单失败", e);
                    return ToolResult.error("填写失败: " + e.getMessage());
                }
            }
        };
    }

    /**
     * 批量填写周计划食谱
     */
    public ToolHandler fillWeeklyRecipePlan() {
        return new ToolHandler() {
            @Override
            public ToolDefinition getDefinition() {
                return ToolDefinition.builder()
                        .name("formfill_weekly_recipe")
                        .description("""
                                批量填写一周的营养食谱计划。
                                
                                输入格式示例：
                                {
                                  "monday": {
                                    "breakfast": { "dishName": "牛奶燕麦", "calories": 350, ... },
                                    "lunch": { "dishName": "红烧肉", "calories": 500, ... },
                                    "dinner": { "dishName": "清蒸鱼", "calories": 400, ... }
                                  },
                                  "tuesday": { ... },
                                  ...
                                }
                                """)
                        .inputSchema(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "weeklyPlan", Map.of(
                                                "type", "object",
                                                "description", "一周食谱计划（按天和餐次组织）"
                                        ),
                                        "targetPage", Map.of(
                                                "type", "string",
                                                "description", "目标页面URL（可选）"
                                        )
                                ),
                                "required", List.of("weeklyPlan")
                        ))
                        .build();
            }

            @Override
            public ToolResult execute(Map<String, Object> arguments) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> weeklyPlan = (Map<String, Object>) arguments.get("weeklyPlan");
                    String targetPage = (String) arguments.get("targetPage");

                    log.info("批量填写周计划食谱: 共{}天", weeklyPlan.size());

                    String result = formFillerService.fillWeeklyRecipePlan(weeklyPlan, targetPage);
                    return ToolResult.text(result);
                } catch (Exception e) {
                    log.error("批量填写周计划食谱失败", e);
                    return ToolResult.error("填写失败: " + e.getMessage());
                }
            }
        };
    }

    /**
     * 填写食品安全检测表单
     */
    public ToolHandler fillSafetyForm() {
        return new ToolHandler() {
            @Override
            public ToolDefinition getDefinition() {
                return ToolDefinition.builder()
                        .name("formfill_safety")
                        .description("""
                                填写食品安全检测表单。
                                
                                字段说明：
                                - sampleId: 样品编号
                                - sampleName: 样品名称
                                - testDate: 检测日期
                                - testItem: 检测项目
                                - testResult: 检测结果（pass/fail/pending）
                                - testValue: 检测值
                                - standardValue: 标准值
                                - unit: 单位
                                - inspector: 检测人
                                - notes: 备注
                                """)
                        .inputSchema(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "safetyData", Map.of(
                                                "type", "object",
                                                "description", "食品安全检测数据"
                                        ),
                                        "targetPage", Map.of(
                                                "type", "string",
                                                "description", "目标页面URL（可选）"
                                        )
                                ),
                                "required", List.of("safetyData")
                        ))
                        .build();
            }

            @Override
            public ToolResult execute(Map<String, Object> arguments) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> safetyData = (Map<String, Object>) arguments.get("safetyData");
                    String targetPage = (String) arguments.get("targetPage");

                    log.info("填写食品安全表单: {}", safetyData);

                    String result = formFillerService.fillSafetyForm(safetyData, targetPage);
                    return ToolResult.text(result);
                } catch (Exception e) {
                    log.error("填写食品安全表单失败", e);
                    return ToolResult.error("填写失败: " + e.getMessage());
                }
            }
        };
    }

    /**
     * 填写经费管理表单
     */
    public ToolHandler fillBudgetForm() {
        return new ToolHandler() {
            @Override
            public ToolDefinition getDefinition() {
                return ToolDefinition.builder()
                        .name("formfill_budget")
                        .description("""
                                填写经费管理表单。
                                
                                字段说明：
                                - expenseId: 项目编号
                                - expenseName: 项目名称
                                - category: 费用类别（ingredients/equipment/labor/other）
                                - budgetAmount: 预算金额
                                - actualAmount: 实际支出
                                - paymentStatus: 支付状态（pending/partial/paid）
                                - paymentDate: 支付日期
                                - supplier: 供应商
                                - notes: 备注
                                """)
                        .inputSchema(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "budgetData", Map.of(
                                                "type", "object",
                                                "description", "经费数据"
                                        ),
                                        "targetPage", Map.of(
                                                "type", "string",
                                                "description", "目标页面URL（可选）"
                                        )
                                ),
                                "required", List.of("budgetData")
                        ))
                        .build();
            }

            @Override
            public ToolResult execute(Map<String, Object> arguments) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> budgetData = (Map<String, Object>) arguments.get("budgetData");
                    String targetPage = (String) arguments.get("targetPage");

                    log.info("填写经费表单: {}", budgetData);

                    String result = formFillerService.fillBudgetForm(budgetData, targetPage);
                    return ToolResult.text(result);
                } catch (Exception e) {
                    log.error("填写经费表单失败", e);
                    return ToolResult.error("填写失败: " + e.getMessage());
                }
            }
        };
    }

    /**
     * 通用表单填写工具
     */
    public ToolHandler fillGenericForm() {
        return new ToolHandler() {
            @Override
            public ToolDefinition getDefinition() {
                return ToolDefinition.builder()
                        .name("formfill_generic")
                        .description("""
                                通用表单填写工具。可以填写任意表单，需要提供字段映射配置。
                                
                                使用场景：
                                - 没有专用工具的表单
                                - 自定义表单结构
                                - 快速原型验证
                                """)
                        .inputSchema(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "formData", Map.of(
                                                "type", "object",
                                                "description", "表单数据（键值对）"
                                        ),
                                        "fieldMapping", Map.of(
                                                "type", "object",
                                                "description", "字段映射配置（数据字段名 -> 表单元素ID）"
                                        ),
                                        "targetPage", Map.of(
                                                "type", "string",
                                                "description", "目标页面URL（可选）"
                                        )
                                ),
                                "required", List.of("formData")
                        ))
                        .build();
            }

            @Override
            public ToolResult execute(Map<String, Object> arguments) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> formData = (Map<String, Object>) arguments.get("formData");
                    @SuppressWarnings("unchecked")
                    Map<String, String> fieldMapping = (Map<String, String>) arguments.get("fieldMapping");
                    String targetPage = (String) arguments.get("targetPage");

                    log.info("填写通用表单: formData={}, fieldMapping={}", formData, fieldMapping);

                    String result = formFillerService.fillGenericForm(formData, fieldMapping, targetPage);
                    return ToolResult.text(result);
                } catch (Exception e) {
                    log.error("填写通用表单失败", e);
                    return ToolResult.error("填写失败: " + e.getMessage());
                }
            }
        };
    }

    /**
     * 获取表单结构
     */
    public ToolHandler getFormStructure() {
        return new ToolHandler() {
            @Override
            public ToolDefinition getDefinition() {
                return ToolDefinition.builder()
                        .name("formfill_get_structure")
                        .description("""
                                获取目标页面的表单结构信息。
                                返回表单字段列表、字段类型和验证规则，帮助 AI 了解如何填写表单。
                                """)
                        .inputSchema(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "scene", Map.of(
                                                "type", "string",
                                                "enum", List.of("recipe", "weekly_recipe", "safety", "budget", "auto"),
                                                "description", "场景类型（auto=自动检测）"
                                        ),
                                        "targetPage", Map.of(
                                                "type", "string",
                                                "description", "目标页面URL（可选）"
                                        )
                                ),
                                "required", List.of()
                        ))
                        .build();
            }

            @Override
            public ToolResult execute(Map<String, Object> arguments) {
                try {
                    String scene = (String) arguments.getOrDefault("scene", "auto");
                    String targetPage = (String) arguments.get("targetPage");

                    log.info("获取表单结构: scene={}, targetPage={}", scene, targetPage);

                    String result = formFillerService.getFormStructure(scene, targetPage);
                    return ToolResult.text(result);
                } catch (Exception e) {
                    log.error("获取表单结构失败", e);
                    return ToolResult.error("获取失败: " + e.getMessage());
                }
            }
        };
    }
}
