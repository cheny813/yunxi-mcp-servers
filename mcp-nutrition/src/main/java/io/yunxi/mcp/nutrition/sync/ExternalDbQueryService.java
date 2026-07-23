package io.yunxi.mcp.nutrition.sync;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.yunxi.mcp.nutrition.sync.config.SyncDatasourceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

/**
 * 外部数据库查询服务
 * <p>
 * 支持两种查询方式：
 * <ul>
 *   <li>{@link #queryByDsId(String, String, int)} — 通过数据源 id 查（default 复用 Spring 连接池）</li>
 *   <li>{@link #query(String, String, String, String, int, int)} — 通过原始 JDBC 参数查（兼容旧调用）</li>
 * </ul>
 * 连接信息来自 sync.sources 命名注册表，不再散落在多处配置。
 * </p>
 *
 * @author yunxi-agent-platform
 * @version 2.1.0
 */
@Service
public class ExternalDbQueryService {

    private static final Logger log = LoggerFactory.getLogger(ExternalDbQueryService.class);
    private final Gson gson = new Gson();

    private final SyncDatasourceRegistry datasourceRegistry;

    public ExternalDbQueryService(SyncDatasourceRegistry datasourceRegistry) {
        this.datasourceRegistry = datasourceRegistry;
    }

    @Value("${sync.query-timeout-seconds:30}")
    private int queryTimeoutSeconds;

    /**
     * 通过数据源 id 执行查询（推荐方式）。default 复用 Spring 连接池，其他按需建池并缓存。
     */
    public String queryByDsId(String dsId, String sql, int limit) {
        DataSource ds = datasourceRegistry.getDataSource(dsId);
        return queryWithDataSource(ds, sql, limit, queryTimeoutSeconds);
    }

    /**
     * 执行外部库查询（无限制行数）。
     */
    public String query(String jdbcUrl, String username, String password, String sql) {
        return query(jdbcUrl, username, password, sql, 0, queryTimeoutSeconds);
    }

    /**
     * 执行外部库查询（带行数限制）。
     */
    public String query(String jdbcUrl, String username, String password, String sql, int limit) {
        return query(jdbcUrl, username, password, sql, limit, queryTimeoutSeconds);
    }

    /**
     * 执行外部库查询（完整参数，每次调用创建临时连接池，适用于非 default 数据源的一次性查询）。
     */
    public String query(String jdbcUrl, String username, String password, String sql, int limit, int timeoutSeconds) {
        HikariConfig config = buildHikariConfig(jdbcUrl, username, password, timeoutSeconds);
        try (HikariDataSource dataSource = new HikariDataSource(config)) {
            return queryWithDataSource(dataSource, sql, limit, timeoutSeconds);
        }
    }

    private String queryWithDataSource(DataSource dataSource, String sql, int limit, int timeoutSeconds) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.setQueryTimeout(timeoutSeconds);
            if (limit > 0) {
                stmt.setMaxRows(limit);
            }
            boolean hasResult = stmt.execute(sql);
            if (hasResult) {
                try (ResultSet rs = stmt.getResultSet()) {
                    return resultSetToJson(rs);
                }
            } else {
                int updateCount = stmt.getUpdateCount();
                JsonObject result = new JsonObject();
                result.addProperty("updated", updateCount);
                return gson.toJson(result);
            }
        } catch (SQLException e) {
            log.error("外部数据库查询失败: {}", sql, e);
            JsonObject error = new JsonObject();
            error.addProperty("error", e.getMessage());
            return gson.toJson(error);
        }
    }

    private HikariConfig buildHikariConfig(String jdbcUrl, String username, String password, int timeoutSeconds) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMaximumPoolSize(2);
        config.setConnectionTimeout(5000);
        config.setIdleTimeout(30000);
        config.setMaxLifetime(30000);
        config.setPoolName("sync-external-db");
        config.addDataSourceProperty("connectTimeout", "5000");
        config.addDataSourceProperty("socketTimeout", String.valueOf(timeoutSeconds * 1000));
        return config;
    }

    /**
     * 将 ResultSet 转为 JSON 数组。
     */
    private String resultSetToJson(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        JsonArray jsonArray = new JsonArray();
        while (rs.next()) {
            JsonObject row = new JsonObject();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnLabel(i);
                Object value = rs.getObject(i);
                if (value == null) {
                    row.add(columnName, null);
                } else if (value instanceof Number) {
                    row.addProperty(columnName, (Number) value);
                } else if (value instanceof Boolean) {
                    row.addProperty(columnName, (Boolean) value);
                } else {
                    row.addProperty(columnName, value.toString());
                }
            }
            jsonArray.add(row);
        }
        return gson.toJson(jsonArray);
    }
}
