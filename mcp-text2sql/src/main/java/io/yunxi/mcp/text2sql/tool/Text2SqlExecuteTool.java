package io.yunxi.mcp.text2sql.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.text2sql.config.DatasourceRegistry;
import io.yunxi.mcp.text2sql.service.SessionRegistry;
import io.yunxi.mcp.text2sql.util.SqlGuard;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * text2sql_execute：在已注册数据源上执行只读 SQL（自动安全校验）。
 * <p>用于运行 text2sql_chat 产出的 SQL 并取数；护栏仅允许 SELECT/SHOW/DESCRIBE/EXPLAIN。</p>
 */
@Component
public class Text2SqlExecuteTool implements ToolHandler {

    private final DatasourceRegistry datasourceRegistry;
    private final SessionRegistry sessionRegistry;
    private final ObjectMapper mapper = new ObjectMapper();

    public Text2SqlExecuteTool(DatasourceRegistry datasourceRegistry, SessionRegistry sessionRegistry) {
        this.datasourceRegistry = datasourceRegistry;
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    public String getName() {
        return "text2sql_execute";
    }

    @Override
    public String getDescription() {
        return "在已注册数据源上执行只读 SQL（自动安全校验：仅允许 SELECT/SHOW/DESCRIBE/EXPLAIN，拦截写操作与注入）。 "
                + "Use this to run the SQL produced by text2sql_chat and return result rows. "
                + "适用于：验证生成的 SQL 并取数。";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "session_id", Map.of("type", "string",
                                "description", "text2sql_start 返回的会话 ID（用于确定数据源）；提供则忽略 db_id"),
                        "db_id", Map.of("type", "string",
                                "description", "数据源 ID，未提供 session_id 时使用，默认 'default'"),
                        "sql", Map.of("type", "string",
                                "description", "要执行的只读 SQL"),
                        "limit", Map.of("type", "integer",
                                "description", "最大返回行数（默认 100）", "default", 100)
                ),
                "required", List.of("sql"));
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        String sql = (String) arguments.get("sql");
        if (sql == null || sql.isBlank()) {
            throw new RuntimeException("sql 不能为空");
        }
        String sessionId = (String) arguments.get("session_id");
        String dbId;
        if (sessionId != null && !sessionId.isBlank()) {
            dbId = sessionRegistry.get(sessionId).dbId();
        } else {
            dbId = (String) arguments.getOrDefault("db_id", "default");
        }

        SqlGuard.Result guard = SqlGuard.validate(sql);
        if (!guard.isValid()) {
            throw new RuntimeException("安全校验失败: " + guard.getMessage());
        }
        int limit = arguments.containsKey("limit")
                ? ((Number) arguments.get("limit")).intValue() : 100;

        DataSource ds = datasourceRegistry.getDataSource(dbId);
        try (Connection conn = ds.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.setMaxRows(limit);
            stmt.setQueryTimeout(30);
            try (ResultSet rs = stmt.executeQuery(sql)) {
                List<Map<String, Object>> rows = new ArrayList<>();
                ResultSetMetaData md = rs.getMetaData();
                int cc = md.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= cc; i++) {
                        row.put(md.getColumnLabel(i), rs.getObject(i));
                    }
                    rows.add(row);
                }
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("db_id", dbId);
                out.put("row_count", rows.size());
                out.put("rows", rows);
                return mapper.writeValueAsString(out);
            }
        } catch (SQLException e) {
            throw new RuntimeException("SQL 执行失败: " + e.getMessage());
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
