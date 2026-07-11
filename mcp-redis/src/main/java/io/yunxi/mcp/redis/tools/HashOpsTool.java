package io.yunxi.mcp.redis.tools;

import io.yunxi.mcp.common.handler.ToolHandler;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class HashOpsTool implements ToolHandler {
    private final StringRedisTemplate redis;

    public HashOpsTool(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public String getName() { return "hash_ops"; }

    @Override
    public String getDescription() { return "Redis Hash operations: get, set, getAll, delete. Redis哈希操作。"; }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "action", Map.of("type", "string", "description", "Action: hget, hset, hgetall, hdel"),
                        "key", Map.of("type", "string", "description", "Hash key"),
                        "field", Map.of("type", "string", "description", "Hash field (for hget/hset/hdel)"),
                        "value", Map.of("type", "string", "description", "Value (for hset)")),
                "required", List.of("action", "key"));
    }

    @Override
    public String execute(Map<String, Object> args) {
        String action = (String) args.get("action");
        String key = (String) args.get("key");
        if (action == null || key == null) throw new RuntimeException("action and key are required");
        try {
            return switch (action) {
                case "hget" -> {
                    String field = (String) args.get("field");
                    if (field == null) throw new RuntimeException("field is required for hget");
                    String v = (String) redis.opsForHash().get(key, field);
                    yield v != null ? v : "(nil)";
                }
                case "hset" -> {
                    String field = (String) args.get("field");
                    String value = (String) args.get("value");
                    if (field == null || value == null) throw new RuntimeException("field and value required");
                    redis.opsForHash().put(key, field, value);
                    yield "OK";
                }
                case "hgetall" -> {
                    Map<Object, Object> entries = redis.opsForHash().entries(key);
                    StringBuilder sb = new StringBuilder("Hash " + key + ":\n");
                    entries.forEach((k, v) -> sb.append("  ").append(k).append(": ").append(v).append("\n"));
                    yield sb.toString();
                }
                case "hdel" -> {
                    String field = (String) args.get("field");
                    if (field == null) throw new RuntimeException("field is required for hdel");
                    Long n = redis.opsForHash().delete(key, field);
                    yield "Deleted " + n + " field(s)";
                }
                default -> throw new RuntimeException("Unknown action: " + action);
            };
        } catch (Exception e) {
            throw new RuntimeException("Redis operation failed: " + e.getMessage());
        }
    }
}
