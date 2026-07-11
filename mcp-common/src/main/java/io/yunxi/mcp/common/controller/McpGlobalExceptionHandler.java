package io.yunxi.mcp.common.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;

/**
 * MCP 全局异常处理器 (v2.0 - 精简版)
 * <p>
 * 协议级错误由官方 MCP SDK 自行处理，此处理器只负责 Spring MVC 层面的异常兜底。
 * </p>
 *
 * @version 2.0.0
 * @since 1.0.0
 */
@Slf4j
@RestControllerAdvice(basePackages = "io.yunxi.mcp")
public class McpGlobalExceptionHandler {

    private static final String JSON_RPC_VERSION = "2.0";
    private static final int PARSE_ERROR = -32700;
    private static final int INVALID_PARAMS = -32602;
    private static final int INTERNAL_ERROR = -32603;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private ResponseEntity<String> buildErrorResponse(int code, String message, String data, HttpStatus httpStatus) {
        try {
            ObjectNode response = objectMapper.createObjectNode();
            response.put("jsonrpc", JSON_RPC_VERSION);
            response.putNull("id");

            ObjectNode error = response.putObject("error");
            error.put("code", code);
            error.put("message", message);
            if (data != null) {
                error.put("data", data);
            }
            error.put("traceId", generateTraceId());

            return ResponseEntity
                    .status(httpStatus)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(response));
        } catch (JsonProcessingException e) {
            log.error("构建错误响应失败", e);
            return ResponseEntity.status(httpStatus)
                    .body("{\"jsonrpc\":\"2.0\",\"error\":{\"code\":" + code + ",\"message\":\"" + message + "\"}}");
        }
    }

    @ExceptionHandler(JsonProcessingException.class)
    public ResponseEntity<String> handleJsonProcessingException(JsonProcessingException e) {
        log.warn("JSON 解析失败: {}", e.getMessage());
        return buildErrorResponse(PARSE_ERROR, "Parse error", e.getOriginalMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("参数无效: {}", e.getMessage());
        return buildErrorResponse(INVALID_PARAMS, "Invalid params", e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalStateException(IllegalStateException e) {
        log.warn("状态异常: {}", e.getMessage());
        return buildErrorResponse(INVALID_PARAMS, "Invalid request", e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGenericException(Exception e) {
        log.error("未处理的异常: {}", e.getClass().getName(), e);
        return buildErrorResponse(INTERNAL_ERROR, "Internal error", e.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
