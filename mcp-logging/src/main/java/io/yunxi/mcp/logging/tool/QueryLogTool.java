package io.yunxi.mcp.logging.tool;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import io.yunxi.mcp.logging.service.LogQueryService;

import java.util.List;
import java.util.Map;

/**
 * 查询日志工具
 * <p>
 * 实现日志查询功能的 MCP 工具，支持按关键词、日志级别、时间范围搜索日志内容。
 * </p>
 *
 * <h3>功能特性</h3>
 * <ul>
 * <li>关键词搜索 - 支持在日志内容中搜索指定关键词</li>
 * <li>级别过滤 - 支持按 ERROR/WARN/INFO/DEBUG 级别过滤</li>
 * <li>时间范围 - 支持按开始时间和结束时间过滤</li>
 * <li>多文件搜索 - 自动搜索日志目录中的所有日志文件</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * 
 * <pre>
 * {
 *   "keyword": "ERROR",
 *   "level": "ERROR",
 *   "startTime": "2024-01-01 00:00:00",
 *   "endTime": "2024-01-01 23:59:59",
 *   "maxLines": 100
 * }
 * </pre>
 *
 * @author yunxi-mcp-servers
 * @since 1.0.0
 * @see LogQueryService
 */
public class QueryLogTool implements ToolHandler {

        /**
         * 日志查询服务
         */
        private final LogQueryService logQueryService;

        /**
         * 构造方法
         *
         * @param logQueryService 日志查询服务
         */
        public QueryLogTool(LogQueryService logQueryService) {
                this.logQueryService = logQueryService;
        }

        /**
         * 获取工具定义
         * <p>
         * 定义工具的元数据，包括名称、描述和输入参数模式。
         * </p>
         *
         * @return 工具定义对象
         */
        @Override
        public ToolDefinition getDefinition() {
                return ToolDefinition.builder()
                                .name("query_logs")
                                .description(
                                                "查询应用日志。\n" +
                                                                "根据关键词、日志级别、时间范围搜索日志内容。\n" +
                                                                "用于诊断系统问题、查看错误信息、追踪业务流程。")
                                .inputSchema(Map.of(
                                                "type", "object",
                                                "properties", Map.of(
                                                                "keyword", Map.of(
                                                                                "type", "string",
                                                                                "description", "日志关键词，可选"),
                                                                "level", Map.of(
                                                                                "type", "string",
                                                                                "description",
                                                                                "日志级别: ERROR, WARN, INFO, DEBUG"),
                                                                "startTime", Map.of(
                                                                                "type", "string",
                                                                                "description",
                                                                                "开始时间 (yyyy-MM-dd HH:mm:ss)"),
                                                                "endTime", Map.of(
                                                                                "type", "string",
                                                                                "description",
                                                                                "结束时间 (yyyy-MM-dd HH:mm:ss)"),
                                                                "maxLines", Map.of(
                                                                                "type", "number",
                                                                                "description", "最大返回行数，默认100"))))
                                .build();
        }

        /**
         * 执行日志查询
         * <p>
         * 根据传入的参数执行日志查询，返回匹配的日志内容。
         * </p>
         *
         * @param arguments 查询参数，包含 keyword、level、startTime、endTime、maxLines
         * @return 查询结果，包含匹配的日志列表或错误信息
         */
        @Override
        public ToolResult execute(Map<String, Object> arguments) {
                String keyword = (String) arguments.get("keyword");
                String level = (String) arguments.get("level");
                String startTime = (String) arguments.get("startTime");
                String endTime = (String) arguments.get("endTime");
                int maxLines = arguments.containsKey("maxLines")
                                ? ((Number) arguments.get("maxLines")).intValue()
                                : 100;

                try {
                        List<String> logs = logQueryService.queryLogs(keyword, level, startTime, endTime, maxLines);

                        if (logs.isEmpty()) {
                                return ToolResult.text("未找到匹配条件的日志");
                        }

                        StringBuilder sb = new StringBuilder();
                        sb.append("找到 ").append(logs.size()).append(" 条日志:\n\n");
                        for (String log : logs) {
                                sb.append(log).append("\n");
                        }

                        return ToolResult.text(sb.toString());
                } catch (Exception e) {
                        return ToolResult.error("查询日志失败: " + e.getMessage());
                }
        }
}
