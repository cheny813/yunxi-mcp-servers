package io.yunxi.mcp.common.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.yunxi.mcp.common.constants.McpConstants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * MCP 协议请求
 * <p>
 * 基于 JSON-RPC 2.0 规范
 * </p>
 *
 * @see <a href="https://spec.modelcontextprotocol.io/">MCP Specification</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpRequest {

    /**
     * JSON-RPC 版本，固定为 "2.0"
     */
    @Builder.Default
    private String jsonrpc = McpConstants.JSONRPC_VERSION;

    /**
     * 请求 ID，用于匹配响应
     */
    private Object id;

    /**
     * 方法名称
     */
    private String method;

    /**
     * 方法参数
     */
    private Map<String, Object> params;

    /**
     * 可选的请求跟踪 ID，用于跨服务链路跟踪和错误诊断
     */
    private String traceId;
}
