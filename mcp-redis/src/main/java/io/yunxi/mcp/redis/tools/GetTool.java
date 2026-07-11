package io.yunxi.mcp.redis.tools;

import io.yunxi.mcp.common.handler.ToolHandler;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GetTool implements ToolHandler {
    private final StringRedisTemplate redis;

    public GetTool(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public String getName() { return "get"; }

    @Override
    public String getDescription() {
        return "Retrieve the value of a key from Redis. 从Redis获取指定键的值。";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "key", Map.of("type", "string", "description", "Redis key to retrieve")),
                "required", List.of("key"));
    }

    @Override
    public String execute(Map<String, Object> args) {
        String key = (String) args.get("key");
        if (key == null) throw new RuntimeException("key is required");
        try {
            String value = redis.opsForValue().get(key);
            return value != null ? value : "(nil)";
        } catch (Exception e) {
            throw new RuntimeException("Redis operation failed: " + e.getMessage());
        }
    }
}
