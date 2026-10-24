package io.yunxi.mcp.text2sql.service;

import io.yunxi.mcp.text2sql.model.SchemaSnapshot;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Text2SQL 会话注册表：text2sql_start 创建会话，text2sql_chat/execute 复用其 schema 上下文。
 * <p>对应 Focus_MCP_SQL 的 chatId 会话机制，但不再需要远端模型注册。</p>
 */
@Service
public class SessionRegistry {

    private final ConcurrentMap<String, Session> sessions = new ConcurrentHashMap<>();

    public String create(String dbId, String language, String dialect, SchemaSnapshot schema) {
        String id = UUID.randomUUID().toString().replace("-", "");
        sessions.put(id, new Session(dbId, language, dialect, schema));
        return id;
    }

    public Session get(String sessionId) {
        Session s = sessions.get(sessionId);
        if (s == null) {
            throw new RuntimeException("无效的会话 ID（请先调用 text2sql_start 初始化）: " + sessionId);
        }
        return s;
    }

    public record Session(String dbId, String language, String dialect, SchemaSnapshot schema) {
    }
}
