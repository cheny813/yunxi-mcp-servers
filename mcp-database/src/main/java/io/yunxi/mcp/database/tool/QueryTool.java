package io.yunxi.mcp.database.tool;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import io.yunxi.mcp.database.config.DatabaseConfig;
import io.yunxi.mcp.database.config.DatabaseConfigService;
import io.yunxi.mcp.database.security.SqlSandbox;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/**
 * SQL 查询工具 - 支持多数据库
 * <p>
 * 工具名称: {@code query_db}
 * </p>
 * <p>
 * 执行 SQL 查询并返回结果，支持多数据库切换。
 * 仅允许 SELECT/SHOW/DESCRIBE/EXPLAIN 查询，确保数据安全。
 * </p>
 *
 * @author yunxi
 * @version 1.0.0
 * @since 1.0.0
 */
public class QueryTool implements ToolHandler {

    /**
     * 数据库配置服务
     * <p>
     * 提供多数据库配置管理和数据源获取功能。
     * </p>
     */
    private final DatabaseConfigService configService;

    public QueryTool(DatabaseConfigService configService) {
        this.configService = configService;
    }

    @Override
    public ToolDefinition getDefinition() {
        return ToolDefinition.builder()
                .name("query_db")
                .description(
                        "Execute a SQL query on a specific database and return results. " +
                                "执行SQL查询并返回结果。 " +
                                "Use this when you need to read data from database, perform analytics queries, or generate reports. "
                                +
                                "适用于从数据库读取数据、执行分析查询或生成报告等场景。 " +
                                "注意：仅允许 SELECT/SHOW/DESCRIBE/EXPLAIN 查询。")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "db_id", Map.of(
                                        "type", "string",
                                        "description",
                                        "Database ID to query. Use 'default' for the primary database. " +
                                                "要查询的数据库ID，使用'default'查询主数据库。"),
                                "sql", Map.of(
                                        "type", "string",
                                        "description", "SQL query to execute (SELECT/SHOW/DESCRIBE/EXPLAIN only). " +
                                                "要执行的SQL查询"),
                                "limit", Map.of(
                                        "type", "integer",
                                        "description", "Max rows to return (default: 100)",
                                        "default", 100)),
                        "required", List.of("sql")))
                .build();
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        String dbId = (String) arguments.getOrDefault("db_id", "default");
        String sql = (String) arguments.get("sql");
        int limit = arguments.containsKey("limit") ? ((Number) arguments.get("limit")).intValue() : 100;

        if (sql == null || sql.isBlank()) {
            return ToolResult.error("SQL query is required");
        }

        // 获取数据库配置进行沙箱校验
        DatabaseConfig dbConfig = configService.getConfig(dbId);
        if (dbConfig == null) {
            return ToolResult.error("Database config not found: " + dbId);
        }

        // 沙箱安全校验
        SqlSandbox.ValidationResult sandboxResult = SqlSandbox.validate(sql, dbConfig);
        if (!sandboxResult.isValid()) {
            return ToolResult.error("Security check failed: " + sandboxResult.getMessage());
        }

        try {
            DataSource dataSource = configService.getDataSource(dbId);
            if (dataSource == null) {
                return ToolResult.error("Database not found: " + dbId);
            }

            try (Connection conn = dataSource.getConnection();
                    Statement stmt = conn.createStatement()) {

                // 应用行数限制（取配置和用户指定的最小值）
                int maxRows = Math.min(limit, dbConfig.getMaxRowsLimit());
                stmt.setMaxRows(maxRows);
                stmt.setQueryTimeout(dbConfig.getQueryTimeoutSeconds());
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
                result.put("dbId", dbId);
                result.put("rowCount", rows.size());
                result.put("columns", getColumnNames(metaData));
                result.put("rows", rows);

                return ToolResult.text(formatResult(result));
            }
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
        sb.append("Database: ").append(result.get("dbId")).append("\n");
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