package io.yunxi.mcp.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * MCP Tool 执行结果
 * <p>
 * 封装 MCP Tool 执行后的返回结果，包括执行内容和错误状态。
 * </p>
 * <p>
 * <b>结果格式：</b>
 * 
 * <pre>{@code
 * {
 *   "content": [
 *     {
 *       "type": "text",
 *       "text": "执行结果文本"
 *     }
 *   ],
 *   "isError": false
 * }
 * }</pre>
 * </p>
 * <p>
 * <b>结果类型：</b>
 * <ul>
 * <li><b>成功结果</b>: {@code isError = false}，包含执行产生的输出</li>
 * <li><b>错误结果</b>: {@code isError = true}，包含错误信息</li>
 * </ul>
 * </p>
 * <p>
 * <b>内容类型：</b>
 * 目前支持 {@code text} 类型，表示纯文本内容。
 * 未来版本可能扩展支持更多类型（如 Markdown、JSON、图像等）。
 * </p>
 * <p>
 * <b>最佳实践：</b>
 * <ul>
 * <li>成功结果应当清晰易读，避免输出冗余信息</li>
 * <li>错误信息应当包含足够的上下文，帮助调试问题</li>
 * <li>对于大量数据，考虑使用格式化输出（如表格、列表）</li>
 * <li>敏感信息（如密码、密钥）应当被脱敏处理</li>
 * <li>结构化数据建议使用 JSON 或 YAML 格式输出</li>
 * </ul>
 * </p>
 * <p>
 * <b>使用示例：</b>
 * 
 * <pre>{@code
 * // 成功结果
 * return ToolResult.text("File content: " + content);
 *
 * // 错误结果
 * return ToolResult.error("File not found: " + path);
 *
 * // 格式化结果
 * StringBuilder sb = new StringBuilder();
 * sb.append("Files found: ").append(files.size()).append("\n");
 * files.forEach(f -> sb.append("- ").append(f).append("\n"));
 * return ToolResult.text(sb.toString());
 * }</pre>
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
public class ToolResult {

    /**
     * 内容列表
     * <p>
     * 工具执行产生的内容，可以包含多个内容项。
     * </p>
     * <p>
     * 每个内容项包含：
     * <ul>
     * <li><b>type</b>: 内容类型，目前支持 {@code "text"}</li>
     * <li><b>text</b>: 内容文本</li>
     * </ul>
     * </p>
     * <p>
     * <b>设计考虑：</b>
     * <ul>
     * <li>支持多个内容项，便于分离不同类型的信息</li>
     * <li>未来可扩展支持更多内容类型（image、resource 等）</li>
     * <li>顺序保持，确保内容按预期顺序展示</li>
     * </ul>
     * </p>
     */
    private List<Content> content;

    /**
     * 错误标志
     * <p>
     * 标识工具执行是否成功：
     * <ul>
     * <li>{@code false} - 执行成功，content 包含正常输出</li>
     * <li>{@code true} - 执行失败，content 包含错误信息</li>
     * </ul>
     * </p>
     * <p>
     * <b>错误处理建议：</b>
     * <ul>
     * <li>对于参数错误，返回具体的错误信息</li>
     * <li>对于权限错误，提示缺少的权限或配置</li>
     * <li>对于资源不存在，明确说明资源名称和位置</li>
     * <li>对于超时错误，提供已处理的部分结果</li>
     * <li>避免暴露系统内部细节（如堆栈跟踪）</li>
     * </ul>
     * </p>
     */
    private boolean isError;

    /**
     * 创建文本结果
     * <p>
     * 创建一个包含纯文本内容的成功结果。
     * </p>
     *
     * @param text 执行结果文本
     * @return 结果对象，isError = false
     */
    public static ToolResult text(String text) {
        return ToolResult.builder()
                .content(List.of(Content.text(text)))
                .isError(false)
                .build();
    }

    /**
     * 创建错误结果
     * <p>
     * 创建一个包含错误信息的失败结果。
     * </p>
     *
     * @param errorMessage 错误信息，应当描述清楚错误原因和建议的解决方案
     * @return 结果对象，isError = true
     */
    public static ToolResult error(String errorMessage) {
        return ToolResult.builder()
                .content(List.of(Content.text(errorMessage)))
                .isError(true)
                .build();
    }

    /**
     * 内容项
     * <p>
     * 表示工具结果中的单个内容块。
     * </p>
     * <p>
     * <b>未来扩展：</b>
     * 可能支持的内容类型：
     * <ul>
     * <li>{@code text} - 纯文本</li>
     * <li>{@code markdown} - Markdown 格式文本</li>
     * <li>{@code image} - 图像数据</li>
     * <li>{@code resource} - 资源引用（如文件 URL）</li>
     * <li>{@code json} - 结构化 JSON 数据</li>
     * </ul>
     * </p>
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        /**
         * 内容类型
         */
        private String type;

        /**
         * 内容文本
         */
        private String text;

        /**
         * 创建文本类型内容
         *
         * @param text 文本内容
         * @return Content 对象，type = "text"
         */
        public static Content text(String text) {
            return Content.builder()
                    .type("text")
                    .text(text)
                    .build();
        }
    }
}
