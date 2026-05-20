package io.yunxi.mcp.database.tool;

import io.yunxi.mcp.database.DatabaseMcpServer;
import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;

import java.sql.*;
import java.util.*;

/**
 * 描述表结构工具
 */
public class DescribeTableTool implements ToolHandler {

    private final DatabaseMcpServer server;

    public DescribeTableTool(DatabaseMcpServer server) {
        this.server = server;
    }

    @Override
    public ToolDefinition getDefinition() {
        return ToolDefinition.builder()
                .name("describe_table")
                .description("Get the structure of a specific table including columns, types, and constraints")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "table", Map.of(
                                        "type", "string",
                                        "description", "Table name to describe")),
                        "required", List.of("table")))
                .build();
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        String tableName = (String) arguments.get("table");

        if (tableName == null || tableName.isBlank()) {
            return ToolResult.error("Table name is required");
        }

        try (Connection conn = server.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String catalog = conn.getCatalog();

            // 获取列信息
            ResultSet columnsRs = metaData.getColumns(catalog, null, tableName, null);
            List<Map<String, Object>> columns = new ArrayList<>();

            while (columnsRs.next()) {
                Map<String, Object> column = new LinkedHashMap<>();
                column.put("name", columnsRs.getString("COLUMN_NAME"));
                column.put("type", columnsRs.getString("TYPE_NAME"));
                column.put("size", columnsRs.getInt("COLUMN_SIZE"));
                column.put("nullable", columnsRs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                column.put("defaultValue", columnsRs.getString("COLUMN_DEF"));
                column.put("remarks", columnsRs.getString("REMARKS"));
                columns.add(column);
            }

            // 获取主键
            ResultSet pkRs = metaData.getPrimaryKeys(catalog, null, tableName);
            List<String> primaryKeys = new ArrayList<>();
            while (pkRs.next()) {
                primaryKeys.add(pkRs.getString("COLUMN_NAME"));
            }

            // 格式化输出
            StringBuilder sb = new StringBuilder();
            sb.append("Table: ").append(tableName).append("\n");
            sb.append("Columns (").append(columns.size()).append("):\n\n");

            for (Map<String, Object> col : columns) {
                String pk = primaryKeys.contains(col.get("name")) ? " [PK]" : "";
                String nullable = (Boolean) col.get("nullable") ? "NULL" : "NOT NULL";
                sb.append("- ").append(col.get("name"))
                        .append(": ").append(col.get("type"))
                        .append("(").append(col.get("size")).append(")")
                        .append(" ").append(nullable)
                        .append(pk).append("\n");

                if (col.get("remarks") != null && !((String) col.get("remarks")).isEmpty()) {
                    sb.append("  Comment: ").append(col.get("remarks")).append("\n");
                }
            }

            return ToolResult.text(sb.toString());
        } catch (SQLException e) {
            return ToolResult.error("Database Error: " + e.getMessage());
        }
    }
}
