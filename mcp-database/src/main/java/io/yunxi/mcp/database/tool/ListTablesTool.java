package io.yunxi.mcp.database.tool;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/**
 * 列出数据库表工具
 * <p>
 * 工具名称: {@code list_tables}
 * </p>
 * <p>
 * 列出当前数据库中的所有表，支持按 schema 过滤。
 * 用于探索数据库结构、发现可用表或理解数据模型。
 * </p>
 *
 * @author yunxi
 * @version 1.0.0
 * @since 1.0.0
 */
public class ListTablesTool implements ToolHandler {

    /**
     * 数据源
     * <p>
     * 用于获取数据库连接和元数据信息。
     * </p>
     */
    private final DataSource dataSource;

    public ListTablesTool(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public ToolDefinition getDefinition() {
        return ToolDefinition.builder()
                .name("list_tables")
                .description(
                        "List all tables in the database. " +
                                "列出数据库中的所有表。 " +
                                "Use this when you need to explore database structure, discover available tables, or understand the data model. "
                                +
                                "适用于探索数据库结构、发现可用表或理解数据模型等场景。 " +
                                "Common use cases: database exploration, schema discovery, documentation. " +
                                "典型用例：数据库探索、模式发现、文档编写。")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "schema", Map.of(
                                        "type", "string",
                                        "description", "Database schema name (optional). Example: 'public', 'dbo' | " +
                                                "数据库模式名称（可选）。示例: 'public', 'dbo'"))))
                .build();
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        String schema = (String) arguments.get("schema");

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String catalog = conn.getCatalog();

            ResultSet rs = metaData.getTables(catalog, schema, "%", new String[] { "TABLE" });

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
