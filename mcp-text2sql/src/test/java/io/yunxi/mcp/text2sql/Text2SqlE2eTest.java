package io.yunxi.mcp.text2sql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.yunxi.mcp.text2sql.tool.Text2SqlChatTool;
import io.yunxi.mcp.text2sql.tool.Text2SqlExecuteTool;
import io.yunxi.mcp.text2sql.tool.Text2SqlStartTool;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.List;
import java.util.Map;

/**
 * 端到端测试：真实连接数据源 + 真实 LLM，跑通 text2sql_start → text2sql_chat → text2sql_execute 全链路。
 * <p>默认不运行（避免无外部依赖时 CI 失败）。显式开启：</p>
 * <pre>
 * mvn -pl mcp-text2sql -am test -Dtest=Text2SqlE2eTest -Dtext2sql.e2e=true \
 *   -DLLM_API_KEY=... -DDB_HOST=... -DDB_NAME=... -DDB_USERNAME=... -DDB_PASSWORD=...
 * </pre>
 */
@SpringBootTest
@EnabledIfSystemProperty(named = "text2sql.e2e", matches = "true")
class Text2SqlE2eTest {

    @Autowired
    Text2SqlStartTool startTool;
    @Autowired
    Text2SqlChatTool chatTool;
    @Autowired
    Text2SqlExecuteTool executeTool;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void endToEndAgainstRealDb() throws Exception {
        String startJson = startTool.execute(Map.of("db_id", "default", "language", "chinese"));
        System.out.println("=== text2sql_start ===\n" + startJson);
        JsonNode start = mapper.readTree(startJson);
        String sessionId = start.get("session_id").asText();
        List<String> tables = mapper.convertValue(start.get("tables"), List.class);
        if (tables == null || tables.isEmpty()) {
            throw new IllegalStateException("数据库内省未返回任何表，无法继续端到端");
        }
        String firstTable = tables.get(0);
        System.out.println("选择首张表进行验证: " + firstTable);

        String question = "表 " + firstTable + " 一共有多少条记录？";
        String chatJson = chatTool.execute(Map.of("session_id", sessionId, "question", question));
        System.out.println("=== text2sql_chat ===\n" + chatJson);
        JsonNode chat = mapper.readTree(chatJson);
        String sql = chat.get("sql").asText();
        System.out.println("=== generated SQL ===\n" + sql);

        String execJson = executeTool.execute(Map.of("session_id", sessionId, "sql", sql, "limit", 50));
        System.out.println("=== text2sql_execute ===\n" + execJson);
    }
}
