package io.yunxi.mcp.redis.tools;

import io.yunxi.mcp.common.handler.ToolHandler;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class KeysTool implements ToolHandler {
    private final StringRedisTemplate redis;

    public KeysTool(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public String getName() { return "keys"; }

    @Override
    public String getDescription() { return "List Redis keys matching a pattern. 列出匹配模式的Redis键。"; }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "pattern", Map.of("type", "string", "description", "Key pattern, e.g. 'user:*' (default: *)")),
                "required", List.of());
    }

    @Override
    public String execute(Map<String, Object> args) {
        String pattern = (String) args.getOrDefault("pattern", "*");
        try {
            Set<String> keys = redis.keys(pattern);
            if (keys.isEmpty()) return "(empty)";
            StringBuilder sb = new StringBuilder("Keys matching '" + pattern + "':\n");
            keys.forEach(k -> sb.append("  - ").append(k).append("\n"));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Redis operation failed: " + e.getMessage());
        }
    }
}
