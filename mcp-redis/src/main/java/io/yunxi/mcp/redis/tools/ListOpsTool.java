package io.yunxi.mcp.redis.tools;

import io.yunxi.mcp.common.handler.ToolHandler;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ListOpsTool implements ToolHandler {
    private final StringRedisTemplate redis;

    public ListOpsTool(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public String getName() { return "list_ops"; }

    @Override
    public String getDescription() { return "Redis List operations: push, pop, range. Redis列表操作。"; }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "action", Map.of("type", "string", "description", "Action: lpush, rpush, lpop, rpop, lrange"),
                        "key", Map.of("type", "string", "description", "List key"),
                        "value", Map.of("type", "string", "description", "Value (for push)"),
                        "start", Map.of("type", "integer", "description", "Start index (for range)"),
                        "stop", Map.of("type", "integer", "description", "Stop index (for range, default: -1)")),
                "required", List.of("action", "key"));
    }

    @Override
    public String execute(Map<String, Object> args) {
        String action = (String) args.get("action");
        String key = (String) args.get("key");
        if (action == null || key == null) throw new RuntimeException("action and key are required");
        try {
            return switch (action) {
                case "lpush" -> {
                    String val = (String) args.get("value");
                    Long n = redis.opsForList().leftPush(key, val);
                    yield "LPUSH " + key + " -> length: " + n;
                }
                case "rpush" -> {
                    String val = (String) args.get("value");
                    Long n = redis.opsForList().rightPush(key, val);
                    yield "RPUSH " + key + " -> length: " + n;
                }
                case "lpop" -> {
                    String v = redis.opsForList().leftPop(key);
                    yield v != null ? v : "(nil)";
                }
                case "rpop" -> {
                    String v = redis.opsForList().rightPop(key);
                    yield v != null ? v : "(nil)";
                }
                case "lrange" -> {
                    long start = args.containsKey("start") ? ((Number) args.get("start")).longValue() : 0;
                    long stop = args.containsKey("stop") ? ((Number) args.get("stop")).longValue() : -1;
                    List<String> items = redis.opsForList().range(key, start, stop);
                    yield items != null ? String.join("\n", items) : "(empty)";
                }
                default -> throw new RuntimeException("Unknown action: " + action);
            };
        } catch (Exception e) {
            throw new RuntimeException("Redis operation failed: " + e.getMessage());
        }
    }
}
