package io.yunxi.mcp.redis.tools;

import io.yunxi.mcp.common.handler.ToolHandler;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DeleteTool implements ToolHandler {
    private final StringRedisTemplate redis;

    public DeleteTool(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public String getName() { return "delete"; }

    @Override
    public String getDescription() { return "Delete one or more keys from Redis. 从Redis删除一个或多个键。"; }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "key", Map.of("type", "string", "description", "Redis key to delete (comma-separated for multiple)")),
                "required", List.of("key"));
    }

    @Override
    public String execute(Map<String, Object> args) {
        String key = (String) args.get("key");
        if (key == null) throw new RuntimeException("key is required");
        try {
            String[] keys = key.split(",");
            Long deleted = redis.delete(List.of(keys));
            return "Deleted " + deleted + " key(s)";
        } catch (Exception e) {
            throw new RuntimeException("Redis operation failed: " + e.getMessage());
        }
    }
}
