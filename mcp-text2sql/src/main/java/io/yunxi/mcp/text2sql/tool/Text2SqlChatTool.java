package io.yunxi.mcp.text2sql.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.text2sql.model.SchemaSnapshot;
import io.yunxi.mcp.text2sql.service.LlmClient;
import io.yunxi.mcp.text2sql.service.SessionRegistry;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * text2sql_chat：两步法自然语言转 SQL（Focus_MCP_SQL 设计哲学）。
 * <p>第一步：让 LLM 从已注册 schema 中做表/列关联定位（确定性更强，降低幻觉）；
 * 第二步：仅基于定位到的表/列生成单条只读 SQL。</p>
 */
@Component
public class Text2SqlChatTool implements ToolHandler {

    private final LlmClient llm;
    private final SessionRegistry sessionRegistry;
    private final ObjectMapper mapper = new ObjectMapper();

    public Text2SqlChatTool(LlmClient llm, SessionRegistry sessionRegistry) {
        this.llm = llm;
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    public String getName() {
        return "text2sql_chat";
    }

    @Override
    public String getDescription() {
        return "将自然语言问题转换为 SQL（两步法：先做表/列关联定位，再生成 SQL），降低大模型幻觉。 "
                + "需先调用 text2sql_start 获取 session_id。 "
                + "Use this when you have a natural-language question about the database registered in a session. "
                + "适用于：基于已初始化会话反复提问生成 SQL 的场景。";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "session_id", Map.of("type", "string",
                                "description", "text2sql_start 返回的会话 ID"),
                        "question", Map.of("type", "string",
                                "description", "自然语言问题，例如 '年龄最大的人是谁'")
                ),
                "required", List.of("session_id", "question"));
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        String sessionId = (String) arguments.get("session_id");
        String question = (String) arguments.get("question");
        if (question == null || question.isBlank()) {
            throw new RuntimeException("question 不能为空");
        }
        SessionRegistry.Session session = sessionRegistry.get(sessionId);
        SchemaSnapshot schema = session.schema();

        // 第一步：schema 关联定位
        String schemaJson = schemaToJson(schema);
        String linkingPrompt = "数据库表结构如下(JSON):\n" + schemaJson + "\n\n用户问题: " + question
                + "\n请从中挑选出回答该问题所需的表与列。仅返回 JSON: "
                + "{\"tables\":[\"表名\"],\"columns\":[\"表名.列名\"],\"reason\":\"简要说明\"}。";
        String linkingRaw = llm.chat(
                "你是数据库 schema 关联分析专家，只做表/列定位，不生成 SQL。", linkingPrompt);
        JsonNode linking = parseJson(linkingRaw);
        List<String> tables = new ArrayList<>();
        linking.path("tables").forEach(n -> tables.add(n.asText()));
        List<String> columns = new ArrayList<>();
        linking.path("columns").forEach(n -> columns.add(n.asText()));

        // 第二步：基于定位生成 SQL
        String sqlPrompt = "数据库类型: " + session.dialect() + "。\n可使用表/列(来自关联定位):\n"
                + "tables=" + tables + "\ncolumns=" + columns
                + "\n完整结构(JSON)供参考:\n" + schemaJson
                + "\n\n用户问题: " + question
                + "\n请生成一条【只读】SQL 回答该问题。仅返回 JSON: "
                + "{\"sql\":\"...\",\"explanation\":\"简要说明\"}。不要使用任何写操作。";
        String sqlRaw = llm.chat(
                "你是资深 SQL 工程师，只生成单条只读 SQL（SELECT/SHOW/DESCRIBE/EXPLAIN），"
                        + "表名与列名必须来自给定结构。", sqlPrompt);
        JsonNode sqlNode = parseJson(sqlRaw);
        String sql = sqlNode.path("sql").asText("");
        String explanation = sqlNode.path("explanation").asText("");

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("session_id", sessionId);
        out.put("linked_tables", tables);
        out.put("linked_columns", columns);
        out.put("sql", sql);
        out.put("explanation", explanation);
        try {
            return mapper.writeValueAsString(out);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String schemaToJson(SchemaSnapshot schema) {
        try {
            return mapper.writeValueAsString(schema);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonNode parseJson(String raw) {
        try {
            String s = raw.trim();
            int a = s.indexOf('{');
            int b = s.lastIndexOf('}');
            if (a >= 0 && b > a) {
                s = s.substring(a, b + 1);
            }
            return mapper.readTree(s);
        } catch (Exception e) {
            throw new RuntimeException("LLM 返回无法解析为 JSON: " + raw);
        }
    }
}
