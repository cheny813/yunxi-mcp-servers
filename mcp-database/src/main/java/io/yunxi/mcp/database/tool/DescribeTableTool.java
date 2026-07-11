package io.yunxi.mcp.database.tool;

import io.yunxi.mcp.common.handler.ToolHandler;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/**
 * 描述表结构工具
 *
 * @author yunxi
 * @version 2.0.0
 * @since 1.0.0
 */
@Component
public class DescribeTableTool implements ToolHandler {

    private final DataSource dataSource;

    public DescribeTableTool(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public String getName() {
        return "describe_table";
    }

    @Override
    public String getDescription() {
        return "Get the structure of a specific table including columns, types, and constraints. " +
                "获取指定表的结构，包括列、类型和约束。 " +
                "Use this when you need to understand table structure, check column types, or verify constraints. " +
                "适用于理解表结构、检查列类型或验证约束等场景。 " +
                "Common use cases: schema inspection, query planning, data validation. " +
                "典型用例：模式检查、查询规划、数据验证。";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "table", Map.of(
                                "type", "string",
                                "description", "Table name to describe. Example: 'users', 'orders' | " +
                                        "要描述的表名。示例: 'users', 'orders'")),
                "required", List.of("table"));
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        String tableName = (String) arguments.get("table");

        if (tableName == null || tableName.isBlank()) {
            throw new RuntimeException("Table name is required");
        }

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String catalog = conn.getCatalog();

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

            ResultSet pkRs = metaData.getPrimaryKeys(catalog, null, tableName);
            List<String> primaryKeys = new ArrayList<>();
            while (pkRs.next()) {
                primaryKeys.add(pkRs.getString("COLUMN_NAME"));
            }

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

            return sb.toString();
        } catch (SQLException e) {
            throw new RuntimeException("Database Error: " + e.getMessage());
        }
    }
}