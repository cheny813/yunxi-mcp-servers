package io.yunxi.mcp.redis.tools;

import io.yunxi.mcp.common.handler.ToolHandler;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class SetTool implements ToolHandler {
    private final StringRedisTemplate redis;

    public SetTool(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public String getName() { return "set"; }

    @Override
    public String getDescription() { return "Set a key-value pair in Redis with optional TTL. 设置Redis键值对，可选过期时间。"; }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "key", Map.of("type", "string", "description", "Redis key"),
                        "value", Map.of("type", "string", "description", "Value to set"),
                        "ttl", Map.of("type", "integer", "description", "TTL in seconds (optional)")),
                "required", List.of("key", "value"));
    }

    @Override
    public String execute(Map<String, Object> args) {
        String key = (String) args.get("key");
        String value = (String) args.get("value");
        if (key == null || value == null) throw new RuntimeException("key and value are required");
        try {
            if (args.containsKey("ttl") && args.get("ttl") != null) {
                long ttl = ((Number) args.get("ttl")).longValue();
                redis.opsForValue().set(key, value, ttl, TimeUnit.SECONDS);
                return "OK (TTL: " + ttl + "s)";
            } else {
                redis.opsForValue().set(key, value);
                return "OK";
            }
        } catch (Exception e) {
            throw new RuntimeException("Redis operation failed: " + e.getMessage());
        }
    }
}
