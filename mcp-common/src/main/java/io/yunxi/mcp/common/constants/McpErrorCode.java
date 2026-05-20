package io.yunxi.mcp.common.constants;

/**
 * MCP 错误代码定义
 * <p>
 * 统一管理 MCP JSON-RPC 错误代码，便于客户端和服务端共享。
 * </p>
 *
 * @since 1.0.0
 */
public final class McpErrorCode {

    /** JSON-RPC 解析错误 */
    public static final int PARSE_ERROR = -32700;

    /** JSON-RPC 无效请求 */
    public static final int INVALID_REQUEST = -32600;

    /** JSON-RPC 方法未找到 */
    public static final int METHOD_NOT_FOUND = -32601;

    /** JSON-RPC 参数无效 */
    public static final int INVALID_PARAMS = -32602;

    /** JSON-RPC 内部错误 */
    public static final int INTERNAL_ERROR = -32603;

    /** 服务器错误代码范围开始 */
    public static final int SERVER_ERROR_START = -32000;

    /** 服务器错误代码范围结束 */
    public static final int SERVER_ERROR_END = -32099;

    private McpErrorCode() {
        throw new UnsupportedOperationException("MCP error code constants should not be instantiated");
    }

    /**
     * 检查错误码是否为服务器错误范围。
     *
     * @param code 错误代码
     * @return true 表示属于服务器错误范围
     */
    public static boolean isServerError(int code) {
        return code <= SERVER_ERROR_START && code >= SERVER_ERROR_END;
    }
}
