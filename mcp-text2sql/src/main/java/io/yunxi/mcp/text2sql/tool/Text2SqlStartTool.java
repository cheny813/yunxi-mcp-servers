package io.yunxi.mcp.text2sql.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.text2sql.config.DatasourceRegistry;
import io.yunxi.mcp.text2sql.config.Text2SqlProperties;
import io.yunxi.mcp.text2sql.model.SchemaSnapshot;
import io.yunxi.mcp.text2sql.service.SchemaService;
import io.yunxi.mcp.text2sql.service.SessionRegistry;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * text2sql_start：初始化 Text2SQL 会话。
 * <p>对应 Focus_MCP_SQL 的 {@code gptText2sqlStart}，但改为自动 JDBC 内省 schema，无需手工提交 JSON，
 * 也不需 DataFocus Bearer Token。</p>
 */
@Component
public class Text2SqlStartTool implements ToolHandler {

    private final SchemaService schemaService;
    private final SessionRegistry sessionRegistry;
    private final DatasourceRegistry datasourceRegistry;
    private final Text2SqlProperties properties;
    private final ObjectMapper mapper = new ObjectMapper();

    public Text2SqlStartTool(SchemaService schemaService, SessionRegistry sessionRegistry,
                             DatasourceRegistry datasourceRegistry, Text2SqlProperties properties) {
        this.schemaService = schemaService;
        this.sessionRegistry = sessionRegistry;
        this.datasourceRegistry = datasourceRegistry;
        this.properties = properties;
    }

    @Override
    public String getName() {
        return "text2sql_start";
    }

    @Override
    public String getDescription() {
        return "初始化 Text2SQL 会话：连接数据源并自动内省数据库表结构（表名、列名、类型），"
                + "建立后续自然语言转 SQL 的上下文。替代需手工注册 schema 的传统做法。 "
                + "Use this before text2sql_chat when you need to convert natural-language questions into SQL. "
                + "适用于：先建立数据库上下文，再多次提问生成 SQL 的场景。";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "db_id", Map.of("type", "string",
                                "description", "数据源 ID，对应 text2sql.datasources 配置项。默认 'default'。"),
                        "language", Map.of("type", "string",
                                "description", "自然语言语种：'chinese' 或 'english'，默认取 text2sql.default-language。",
                                "enum", List.of("chinese", "english"))
                ),
                "required", List.of());
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        String dbId = (String) arguments.getOrDefault("db_id", "default");
        if (!datasourceRegistry.contains(dbId)) {
            throw new RuntimeException("未配置的数据源: " + dbId);
        }
        String language = (String) arguments.getOrDefault("language", properties.getDefaultLanguage());
        String dialect = detectDialect(dbId);
        SchemaSnapshot schema = schemaService.introspect(dbId);
        String sessionId = sessionRegistry.create(dbId, language, dialect, schema);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("session_id", sessionId);
        out.put("db_id", dbId);
        out.put("dialect", dialect);
        out.put("table_count", schema.tables().size());
        out.put("tables", schema.tables().stream().map(SchemaSnapshot.TableInfo::name).toList());
        try {
            return mapper.writeValueAsString(out);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String detectDialect(String dbId) {
        String url = properties.getDatasources().get(dbId).getUrl().toLowerCase();
        if (url.contains("postgres")) {
            return "PostgreSQL";
        }
        if (url.contains("mysql")) {
            return "MySQL";
        }
        if (url.contains("sqlserver")) {
            return "SQL Server";
        }
        return "Generic SQL";
    }
}
