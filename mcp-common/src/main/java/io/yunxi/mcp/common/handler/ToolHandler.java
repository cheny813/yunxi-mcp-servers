package io.yunxi.mcp.common.handler;

import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;

import java.util.Map;

/**
 * MCP Tool 处理器接口
 * <p>
 * 实现 MCP (Model Context Protocol) 的工具功能的接口。
 * 每个 Tool 都是一个可被 AI Agent 调用的操作单元，提供特定的功能服务。
 * </p>
 * <p>
 * Tool 是 MCP 协议中最重要的概念之一，它定义了：
 * <ul>
 * <li><b>元数据</b>: 工具的名称、描述、参数 schema</li>
 * <li><b>执行逻辑</b>: 工具的具体实现</li>
 * <li><b>返回结果</b>: 工具执行后的输出</li>
 * </ul>
 * </p>
 * <p>
 * 使用示例：
 * 
 * <pre>
 * {
 *     &#64;code
 *     public class ReadFileTool implements ToolHandler {
 *         private final Path baseDir;
 *
 *         public ReadFileTool(Path baseDir) {
 *             this.baseDir = baseDir;
 *         }
 *
 *         &#64;Override
 *         public ToolDefinition getDefinition() {
 *             return ToolDefinition.builder()
 *                     .name("read_file")
 *                     .description("Read a file from the filesystem")
 *                     .inputSchema(Map.of(
 *                             "type", "object",
 *                             "properties", Map.of(
 *                                     "path", Map.of(
 *                                             "type", "string",
 *                                             "description", "File path to read")),
 *                             "required", List.of("path")))
 *                     .build();
 *         }
 *
 *         @Override
 *         public ToolResult execute(Map<String, Object> arguments) {
 *             String path = (String) arguments.get("path");
 *             Path fullPath = baseDir.resolve(path).normalize();
 *
 *             try {
 *                 String content = Files.readString(fullPath);
 *                 return ToolResult.text(content);
 *             } catch (IOException e) {
 *                 return ToolResult.error("Failed to read file: " + e.getMessage());
 *             }
 *         }
 *     }
 * }
 * </pre>
 * </p>
 * <p>
 * 实现注意事项：
 * <ul>
 * <li>工具名称应当使用下划线命名（snake_case），如 {@code read_file}</li>
 * <li>输入参数使用 JSON Schema 定义，确保类型和必填项正确</li>
 * <li>执行方法应当处理所有可能的异常，避免向客户端暴露内部错误</li>
 * <li>返回结果应当格式化为易于理解的文本或结构化数据</li>
 * <li>对于耗时操作，考虑添加超时机制或异步处理</li>
 * <li>对于有副作用的操作（如写入文件），建议在描述中明确说明</li>
 * </ul>
 * </p>
 * <p>
 * 工具的生命周期：
 * <ol>
 * <li>工具被注册到 MCP 服务器时创建实例</li>
 * <li>客户端调用 {@code tools/list} 时获取工具定义</li>
 * <li>客户端调用 {@code tools/call} 时执行工具逻辑</li>
 * <li>服务器关闭时工具实例被销毁</li>
 * </ol>
 * </p>
 *
 * @version 1.0.0
 * @since 1.0.0
 */
public interface ToolHandler {

    /**
     * 获取工具定义
     * <p>
     * 返回工具的元数据信息，包括：
     * <ul>
     * <li>name: 工具的唯一标识符</li>
     * <li>description: 工具功能的简要描述</li>
     * <li>inputSchema: 输入参数的 JSON Schema 定义</li>
     * </ul>
     * </p>
     * <p>
     * JSON Schema 格式：
     * 
     * <pre>{@code
     * {
     *   "type": "object",
     *   "properties": {
     *     "param1": {
     *       "type": "string",
     *       "description": "参数1的描述"
     *     },
     *     "param2": {
     *       "type": "integer",
     *       "description": "参数2的描述",
     *       "default": 0
     *     }
     *   },
     *   "required": ["param1"]
     * }
     * }</pre>
     * </p>
     *
     * @return 工具定义对象
     * @see ToolDefinition
     */
    ToolDefinition getDefinition();

    /**
     * 执行工具逻辑
     * <p>
     * 根据传入的参数执行工具的具体操作，并返回执行结果。
     * </p>
     * <p>
     * 执行规则：
     * <ul>
     * <li>所有参数都通过 {@code arguments} Map 传入，未提供的参数值为 {@code null}</li>
     * <li>方法应该是幂等的，除非工具本身有明确的副作用</li>
     * <li>执行时间应当合理（建议不超过 30 秒）</li>
     * <li>应当捕获所有异常并返回错误响应</li>
     * <li>不应当抛出未检查的异常</li>
     * </ul>
     * </p>
     *
     * @param arguments 工具输入参数，键为参数名，值为参数值
     *                  参数值类型：String, Integer, Boolean, List, Map 等
     * @return 工具执行结果
     *         成功时使用 {@code ToolResult.text()} 返回结果文本
     *         失败时使用 {@code ToolResult.error()} 返回错误信息
     * @see ToolResult
     */
    ToolResult execute(Map<String, Object> arguments);

    /**
     * 获取工具名称
     * <p>
     * 默认实现从工具定义中提取名称。
     * 子类也可以重写此方法直接返回名称。
     * </p>
     *
     * @return 工具名称字符串，应当使用 snake_case 命名规范
     */
    default String getName() {
        return getDefinition().getName();
    }
}
