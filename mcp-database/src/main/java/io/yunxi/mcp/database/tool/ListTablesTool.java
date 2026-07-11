package io.yunxi.mcp.database.tool;

import io.yunxi.mcp.common.handler.ToolHandler;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/**
 * 列出数据库表工具
 *
 * @author yunxi
 * @version 2.0.0
 * @since 1.0.0
 */
@Component
public class ListTablesTool implements ToolHandler {

    private final DataSource dataSource;

    public ListTablesTool(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public String getName() {
        return "list_tables";
    }

    @Override
    public String getDescription() {
        return "List all tables in the database. " +
                "列出数据库中的所有表。 " +
                "Use this when you need to explore database structure, discover available tables, or understand the data model. " +
                "适用于探索数据库结构、发现可用表或理解数据模型等场景。 " +
                "Common use cases: database exploration, schema discovery, documentation. " +
                "典型用例：数据库探索、模式发现、文档编写。";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "schema", Map.of(
                                "type", "string",
                                "description", "Database schema name (optional). Example: 'public', 'dbo' | " +
                                        "数据库模式名称（可选）。示例: 'public', 'dbo'")));
    }

    @Override
    public String execute(Map<String, Object> arguments) {
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

            return sb.toString();
        } catch (SQLException e) {
            throw new RuntimeException("Database Error: " + e.getMessage());
        }
    }
}