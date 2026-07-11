package io.yunxi.mcp.database.tool;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.database.config.DatabaseConfig;
import io.yunxi.mcp.database.config.DatabaseConfigService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 列出所有可用数据库
 *
 * @author yunxi
 * @version 2.0.0
 * @since 1.0.0
 */
@Component
public class ListDatabasesTool implements ToolHandler {

    private final DatabaseConfigService configService;

    public ListDatabasesTool(DatabaseConfigService configService) {
        this.configService = configService;
    }

    @Override
    public String getName() {
        return "list_databases";
    }

    @Override
    public String getDescription() {
        return "List all available databases that can be queried. " +
                "列出所有可用的数据库。 " +
                "Use this to see which databases are configured before making a query. " +
                "查询前使用此工具查看可用数据库。";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of());
    }

    @Override
    public String execute(Map<String, Object> arguments) {
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

            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error listing databases: " + e.getMessage());
        }
    }
}