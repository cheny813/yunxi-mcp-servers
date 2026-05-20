package io.yunxi.mcp.common.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.yunxi.mcp.common.constants.McpConstants;
import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolParameter;
import io.yunxi.mcp.common.model.ToolResult;
import io.yunxi.mcp.common.tracing.TraceContext;
import io.yunxi.mcp.common.validation.ParameterValidator;
import io.yunxi.mcp.common.protocol.McpError;
import io.yunxi.mcp.common.protocol.McpRequest;
import io.yunxi.mcp.common.protocol.McpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP 端点抽象基类
 *
 * 提供 MCP 请求分发、工具注册与响应构建的通用实现，供 HTTP/SSE 等不同通信方式复用。
 */
public abstract class AbstractMcpEndpoint {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final ObjectMapper objectMapper = new ObjectMapper();

    protected final Map<String, ToolHandler> toolHandlers = new ConcurrentHashMap<>();

    protected final Map<String, Object> serverInfo = new LinkedHashMap<>();

    protected final Map<String, Object> capabilities = new LinkedHashMap<>();

    protected AbstractMcpEndpoint(String name, String version) {
        serverInfo.put("name", name);
        serverInfo.put("version", version);
        capabilities.put("tools", Map.of("listChanged", false));
    }

    public void registerTool(ToolHandler handler) {
        toolHandlers.put(handler.getName(), handler);
        log.info("注册工具: {}", handler.getName());
    }

    public Map<String, Object> getServerInfo() {
        return serverInfo;
    }

    public List<ToolDefinition> getTools() {
        return toolHandlers.values().stream().map(ToolHandler::getDefinition).toList();
    }

    protected String serializeResponse(McpResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.error("响应序列化失败", e);
            return "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32603,\"message\":\"Internal error\"}}";
        }
    }

    protected McpRequest parseRequest(String requestJson) {
        try {
            return objectMapper.readValue(requestJson, McpRequest.class);
        } catch (Exception e) {
            log.error("请求解析失败: {}", e.getMessage());
            return null;
        }
    }

    protected String handleRawRequest(String requestJson) {
        McpRequest request = parseRequest(requestJson);
        if (request == null) {
            return serializeResponse(McpResponse.error(null, McpError.PARSE_ERROR, "Parse error"));
        }
        return serializeResponse(handleRequest(request));
    }

    protected McpResponse handleRequest(McpRequest request) {
        if (request == null) {
            return McpResponse.error(null, McpError.INVALID_REQUEST, "Invalid request");
        }

        String method = request.getMethod();
        Object id = request.getId();

        // 从请求中获取或生成 TraceId，并设置到 MDC
        String traceId = TraceContext.setTraceIdOrGenerate(request.getTraceId());

        try {
            log.debug("收到请求: method={}, id={}, traceId={}", method, id, traceId);

            McpResponse response = switch (method) {
                case "initialize" -> handleInitialize(request);
                case "tools/list" -> handleToolsList(request);
                case "tools/call" -> handleToolsCall(request);
                case "ping" -> McpResponse.success(id, Map.of());
                default -> McpResponse.error(id, McpError.METHOD_NOT_FOUND, "Method not found: " + method, traceId);
            };

            // 在响应中返回 TraceId
            if (response.getError() != null && response.getError().getTraceId() == null) {
                response.getError().setTraceId(traceId);
            }

            return response;
        } finally {
            // 清理 MDC 中的 TraceId
            TraceContext.clear();
        }
    }

    protected McpResponse handleInitialize(McpRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("protocolVersion", McpConstants.PROTOCOL_VERSION);
        result.put("capabilities", capabilities);
        result.put("serverInfo", serverInfo);
        return McpResponse.success(request.getId(), result);
    }

    protected McpResponse handleToolsList(McpRequest request) {
        return McpResponse.success(request.getId(), Map.of("tools", getTools()));
    }

    @SuppressWarnings("unchecked")
    protected McpResponse handleToolsCall(McpRequest request) {
        Map<String, Object> params = request.getParams();
        String traceId = request.getTraceId();
        if (params == null) {
            return McpResponse.error(request.getId(), McpError.INVALID_PARAMS, "Missing params", traceId);
        }

        String toolName = (String) params.get("name");
        if (toolName == null) {
            return McpResponse.error(request.getId(), McpError.INVALID_PARAMS, "Missing tool name", traceId);
        }

        ToolHandler handler = toolHandlers.get(toolName);
        if (handler == null) {
            return McpResponse.error(request.getId(), McpError.INVALID_PARAMS, "Unknown tool: " + toolName, traceId);
        }

        Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");
        if (arguments == null) {
            arguments = Map.of();
        }

        // 参数校验
        ToolDefinition definition = handler.getDefinition();
        if (definition != null && definition.getParameters() != null) {
            ParameterValidator.ValidationResult validationResult = validateParameters(
                    definition.getParameters(), arguments);
            if (!validationResult.isValid()) {
                return McpResponse.error(request.getId(), McpError.INVALID_PARAMS,
                        "参数校验失败: " + validationResult.getErrorMessage(), traceId);
            }
        }

        long startTime = System.currentTimeMillis();
        boolean success = false;

        try {
            ToolResult result = handler.execute(arguments);
            success = !result.isError();
            return McpResponse.success(request.getId(), Map.of(
                    "content", result.getContent(),
                    "isError", result.isError()));
        } catch (Exception e) {
            log.error("工具执行失败: {}", toolName, e);
            return McpResponse.success(request.getId(), Map.of(
                    "content", List.of(Map.of("type", "text", "text", "Error: " + e.getMessage())),
                    "isError", true));
        } finally {
            // 记录指标（如果有 McpMetrics 可用）
            recordMetrics(success, System.currentTimeMillis() - startTime);
        }
    }

    /**
     * 验证参数（子类可重写以自定义校验逻辑）
     */
    protected ParameterValidator.ValidationResult validateParameters(
            List<ToolParameter> paramDefs, Map<String, Object> arguments) {
        // 默认空实现，子类可通过 Spring 注入 ParameterValidator 并重写此方法
        return ParameterValidator.ValidationResult.success();
    }

    /**
     * 记录指标（子类可重写以集成 McpMetrics）
     */
    protected void recordMetrics(boolean success, long durationMs) {
        // 默认空实现，子类可通过 Spring 注入 McpMetrics 并重写此方法
    }
}
