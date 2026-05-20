package io.yunxi.mcp.common.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.yunxi.mcp.common.constants.McpConstants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP 协议响应
 * <p>
 * 基于 JSON-RPC 2.0 规范
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpResponse {

    /**
     * JSON-RPC 版本，固定为 "2.0"
     */
    @Builder.Default
    private String jsonrpc = McpConstants.JSONRPC_VERSION;

    /**
     * 请求 ID，与请求匹配
     */
    private Object id;

    /**
     * 响应结果（成功时）
     */
    private Object result;

    /**
     * 错误信息（失败时）
     */
    private McpError error;

    /**
     * 创建成功响应
     */
    public static McpResponse success(Object id, Object result) {
        return McpResponse.builder()
                .id(id)
                .result(result)
                .build();
    }

    /**
     * 创建错误响应
     */
    public static McpResponse error(Object id, int code, String message) {
        return error(id, code, message, null, null);
    }

    /**
     * 创建错误响应（带详细信息）
     */
    public static McpResponse error(Object id, int code, String message, Object data) {
        return error(id, code, message, data, null);
    }

    /**
     * 创建错误响应（带跟踪 ID）
     */
    public static McpResponse error(Object id, int code, String message, String traceId) {
        return error(id, code, message, null, traceId);
    }

    /**
     * 创建错误响应（带详细信息与跟踪 ID）
     */
    public static McpResponse error(Object id, int code, String message, Object data, String traceId) {
        return McpResponse.builder()
                .id(id)
                .error(McpError.builder()
                        .code(code)
                        .message(message)
                        .data(data)
                        .traceId(traceId)
                        .build())
                .build();
    }
}
