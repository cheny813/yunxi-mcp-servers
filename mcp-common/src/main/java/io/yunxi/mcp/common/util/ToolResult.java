package io.yunxi.mcp.common.util;

/**
 * v2.0 兼容工具类 (Tool Result Helper)
 * <p>
 * 在 v2.0 架构中，{@code ToolHandler.execute(...)} 直接返回纯文本 {@link String}，
 * 官方 MCP SDK 负责将其包装为 {@code CallToolResult}。此类仅作为遗留工具私有方法的
 * 轻量兼容垫片，提供返回 {@code String} 的静态工厂，避免分散在多个模块中的历史调用
 * 需要逐处改写。
 * </p>
 *
 * <p>对于错误场景，推荐直接 {@code throw new RuntimeException(message)}，框架层会自动
 * 捕获并设置 {@code isError = true}；历史代码中的 {@link #error(String)} 等价于返回错误文本。</p>
 *
 * @version 2.0.0
 * @since 2.0.0
 */
public final class ToolResult {

    private ToolResult() {
    }

    /**
     * 返回工具成功结果文本
     */
    public static String text(String content) {
        return content;
    }

    /**
     * 返回工具错误结果文本
     * <p>语义上等同于直接返回错误描述文本；如需标记 isError，请改用抛异常。</p>
     */
    public static String error(String message) {
        return message;
    }
}
