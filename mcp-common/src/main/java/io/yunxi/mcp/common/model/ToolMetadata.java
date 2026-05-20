package io.yunxi.mcp.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * MCP 工具元数据。
 * <p>
 * 包含工具的版本、分类、标签等附加信息，便于能力发现和文档生成。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolMetadata {

    /** 工具版本，便于能力演进和兼容性管理。 */
    private String version;

    /** 工具分类，用于能力分组。 */
    private String category;

    /** 工具标签，用于能力搜索和筛选。 */
    private List<String> tags;

    /** 是否支持异步调用。 */
    private Boolean supportsAsync;

    /** 附加扩展字段，允许供应方补充自定义元数据。 */
    private Map<String, Object> extensions;
}
