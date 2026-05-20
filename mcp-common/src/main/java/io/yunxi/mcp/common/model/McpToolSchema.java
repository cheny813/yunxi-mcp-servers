package io.yunxi.mcp.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * MCP 工具参数 JSON Schema 描述。
 * <p>
 * 用于描述工具输入输出结构，支持基本类型、对象、数组和枚举。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpToolSchema {

    /** 数据类型，例如 object、string、integer、boolean、array 等。 */
    private String type;

    /** 属性定义，适用于 object 类型。 */
    private Map<String, McpToolSchema> properties;

    /** 必填字段列表。 */
    private List<String> required;

    /** 描述信息。 */
    private String description;

    /** 示例值。 */
    private Object example;

    /** 可选枚举值。 */
    private List<Object> enumValues;

    /** 数组元素类型定义，适用于 array 类型。 */
    private McpToolSchema items;

    /** 允许额外属性。 */
    private Object additionalProperties;
}
