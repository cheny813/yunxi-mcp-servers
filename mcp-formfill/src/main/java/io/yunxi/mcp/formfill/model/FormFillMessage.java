package io.yunxi.mcp.formfill.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 表单填写消息
 * 
 * 用于前后端 WebSocket 通信
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormFillMessage {

    /**
     * 消息类型
     * - fill: 填写表单
     * - batch_fill: 批量填写
     * - get_structure: 获取表单结构
     * - result: 填写结果（前端反馈）
     */
    private String type;

    /**
     * 场景类型
     * - recipe: 食谱表单
     * - weekly_recipe: 周计划食谱
     * - safety: 食安表单
     * - budget: 经费表单
     * - generic: 通用表单
     */
    private String scene;

    /**
     * 表单数据
     */
    private Map<String, Object> formData;

    /**
     * 字段映射配置
     */
    private Map<String, String> fieldMapping;

    /**
     * 附加参数
     * - day: 星期几（用于周计划）
     * - meal: 餐次（用于周计划）
     * - targetPage: 目标页面URL
     * - requestId: 请求ID（用于跟踪）
     */
    private Map<String, Object> params;

    /**
     * 填写结果（前端反馈）
     */
    private FillResult result;

    /**
     * 填写结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FillResult {
        private boolean success;
        private String message;
        private int filledCount;
        private String requestId;
        private String error;
    }
}
