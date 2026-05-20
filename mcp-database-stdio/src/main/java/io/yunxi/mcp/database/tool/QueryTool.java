package io.yunxi.mcp.database.tool;

import io.yunxi.mcp.database.DatabaseMcpServer;
import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;

import java.sql.*;
import java.util.*;

/**
 * SQL 查询工具
 * <p>
 * 执行 SQL 查询并返回结果
 * </p>
 */
public class QueryTool implements ToolHandler {

    private final DatabaseMcpServer server;

    public QueryTool(DatabaseMcpServer server) {
        this.server = server;
    }

    @Override
    public ToolDefinition getDefinition() {
        return ToolDefinition.builder()
                .name("query")
                .description("Execute a SQL query and return results. Use for SELECT queries.")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "sql", Map.of(
                                        "type", "string",
                                        "description", "SQL query to execute (SELECT statements only)"
                                ),
                                "limit", Map.of(
                                        "type", "integer",
                                        "description", "Maximum number of rows to return (default: 100)",
                                        "default", 100
                                )
                        ),
                        "required", List.of("sql")
                ))
                .build();
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        String sql = (String) arguments.get("sql");
        int limit = arguments.containsKey("limit") ? ((Number) arguments.get("limit")).intValue() : 100;

        if (sql == null || sql.isBlank()) {
            return ToolResult.error("SQL query is required");
        }

        // 安全检查：只允许 SELECT 语句
        String trimmedSql = sql.trim().toUpperCase();
        if (!trimmedSql.startsWith("SELECT") && !trimmedSql.startsWith("SHOW") && !trimmedSql.startsWith("DESCRIBE") && !trimmedSql.startsWith("EXPLAIN")) {
            return ToolResult.error("Only SELECT, SHOW, DESCRIBE, EXPLAIN queries are allowed");
        }

        try (Connection conn = server.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.setMaxRows(limit);
            ResultSet rs = stmt.executeQuery(sql);

            List<Map<String, Object>> rows = new ArrayList<>();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                }
                rows.add(row);
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("rowCount", rows.size());
            result.put("columns", getColumnNames(metaData));
            result.put("rows", rows);

            return ToolResult.text(formatResult(result));
        } catch (SQLException e) {
            return ToolResult.error("SQL Error: " + e.getMessage());
        }
    }

    private List<String> getColumnNames(ResultSetMetaData metaData) throws SQLException {
        List<String> columns = new ArrayList<>();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            columns.add(metaData.getColumnLabel(i));
        }
        return columns;
    }

    private String formatResult(Map<String, Object> result) {
        StringBuilder sb = new StringBuilder();
        sb.append("Query Result:\n");
        sb.append("- Rows: ").append(result.get("rowCount")).append("\n");
        sb.append("- Columns: ").append(result.get("columns")).append("\n\n");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) result.get("rows");
        for (int i = 0; i < rows.size(); i++) {
            sb.append("Row ").append(i + 1).append(": ").append(rows.get(i)).append("\n");
        }

        return sb.toString();
    }
}
