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
     * 场景标识
     */
    private String scene;

    /**
     * 表单数据，键为表单元素 ID，值为待填写内容。
     * 由上游 LLM 通过 formfill_get_structure 了解表单结构后直接产出。
     */
    private Map<String, Object> formData;

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
