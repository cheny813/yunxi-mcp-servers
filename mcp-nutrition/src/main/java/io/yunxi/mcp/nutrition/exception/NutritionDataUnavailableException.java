package io.yunxi.mcp.nutrition.exception;

/**
 * 营养数据服务不可用异常（如 MySQL 连接失败、营养库未初始化）。
 *
 * <p>由数据访问层在捕获到底层 {@code DataAccessException} 后抛出，工具层据此向 Agent
 * 返回友好错误而非让 MCP 调用崩溃，实现"数据库未连接时自动降级"。</p>
 */
public class NutritionDataUnavailableException extends RuntimeException {

    public NutritionDataUnavailableException(String message) {
        super(message);
    }

    public NutritionDataUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
