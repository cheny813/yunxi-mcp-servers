package io.yunxi.mcp.text2sql.service;

import io.yunxi.mcp.text2sql.config.DatasourceRegistry;
import io.yunxi.mcp.text2sql.model.SchemaSnapshot;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 通过 JDBC {@link DatabaseMetaData} 自动内省数据库表结构。
 * <p>相比 Focus_MCP_SQL 需要手工提交 schema JSON，本服务直接连接数据库内省，使用更顺畅。</p>
 */
@Service
public class SchemaService {

    private final DatasourceRegistry datasourceRegistry;

    public SchemaService(DatasourceRegistry datasourceRegistry) {
        this.datasourceRegistry = datasourceRegistry;
    }

    public SchemaSnapshot introspect(String dbId) {
        DataSource ds = datasourceRegistry.getDataSource(dbId);
        try (Connection conn = ds.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            String catalog = conn.getCatalog();

            List<String> tableNames = new ArrayList<>();
            try (ResultSet tables = meta.getTables(catalog, null, "%", new String[]{"TABLE"})) {
                while (tables.next()) {
                    tableNames.add(tables.getString("TABLE_NAME"));
                }
            }

            List<SchemaSnapshot.TableInfo> tableInfos = new ArrayList<>();
            for (String tableName : tableNames) {
                List<SchemaSnapshot.ColumnInfo> columns = new ArrayList<>();
                try (ResultSet cols = meta.getColumns(catalog, null, tableName, "%")) {
                    while (cols.next()) {
                        String colName = cols.getString("COLUMN_NAME");
                        String type = cols.getString("TYPE_NAME");
                        boolean nullable = "YES".equalsIgnoreCase(cols.getString("IS_NULLABLE"));
                        columns.add(new SchemaSnapshot.ColumnInfo(colName, type, nullable));
                    }
                }
                tableInfos.add(new SchemaSnapshot.TableInfo(tableName, columns));
            }
            return new SchemaSnapshot(tableInfos);
        } catch (SQLException e) {
            throw new RuntimeException("Schema 内省失败: " + e.getMessage(), e);
        }
    }
}
