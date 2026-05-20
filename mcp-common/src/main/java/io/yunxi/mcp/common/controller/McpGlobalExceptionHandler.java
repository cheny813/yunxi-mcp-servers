package io.yunxi.mcp.common.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.yunxi.mcp.common.constants.McpErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.UUID;

/**
 * MCP 全局异常处理器
 * <p>
 * 统一处理 MCP 服务中的各类异常，返回标准 JSON-RPC 2.0 错误格式。
 * 所有 MCP 模块自动继承此处理器，确保错误响应格式一致。
 * </p>
 *
 * <h3>错误响应格式</h3>
 * <pre>{@code
 * {
 *   "jsonrpc": "2.0",
 *   "error": {
 *     "code": -32603,
 *     "message": "Internal error",
 *     "data": "详细错误描述",
 *     "traceId": "请求追踪ID"
 *   },
 *   "id": null
 * }
 * }</pre>
 *
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@RestControllerAdvice(basePackages = "io.yunxi.mcp")
public class McpGlobalExceptionHandler {

    private static final String JSON_RPC_VERSION = "2.0";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 生成追踪ID
     */
    private static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * 构建标准 JSON-RPC 错误响应
     */
    private ResponseEntity<String> buildErrorResponse(int code, String message, Object data, String traceId, HttpStatus httpStatus) {
        try {
            ObjectNode response = objectMapper.createObjectNode();
            response.put("jsonrpc", JSON_RPC_VERSION);
            response.putNull("id");

            ObjectNode error = response.putObject("error");
            error.put("code", code);
            error.put("message", message);
            if (data != null) {
                error.putPOJO("data", data);
            }
            error.put("traceId", traceId);

            return ResponseEntity
                    .status(httpStatus)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(response));
        } catch (JsonProcessingException e) {
            log.error("构建错误响应失败", e);
            return ResponseEntity
                    .status(httpStatus)
                    .body("{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":" + code
                            + ",\"message\":\"" + message + "\"}}");
        }
    }

    /**
     * 处理 JSON 解析异常
     */
    @ExceptionHandler(JsonProcessingException.class)
    public ResponseEntity<String> handleJsonProcessingException(JsonProcessingException e) {
        String traceId = generateTraceId();
        log.warn("[traceId={}] JSON 解析失败: {}", traceId, e.getMessage());
        return buildErrorResponse(
                McpErrorCode.PARSE_ERROR,
                "Parse error",
                e.getOriginalMessage(),
                traceId,
                HttpStatus.BAD_REQUEST
        );
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
        String traceId = generateTraceId();
        log.warn("[traceId={}] 参数无效: {}", traceId, e.getMessage());
        return buildErrorResponse(
                McpErrorCode.INVALID_PARAMS,
                "Invalid params",
                e.getMessage(),
                traceId,
                HttpStatus.BAD_REQUEST
        );
    }

    /**
     * 处理非法状态异常
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalStateException(IllegalStateException e) {
        String traceId = generateTraceId();
        log.warn("[traceId={}] 状态异常: {}", traceId, e.getMessage());
        return buildErrorResponse(
                McpErrorCode.INVALID_REQUEST,
                "Invalid request",
                e.getMessage(),
                traceId,
                HttpStatus.BAD_REQUEST
        );
    }

    /**
     * 处理资源未找到异常
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<String> handleNoResourceFoundException(NoResourceFoundException e) {
        String traceId = generateTraceId();
        log.warn("[traceId={}] 资源未找到: {}", traceId, e.getMessage());
        return buildErrorResponse(
                McpErrorCode.METHOD_NOT_FOUND,
                "Method not found",
                e.getMessage(),
                traceId,
                HttpStatus.NOT_FOUND
        );
    }

    /**
     * 处理安全异常（权限、认证等）
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<String> handleSecurityException(SecurityException e) {
        String traceId = generateTraceId();
        log.warn("[traceId={}] 安全异常: {}", traceId, e.getMessage());
        return buildErrorResponse(
                McpErrorCode.SERVER_ERROR_START - 1,
                "Unauthorized",
                e.getMessage(),
                traceId,
                HttpStatus.FORBIDDEN
        );
    }

    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<String> handleNullPointerException(NullPointerException e) {
        String traceId = generateTraceId();
        log.error("[traceId={}] 空指针异常", traceId, e);
        return buildErrorResponse(
                McpErrorCode.INTERNAL_ERROR,
                "Internal error",
                "A required value was null",
                traceId,
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    /**
     * 处理所有未捕获的异常（兜底）
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGenericException(Exception e) {
        String traceId = generateTraceId();
        log.error("[traceId={}] 未处理的异常: {}", traceId, e.getClass().getName(), e);
        return buildErrorResponse(
                McpErrorCode.INTERNAL_ERROR,
                "Internal error",
                e.getMessage(),
                traceId,
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}
