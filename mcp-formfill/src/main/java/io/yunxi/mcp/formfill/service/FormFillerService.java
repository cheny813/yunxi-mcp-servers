package io.yunxi.mcp.formfill.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.yunxi.mcp.formfill.model.FormFillMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 表单填写服务
 *
 * 提供表单填写的核心逻辑，包括：
 * - 字段映射（中文 -> 英文 ID）
 * - 数据验证
 * - 表单结构分析
 * - 浏览器自动化集成
 *
 * @author yunxi-mcp-servers
 */
@Slf4j
@Service
public class FormFillerService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SimpMessagingTemplate messagingTemplate;

    public FormFillerService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // ==================== 场景配置 ====================

    /**
     * 食谱表单字段映射
     * 中文字段名到英文字段名的映射关系
     */
    private static final Map<String, String> RECIPE_FIELD_MAPPING = Map.ofEntries(
            Map.entry("菜谱名称", "dishName"),
            Map.entry("名称", "dishName"),
            Map.entry("分类", "category"),
            Map.entry("适用人群", "targetAudience"),
            Map.entry("主要配料", "ingredients"),
            Map.entry("食材", "ingredients"),
            Map.entry("热量", "calories"),
            Map.entry("蛋白质", "protein"),
            Map.entry("脂肪", "fat"),
            Map.entry("碳水", "carbs"),
            Map.entry("碳水化合物", "carbs"),
            Map.entry("制作步骤", "steps"),
            Map.entry("步骤", "steps"),
            Map.entry("营养建议", "nutritionNotes"),
            Map.entry("营养特点", "nutritionNotes"));

    /**
     * 食安表单字段映射
     */
    private static final Map<String, String> SAFETY_FIELD_MAPPING = Map.ofEntries(
            Map.entry("样品编号", "sampleId"),
            Map.entry("样品名称", "sampleName"),
            Map.entry("检测日期", "testDate"),
            Map.entry("检测项目", "testItem"),
            Map.entry("检测结果", "testResult"),
            Map.entry("检测值", "testValue"),
            Map.entry("标准值", "standardValue"),
            Map.entry("单位", "unit"),
            Map.entry("检测人", "inspector"),
            Map.entry("备注", "notes"));

    /**
     * 经费表单字段映射
     */
    private static final Map<String, String> BUDGET_FIELD_MAPPING = Map.ofEntries(
            Map.entry("项目编号", "expenseId"),
            Map.entry("项目名称", "expenseName"),
            Map.entry("费用类别", "category"),
            Map.entry("预算金额", "budgetAmount"),
            Map.entry("实际支出", "actualAmount"),
            Map.entry("支付状态", "paymentStatus"),
            Map.entry("支付日期", "paymentDate"),
            Map.entry("供应商", "supplier"),
            Map.entry("备注", "notes"));

    /**
     * 分类映射（中文 -> 英文）
     */
    private static final Map<String, String> CATEGORY_MAPPING = Map.ofEntries(
            Map.entry("主食", "main"),
            Map.entry("主菜", "main"),
            Map.entry("副菜", "side"),
            Map.entry("配菜", "side"),
            Map.entry("汤品", "soup"),
            Map.entry("甜品", "dessert"),
            Map.entry("早餐", "breakfast"),
            Map.entry("午餐", "lunch"),
            Map.entry("晚餐", "dinner"));

    /**
     * 适用人群映射
     */
    private static final Map<String, String> AUDIENCE_MAPPING = Map.ofEntries(
            Map.entry("学生", "students"),
            Map.entry("教师", "teachers"),
            Map.entry("全体", "all"));

    // ==================== 表单填写方法 ====================

    /**
     * 填写食谱表单
     */
    public String fillRecipeForm(Map<String, Object> recipeData, String day, String meal, String targetPage) {
        try {
            // 生成请求ID
            String requestId = UUID.randomUUID().toString();

            // 1. 映射字段
            Map<String, Object> mappedData = mapFields(recipeData, RECIPE_FIELD_MAPPING);

            // 2. 特殊处理：分类和人群映射
            mapCategory(mappedData);
            mapAudience(mappedData);

            // 3. 构建消息
            Map<String, Object> params = new HashMap<>();
            if (day != null)
                params.put("day", day);
            if (meal != null)
                params.put("meal", meal);
            if (targetPage != null)
                params.put("targetPage", targetPage);
            params.put("requestId", requestId);

            // 4. 根据是否有 day/meal 参数，决定填写方式
            FormFillMessage message;
            if (day != null && meal != null) {
                // 周计划场景
                message = FormFillMessage.builder()
                        .type("fill")
                        .scene("weekly_recipe")
                        .formData(mappedData)
                        .params(params)
                        .build();
            } else {
                // 单日食谱场景
                message = FormFillMessage.builder()
                        .type("fill")
                        .scene("recipe")
                        .formData(mappedData)
                        .params(params)
                        .build();
            }

            // 5. 发送到前端并等待反馈
            FormFillMessage.FillResult result = executeFormFillWithFeedback(message, 30, 3);
            
            String dishName = (String) mappedData.get("dishName");
            StringBuilder resultMessage = new StringBuilder();
            
            if (result != null) {
                if (result.isSuccess()) {
                    resultMessage.append("表单填写完成");
                    if (result.getFilledCount() > 0) {
                        resultMessage.append("，成功填写 ").append(result.getFilledCount()).append(" 个字段");
                    }
                } else {
                    resultMessage.append("表单填写失败");
                    if (result.getError() != null && !result.getError().isEmpty()) {
                        resultMessage.append("：").append(result.getError());
                    }
                }
            } else {
                resultMessage.append("已发送填写指令，等待前端处理");
            }
            
            if (day != null && meal != null) {
                return String.format("%s %s 的食谱 %s（请求ID：%s）",
                        getDayLabel(day), getMealLabel(meal), dishName, requestId);
            } else {
                return String.format("%s：%s（请求ID：%s）", dishName, resultMessage, requestId);
            }
        } catch (Exception e) {
            log.error("填写食谱表单失败", e);
            return "填写失败: " + e.getMessage();
        }
    }

    /**
     * 批量填写周计划食谱
     */
    public String fillWeeklyRecipePlan(Map<String, Object> weeklyPlan, String targetPage) {
        try {
            int filledCount = 0;
            StringBuilder result = new StringBuilder("周计划食谱填写结果：\n");

            // 遍历每天
            for (String day : new String[] { "monday", "tuesday", "wednesday", "thursday", "friday", "saturday",
                    "sunday" }) {
                @SuppressWarnings("unchecked")
                Map<String, Object> dayPlan = (Map<String, Object>) weeklyPlan.get(day);
                if (dayPlan == null)
                    continue;

                // 遍历每餐
                for (String meal : new String[] { "breakfast", "lunch", "dinner" }) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> mealData = (Map<String, Object>) dayPlan.get(meal);
                    if (mealData == null)
                        continue;

                    // 填写该餐次的食谱
                    String fillResult = fillRecipeForm(mealData, day, meal, targetPage);
                    if (fillResult.contains("成功")) {
                        filledCount++;
                        result.append(String.format("✅ %s %s 已填写\n", getDayLabel(day), getMealLabel(meal)));
                    }
                }
            }

            result.append(String.format("\n共填写 %d 个食谱", filledCount));
            return result.toString();
        } catch (Exception e) {
            log.error("批量填写周计划食谱失败", e);
            return "填写失败: " + e.getMessage();
        }
    }

    /**
     * 填写食品安全表单
     */
    public String fillSafetyForm(Map<String, Object> safetyData, String targetPage) {
        try {
            String requestId = UUID.randomUUID().toString();
            Map<String, Object> mappedData = mapFields(safetyData, SAFETY_FIELD_MAPPING);

            Map<String, Object> params = new HashMap<>();
            if (targetPage != null)
                params.put("targetPage", targetPage);
            params.put("requestId", requestId);

            FormFillMessage message = FormFillMessage.builder()
                    .type("fill")
                    .scene("safety")
                    .formData(mappedData)
                    .params(params)
                    .build();

            sendMessage(message);

            return String.format("已发送填写指令：填写食安表单（请求ID：%s）", requestId);
        } catch (Exception e) {
            log.error("填写食品安全表单失败", e);
            return "填写失败: " + e.getMessage();
        }
    }

    /**
     * 填写经费管理表单
     */
    public String fillBudgetForm(Map<String, Object> budgetData, String targetPage) {
        try {
            String requestId = UUID.randomUUID().toString();
            Map<String, Object> mappedData = mapFields(budgetData, BUDGET_FIELD_MAPPING);

            Map<String, Object> params = new HashMap<>();
            if (targetPage != null)
                params.put("targetPage", targetPage);
            params.put("requestId", requestId);

            FormFillMessage message = FormFillMessage.builder()
                    .type("fill")
                    .scene("budget")
                    .formData(mappedData)
                    .params(params)
                    .build();

            sendMessage(message);

            return String.format("已发送填写指令：填写经费表单（请求ID：%s）", requestId);
        } catch (Exception e) {
            log.error("填写经费表单失败", e);
            return "填写失败: " + e.getMessage();
        }
    }

    /**
     * 填写通用表单
     */
    public String fillGenericForm(Map<String, Object> formData, Map<String, String> fieldMapping, String targetPage) {
        try {
            String requestId = UUID.randomUUID().toString();
            Map<String, Object> mappedData = fieldMapping != null ? mapFields(formData, fieldMapping) : formData;

            Map<String, Object> params = new HashMap<>();
            if (targetPage != null)
                params.put("targetPage", targetPage);
            params.put("requestId", requestId);

            FormFillMessage message = FormFillMessage.builder()
                    .type("fill")
                    .scene("generic")
                    .formData(mappedData)
                    .fieldMapping(fieldMapping)
                    .params(params)
                    .build();

            sendMessage(message);

            return String.format("已发送填写指令：填写通用表单（请求ID：%s）", requestId);
        } catch (Exception e) {
            log.error("填写通用表单失败", e);
            return "填写失败: " + e.getMessage();
        }
    }

    /**
     * 获取表单结构
     */
    public String getFormStructure(String scene, String targetPage) {
        try {
            Map<String, Object> structure = new HashMap<>();
            structure.put("scene", scene);

            switch (scene) {
                case "recipe":
                    structure.put("fields", RECIPE_FIELD_MAPPING);
                    structure.put("description", "营养食谱表单，包含菜谱名称、分类、营养成分等字段");
                    break;
                case "weekly_recipe":
                    structure.put("fields", RECIPE_FIELD_MAPPING);
                    structure.put("days", new String[] { "monday", "tuesday", "wednesday", "thursday", "friday",
                            "saturday", "sunday" });
                    structure.put("meals", new String[] { "breakfast", "lunch", "dinner" });
                    structure.put("description", "周计划食谱表单，需要指定 day 和 meal 参数");
                    break;
                case "safety":
                    structure.put("fields", SAFETY_FIELD_MAPPING);
                    structure.put("description", "食品安全检测表单");
                    break;
                case "budget":
                    structure.put("fields", BUDGET_FIELD_MAPPING);
                    structure.put("description", "经费管理表单");
                    break;
                default:
                    structure.put("availableScenes", new String[] { "recipe", "weekly_recipe", "safety", "budget" });
                    structure.put("description", "请指定具体的场景类型");
            }

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(structure);
        } catch (Exception e) {
            log.error("获取表单结构失败", e);
            return "获取失败: " + e.getMessage();
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 字段映射
     */
    private Map<String, Object> mapFields(Map<String, Object> data, Map<String, String> mapping) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // 映射字段名
            String mappedKey = mapping.getOrDefault(key, key);
            result.put(mappedKey, value);
        }
        return result;
    }

    /**
     * 映射分类字段
     */
    private void mapCategory(Map<String, Object> data) {
        Object category = data.get("category");
        if (category != null && category instanceof String) {
            String mapped = CATEGORY_MAPPING.getOrDefault(category, (String) category);
            data.put("category", mapped);
        }
    }

    /**
     * 映射适用人群字段
     */
    private void mapAudience(Map<String, Object> data) {
        Object audience = data.get("targetAudience");
        if (audience != null && audience instanceof String) {
            String mapped = AUDIENCE_MAPPING.getOrDefault(audience, (String) audience);
            data.put("targetAudience", mapped);
        }
    }

    /**
     * 填写周计划中的单个食谱槽位
     */
    private String fillWeeklyRecipeSlot(Map<String, Object> data, String day, String meal, String targetPage) {
        // 生成表单元素 ID
        // 假设页面结构为：id="recipe-{day}-{meal}-{field}"
        // 例如：id="recipe-monday-breakfast-dishName"

        // TODO: 实际的表单填写逻辑
        // 这里可以调用 Playwright 或其他浏览器自动化工具
        // 或者通过 WebSocket 推送到前端

        return String.format("成功填写 %s %s 的食谱: %s", getDayLabel(day), getMealLabel(meal), data.get("dishName"));
    }

    /**
     * 填写单个食谱
     */
    private String fillSingleRecipe(Map<String, Object> data, String targetPage) {
        // TODO: 实际的表单填写逻辑

        return String.format("成功填写食谱: %s", data.get("dishName"));
    }

    /**
     * 获取星期的中文标签
     */
    private String getDayLabel(String day) {
        Map<String, String> labels = Map.of(
                "monday", "周一",
                "tuesday", "周二",
                "wednesday", "周三",
                "thursday", "周四",
                "friday", "周五",
                "saturday", "周六",
                "sunday", "周日");
        return labels.getOrDefault(day, day);
    }

    /**
     * 获取餐次的中文标签
     */
    private String getMealLabel(String meal) {
        Map<String, String> labels = Map.of(
                "breakfast", "早餐",
                "lunch", "午餐",
                "dinner", "晚餐");
        return labels.getOrDefault(meal, meal);
    }

    /**
     * 发送消息到前端（通过 WebSocket）
     */
    private void sendMessage(FormFillMessage message) {
        try {
            messagingTemplate.convertAndSend("/topic/formfill", message);
            log.info("发送表单填写消息: type={}, scene={}, requestId={}",
                    message.getType(), message.getScene(),
                    message.getParams() != null ? message.getParams().get("requestId") : null);
        } catch (Exception e) {
            log.error("发送消息失败", e);
        }
    }

    /**
     * 等待前端反馈结果（带超时和重试机制）
     * 
     * @param requestId 请求ID，用于匹配反馈结果
     * @param timeoutSeconds 超时时间（秒）
     * @param maxRetries 最大重试次数
     * @return 前端反馈的填写结果，超时返回null
     */
    private FormFillMessage.FillResult waitForFeedback(String requestId, int timeoutSeconds, int maxRetries) {
        if (timeoutSeconds <= 0) {
            timeoutSeconds = 30; // 默认30秒超时
        }
        if (maxRetries <= 0) {
            maxRetries = 3; // 默认重试3次
        }

        int retryCount = 0;
        long timeoutMs = timeoutSeconds * 1000L;
        long startTime = System.currentTimeMillis();

        while (retryCount <= maxRetries) {
            try {
                // 使用消息队列或共享存储等待反馈结果
                // 这里简化实现：在实际项目中应该使用Redis、数据库或内存队列
                FormFillMessage.FillResult result = checkFeedbackFromStorage(requestId);
                
                if (result != null) {
                    log.info("成功收到前端反馈: requestId={}, success={}, message={}", 
                            requestId, result.isSuccess(), result.getMessage());
                    return result;
                }

                // 检查是否超时
                if (System.currentTimeMillis() - startTime >= timeoutMs) {
                    log.warn("等待前端反馈超时: requestId={}, timeout={}秒", requestId, timeoutSeconds);
                    break;
                }

                // 等待一段时间后重试
                Thread.sleep(1000); // 1秒重试间隔
                retryCount++;
                
                if (retryCount > 0) {
                    log.debug("正在等待前端反馈: requestId={}, 重试次数={}", requestId, retryCount);
                }

            } catch (InterruptedException e) {
                log.warn("等待前端反馈被中断: requestId={}", requestId, e);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("等待前端反馈发生异常: requestId={}", requestId, e);
                retryCount++;
                
                if (retryCount > maxRetries) {
                    log.error("达到最大重试次数，停止等待: requestId={}", requestId);
                    break;
                }
                
                try {
                    Thread.sleep(2000); // 异常后等待时间稍长
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        log.warn("前端反馈等待结束，未收到结果: requestId={}", requestId);
        return null;
    }

    /**
     * 检查存储中的反馈结果（简化实现）
     * 
     * @param requestId 请求ID
     * @return 反馈结果，不存在返回null
     */
    private FormFillMessage.FillResult checkFeedbackFromStorage(String requestId) {
        // 在实际项目中，这里应该从Redis、数据库或内存缓存中获取反馈结果
        // 简化实现：返回null表示尚未收到反馈
        
        // 注意：在实际项目中需要实现WebSocket消息接收器，将前端反馈存储到共享存储中
        // FormFillWebSocketController.handleFormFillFeedback方法应该将结果存储起来
        
        return null;
    }

    /**
     * 完整的表单填写流程（包含等待反馈）
     */
    private FormFillMessage.FillResult executeFormFillWithFeedback(FormFillMessage message, int timeoutSeconds, int maxRetries) {
        String requestId = message.getParams() != null ? 
                (String) message.getParams().get("requestId") : null;
        
        if (requestId == null || requestId.trim().isEmpty()) {
            log.warn("请求ID为空，跳过等待反馈");
            return null;
        }

        // 发送表单填写指令
        sendMessage(message);

        // 等待前端反馈
        return waitForFeedback(requestId, timeoutSeconds, maxRetries);
    }
}
