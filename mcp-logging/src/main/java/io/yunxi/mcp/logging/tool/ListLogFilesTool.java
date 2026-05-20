package io.yunxi.mcp.logging.tool;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import io.yunxi.mcp.logging.service.LogQueryService;

import java.util.List;
import java.util.Map;

/**
 * 列出日志文件工具
 * <p>
 * MCP 工具实现，用于列出日志目录中的所有日志文件。
 * </p>
 *
 * <h3>功能说明</h3>
 * <ul>
 * <li>扫描配置的日志目录</li>
 * <li>返回所有 .log 文件列表</li>
 * <li>显示文件大小信息</li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <ul>
 * <li>查询前查看可用日志文件</li>
 * <li>了解日志文件大小和数量</li>
 * <li>确定要分析的目标日志</li>
 * </ul>
 *
 * <h3>工具信息</h3>
 * <ul>
 * <li>工具名称: list_log_files</li>
 * <li>输入参数: 无</li>
 * <li>输出格式: 文本列表</li>
 * </ul>
 *
 * @author yunxi-mcp-servers
 * @see QueryLogTool
 * @see LogQueryService
 * @since 1.0.0
 */
public class ListLogFilesTool implements ToolHandler {

    /**
     * 日志查询服务
     */
    private final LogQueryService logQueryService;

    /**
     * 构造方法
     *
     * @param logQueryService 日志查询服务实例
     */
    public ListLogFilesTool(LogQueryService logQueryService) {
        this.logQueryService = logQueryService;
    }

    /**
     * 获取工具定义
     * <p>
     * 定义工具的元数据信息，包括名称、描述和输入参数模式。
     * </p>
     *
     * @return 工具定义对象
     */
    @Override
    public ToolDefinition getDefinition() {
        return ToolDefinition.builder()
                .name("list_log_files")
                .description(
                        "列出可用的日志文件。\n" +
                                "返回日志目录中的所有日志文件列表及其大小。\n" +
                                "用于确定要查询的日志文件。")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of()))
                .build();
    }

    /**
     * 执行工具
     * <p>
     * 列出日志目录中的所有日志文件。
     * </p>
     *
     * @param arguments 工具输入参数（本工具无需参数）
     * @return 工具执行结果，包含日志文件列表
     */
    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        try {
            List<String> files = logQueryService.listLogFiles();

            if (files.isEmpty()) {
                return ToolResult.text("未找到日志文件");
            }

            StringBuilder sb = new StringBuilder();
            sb.append("日志文件列表:\n\n");
            for (String file : files) {
                sb.append("- ").append(file).append("\n");
            }

            return ToolResult.text(sb.toString());
        } catch (Exception e) {
            return ToolResult.error("获取日志文件列表失败: " + e.getMessage());
        }
    }
}
