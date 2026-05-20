package io.yunxi.mcp.database.tool;

import io.yunxi.mcp.database.DatabaseMcpServer;
import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;

import java.sql.*;
import java.util.*;

/**
 * 列出数据库表工具
 */
public class ListTablesTool implements ToolHandler {

    private final DatabaseMcpServer server;

    public ListTablesTool(DatabaseMcpServer server) {
        this.server = server;
    }

    @Override
    public ToolDefinition getDefinition() {
        return ToolDefinition.builder()
                .name("list_tables")
                .description("List all tables in the database")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "schema", Map.of(
                                        "type", "string",
                                        "description", "Database schema name (optional)"
                                )
                        )
                ))
                .build();
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        String schema = (String) arguments.get("schema");

        try (Connection conn = server.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String catalog = conn.getCatalog();

            ResultSet rs = metaData.getTables(catalog, schema, "%", new String[]{"TABLE"});

            List<Map<String, Object>> tables = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> table = new LinkedHashMap<>();
                table.put("schema", rs.getString("TABLE_SCHEM"));
                table.put("name", rs.getString("TABLE_NAME"));
                table.put("type", rs.getString("TABLE_TYPE"));
                table.put("remarks", rs.getString("REMARKS"));
                tables.add(table);
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Database Tables (").append(tables.size()).append("):\n\n");
            for (Map<String, Object> table : tables) {
                sb.append("- ").append(table.get("name"));
                if (table.get("remarks") != null && !((String) table.get("remarks")).isEmpty()) {
                    sb.append(": ").append(table.get("remarks"));
                }
                sb.append("\n");
            }

            return ToolResult.text(sb.toString());
        } catch (SQLException e) {
            return ToolResult.error("Database Error: " + e.getMessage());
        }
    }
}
