package io.yunxi.mcp.database.tool;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import io.yunxi.mcp.database.config.DatabaseConfig;
import io.yunxi.mcp.database.config.DatabaseConfigService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 列出所有可用数据库
 * <p>
 * 工具名称: {@code list_databases}
 * </p>
 * <p>
 * 列出所有已配置的数据库，供查询前参考。
 * 返回数据库 ID、名称、主机地址和数据库名。
 * </p>
 *
 * @author yunxi
 * @version 1.0.0
 * @since 1.0.0
 */
public class ListDatabasesTool implements ToolHandler {

    /**
     * 数据库配置服务
     * <p>
     * 提供多数据库配置管理功能。
     * </p>
     */
    private final DatabaseConfigService configService;

    public ListDatabasesTool(DatabaseConfigService configService) {
        this.configService = configService;
    }

    @Override
    public ToolDefinition getDefinition() {
        return ToolDefinition.builder()
                .name("list_databases")
                .description(
                        "List all available databases that can be queried. " +
                                "列出所有可用的数据库。 " +
                                "Use this to see which databases are configured before making a query. " +
                                "查询前使用此工具查看可用数据库。")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of()))
                .build();
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        try {
            Map<String, DatabaseConfig> configs = configService.getAllDatabases();

            List<Map<String, Object>> list = configs.entrySet().stream()
                    .map(e -> {
                        Map<String, Object> db = new java.util.LinkedHashMap<>();
                        db.put("id", e.getKey());
                        db.put("name", e.getValue().getName());
                        db.put("host", e.getValue().getHost() + ":" + e.getValue().getPort());
                        db.put("database", e.getValue().getDatabase());
                        return db;
                    })
                    .collect(Collectors.toList());

            StringBuilder sb = new StringBuilder();
            sb.append("Available Databases:\n");
            if (list.isEmpty()) {
                sb.append("  (none configured)\n");
            } else {
                for (var db : list) {
                    sb.append("  - ").append(db.get("id"))
                            .append(": ").append(db.get("name"))
                            .append(" (").append(db.get("host"))
                            .append("/").append(db.get("database")).append(")\n");
                }
            }

            return ToolResult.text(sb.toString());
        } catch (Exception e) {
            return ToolResult.error("Error listing databases: " + e.getMessage());
        }
    }
}