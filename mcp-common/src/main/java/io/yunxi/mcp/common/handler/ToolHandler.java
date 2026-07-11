package io.yunxi.mcp.common.handler;

import java.util.Map;

/**
 * MCP Tool 处理器接口 (v2.0 - 基于官方 MCP SDK)
 * <p>
 * 定义 MCP (Model Context Protocol) 工具的最小化接口。
 * 所有 MCP 服务器模块的工具实现只需实现此接口，框架层通过
 * {@link io.yunxi.mcp.common.config.McpServerFactory} 自动适配到官方 MCP SDK。
 * </p>
 *
 * <h3>迁移指南（从 v1.0）</h3>
 * <p>旧版接口变更为：</p>
 * <ul>
 * <li>{@code ToolDefinition getDefinition()} → 拆分为三个独立方法：
 *     {@link #getName()}, {@link #getDescription()}, {@link #getInputSchema()}</li>
 * <li>{@code ToolResult execute(Map)} → {@link #execute(Map)} 返回纯文本 String</li>
 * <li>错误处理：抛出自定义 RuntimeException，框架层自动捕获并设置 isError=true</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * @Component
 * public class QueryTool implements ToolHandler {
 *     @Override
 *     public String getName() { return "query"; }
 *
 *     @Override
 *     public String getDescription() { return "Execute SQL query"; }
 *
 *     @Override
 *     public Map<String, Object> getInputSchema() {
 *         return Map.of(
 *             "type", "object",
 *             "properties", Map.of(
 *                 "sql", Map.of("type", "string", "description", "SQL statement")
 *             ),
 *             "required", List.of("sql")
 *         );
 *     }
 *
 *     @Override
 *     public String execute(Map<String, Object> arguments) {
 *         String sql = (String) arguments.get("sql");
 *         return jdbcTemplate.query(sql);
 *     }
 * }
 * }</pre>
 *
 * @version 2.0.0
 * @since 1.0.0
 */
public interface ToolHandler {

    /**
     * 工具名称（唯一标识符）
     * <p>使用 snake_case 命名规范，如 {@code query_db}, {@code list_tables}</p>
     *
     * @return 工具名称
     */
    String getName();

    /**
     * 工具描述
     * <p>简要说明工具的功能和用途，帮助 AI Agent 理解何时使用此工具</p>
     *
     * @return 工具描述文本
     */
    String getDescription();

    /**
     * 输入参数的 JSON Schema 定义
     * <p>使用标准 JSON Schema 格式描述输入参数的类型、描述、必填项等</p>
     *
     * <pre>{@code
     * {
     *   "type": "object",
     *   "properties": {
     *     "param1": { "type": "string", "description": "参数1" }
     *   },
     *   "required": ["param1"]
     * }
     * }</pre>
     *
     * @return JSON Schema 的 Map 表示
     */
    Map<String, Object> getInputSchema();

    /**
     * 执行工具逻辑
     * <p>根据参数执行具体操作，返回纯文本结果</p>
     * <p>执行失败时抛出 RuntimeException 即可，框架层会自动包装为 isError=true 的响应</p>
     *
     * @param arguments 工具输入参数，键为参数名
     * @return 执行结果文本（支持 JSON 字符串、Markdown、纯文本等格式）
     * @throws RuntimeException 执行失败
     */
    String execute(Map<String, Object> arguments);
}
