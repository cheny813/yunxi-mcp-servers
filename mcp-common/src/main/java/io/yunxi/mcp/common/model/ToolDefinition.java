package io.yunxi.mcp.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * MCP Tool 定义
 * <p>
 * 定义 MCP (Model Context Protocol) 中可被 AI Agent 调用的工具的元数据。
 * </p>
 * <p>
 * Tool 定义遵循 JSON Schema 规范，描述了工具的输入输出接口。
 * AI Agent 通过工具定义来了解工具的功能和使用方式，然后选择合适的工具完成任务。
 * </p>
 * <p>
 * <b>Tool 定义示例：</b>
 * 
 * <pre>{@code
 * {
 *   "name": "read_file",
 *   "description": "Read a file from the filesystem",
 *   "inputSchema": {
 *     "type": "object",
 *     "properties": {
 *       "path": {
 *         "type": "string",
 *         "description": "File path to read (relative to allowed directory)"
 *       },
 *       "encoding": {
 *         "type": "string",
 *         "description": "File encoding (default: utf-8)"
 *       }
 *     },
 *     "required": ["path"]
 *   }
 * }
 * }</pre>
 * </p>
 * <p>
 * <b>命名规范：</b>
 * <ul>
 * <li>工具名称使用 {@code snake_case} 命名（小写字母+下划线）</li>
 * <li>描述应当简洁明了，说明工具的核心功能</li>
 * <li>参数名称也使用 {@code snake_case} 命名</li>
 * <li>参数描述应说明参数的含义、类型、取值范围等</li>
 * </ul>
 * </p>
 * <p>
 * <b>Schema 最佳实践：</b>
 * <ul>
 * <li>为可选参数提供合理的默认值</li>
 * <li>使用精确的类型定义（string, integer, boolean, array, object）</li>
 * <li>对枚举值在描述中列出所有可选值</li>
 * <li>对复杂类型（如数组元素、对象属性）使用嵌套的 properties 定义</li>
 * <li>避免过度复杂的嵌套结构，保持 schema 易于理解</li>
 * </ul>
 * </p>
 *

 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolDefinition {

        /**
         * 工具名称
         * <p>
         * 工具的唯一标识符，用于在调用时指定要执行的工具。
         * </p>
         * <p>
         * <b>命名规则：</b>
         * <ul>
         * <li>使用小写字母和下划线（snake_case）</li>
         * <li>以动词开头，表示工具执行的操作</li>
         * <li>名称应当简短且具有描述性</li>
         * <li>避免使用缩写和模糊的名称</li>
         * </ul>
         * </p>
         * <p>
         * <b>示例：</b>
         * <ul>
         * <li>{@code read_file} - 读取文件</li>
         * <li>{@code write_file} - 写入文件</li>
         * <li>{@code list_directory} - 列出目录内容</li>
         * <li>{@code search_files} - 搜索文件</li>
         * </ul>
         * </p>
         */
        private String name;

        /**
         * 工具描述
         * <p>
         * 对工具功能的简要说明，帮助 AI Agent 理解工具的用途。
         * </p>
         * <p>
         * <b>描述要求：</b>
         * <ul>
         * <li>清晰说明工具的作用</li>
         * <li>使用祈使动词（Read, Write, List, Search 等）</li>
         * <li>简明扼要，通常不超过 50 个词</li>
         * <li>可以提及工具的适用场景或限制</li>
         * </ul>
         * </p>
         * <p>
         * <b>示例：</b>
         * <ul>
         * <li>{@code "Read a file from the filesystem"}</li>
         * <li>{@code "Search files matching a pattern"}</li>
         * <li>{@code "Execute SQL query on database"}</li>
         * </ul>
         * </p>
         */
        private String description;

        /**
         * 输入参数 JSON Schema
         * <p>
         * 使用 JSON Schema 格式定义工具的输入参数，包括：
         * <ul>
         * <li>参数名称和类型</li>
         * <li>参数描述</li>
         * <li>参数是否必填</li>
         * <li>参数默认值</li>
         * <li>参数约束（如取值范围、格式等）</li>
         * </ul>
         * </p>
         * <p>
         * <b>支持的 JSON Schema 类型：</b>
         * <ul>
         * <li>{@code string} - 字符串类型</li>
         * <li>{@code integer} - 整数类型</li>
         * <li>{@code number} - 数字类型（整数或浮点数）</li>
         * <li>{@code boolean} - 布尔类型</li>
         * <li>{@code array} - 数组类型，配合 {@code items} 定义元素类型</li>
         * <li>{@code object} - 对象类型，配合 {@code properties} 定义属性</li>
         * </ul>
         * </p>
         * <p>
         * <b>示例 Schema：</b>
         * 
         * <pre>{@code
         * {
         *   "type": "object",
         *   "properties": {
         *     "path": {
         *       "type": "string",
         *       "description": "File path",
         *       "minLength": 1
         *     },
         *     "limit": {
         *       "type": "integer",
         *       "description": "Result limit",
         *       "default": 10,
         *       "minimum": 1,
         *       "maximum": 100
         *     },
         *     "recursive": {
         *       "type": "boolean",
         *       "description": "Search recursively",
         *       "default": false
         *     }
         *   },
         *   "required": ["path"]
         * }
         * }</pre>
         * </p>
         *
         * @see <a href="https://json-schema.org/">JSON Schema Specification</a>
         */
        private Map<String, Object> inputSchema;

        /**
     * 输出参数 JSON Schema
     * <p>
     * 定义工具调用成功后返回结果的 JSON 结构。
     * </p>
     */
    private Map<String, Object> outputSchema;

    /**
     * 工具参数定义
     * <p>
     * 提供对工具参数的明确描述，包含名称、类型、是否必填和示例值。
     * </p>
     */
    private List<ToolParameter> parameters;

    /**
     * 工具返回类型描述
     * <p>
     * 描述工具执行后的输出类型，便于能力发现和文档生成。
     * </p>
     */
    private String returnType;

    /**
     * 工具元数据
     * <p>
     * 补充工具版本、分类、标签和扩展信息。
     * </p>
     */
    private ToolMetadata metadata;

    /**
     * 示例输入/输出
     * <p>
     * 用于帮助 LLM 理解工具调用方式和预期结果。
     * </p>
     */
    private List<Map<String, Object>> examples;

    /**
     * 创建简单文本参数的工具定义
     * <p>
     * 便捷方法，用于创建只有单个字符串参数的简单工具定义。
     * </p>
     *
     * @param name             工具名称
     * @param description      工具描述
     * @param paramName        参数名称
     * @param paramDescription 参数描述
     * @return 工具定义对象
     */
    public static ToolDefinition simpleText(String name, String description, String paramName,
                    String paramDescription) {
                return ToolDefinition.builder()
                                .name(name)
                                .description(description)
                                .inputSchema(Map.of(
                                                "type", "object",
                                                "properties", Map.of(
                                                                paramName, Map.of(
                                                                                "type", "string",
                                                                                "description", paramDescription)),
                                                "required", new String[] { paramName }))
                                .outputSchema(Map.of(
                                                "type", "string",
                                                "description", "The text result returned by the tool."))
                                .parameters(List.of(ToolParameter.builder()
                                                .name(paramName)
                                                .description(paramDescription)
                                                .type("string")
                                                .required(true)
                                                .example("example value")
                                                .build()))
                                .returnType("string")
                                .examples(List.of(Map.of(
                                                "input", Map.of(paramName, "example value"),
                                                "output", "Example result text")))
                                .build();
        }
}
