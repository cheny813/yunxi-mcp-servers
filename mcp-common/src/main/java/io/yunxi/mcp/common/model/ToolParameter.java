package io.yunxi.mcp.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * MCP 工具参数定义。
 * <p>
 * 描述工具输入参数的名称、类型、是否必填、默认值和可选值。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolParameter {

    /** 参数名称，使用 snake_case 命名规范。 */
    private String name;

    /** 参数描述，说明参数用途与约束。 */
    private String description;

    /** 参数类型，例如 string、integer、boolean、array、object 等。 */
    private String type;

    /** 是否为必填参数。 */
    private boolean required;

    /** 默认值。*/
    private Object defaultValue;

    /** 示例值。*/
    private Object example;

    /** 枚举可选值。*/
    private List<Object> enumValues;

    /** 嵌套对象或数组元素类型定义。*/
    private McpToolSchema schema;

    /** 正则表达式模式（用于字符串校验）。*/
    private String pattern;

    /** 最小长度（用于字符串校验）。*/
    private Integer minLength;

    /** 最大长度（用于字符串校验）。*/
    private Integer maxLength;

    /** 最小值（用于数值校验）。*/
    private Double minimum;

    /** 最大值（用于数值校验）。*/
    private Double maximum;
}
