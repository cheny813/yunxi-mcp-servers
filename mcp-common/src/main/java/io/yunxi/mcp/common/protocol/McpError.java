package io.yunxi.mcp.common.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.yunxi.mcp.common.constants.McpErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP 错误信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpError {

    /**
     * 错误代码
     */
    private int code;

    /**
     * 错误消息
     */
    private String message;

    /**
     * 错误详细信息
     */
    private Object data;

    /**
     * 跟踪 ID，用于跨服务链路定位错误
     */
    private String traceId;

    /**
     * 标准错误代码
     */
    public static final int PARSE_ERROR = McpErrorCode.PARSE_ERROR;
    public static final int INVALID_REQUEST = McpErrorCode.INVALID_REQUEST;
    public static final int METHOD_NOT_FOUND = McpErrorCode.METHOD_NOT_FOUND;
    public static final int INVALID_PARAMS = McpErrorCode.INVALID_PARAMS;
    public static final int INTERNAL_ERROR = McpErrorCode.INTERNAL_ERROR;

    /**
     * 服务器错误代码范围：-32000 到 -32099
     */
    public static final int SERVER_ERROR_START = McpErrorCode.SERVER_ERROR_START;
    public static final int SERVER_ERROR_END = McpErrorCode.SERVER_ERROR_END;
}
