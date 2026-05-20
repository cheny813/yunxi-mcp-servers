package io.yunxi.mcp.redis.tools;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redis SET 命令工具
 * <p>
 * 工具名称: {@code redis_set}
 * </p>
 * <p>
 * 设置 Redis 键值对，可选设置过期时间（TTL）。
 * </p>
 *
 * @author yunxi
 * @version 1.0.0
 * @since 1.0.0
 */
public class SetTool implements ToolHandler {
    /**
     * Redis 操作模板
     * <p>
     * Spring Data Redis 提供的 String 类型 Redis 操作工具类。
     * </p>
     */
    private final StringRedisTemplate redis;

    /**
     * 构造函数
     *
     * @param redis Redis 操作模板实例
     */
    public SetTool(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * 获取工具定义
     *
     * @return 工具定义对象
     */
    @Override
    public ToolDefinition getDefinition() {
        return ToolDefinition.builder()
                .name("set")
                .description(
                        "Set key-value pair in Redis with optional TTL. " +
                                "设置Redis键值对，支持设置过期时间。 " +
                                "Use this when you need to store data for caching, session management, or temporary data storage. "
                                +
                                "适用于数据缓存、会话管理、临时数据存储等场景。 " +
                                "Common use cases: cache storage, user sessions, temporary data. " +
                                "典型用例：缓存存储、用户会话、临时数据。")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "key", Map.of(
                                        "type", "string",
                                        "description",
                                        "The Redis key to set. Example: 'user:001' | Redis键名。示例: 'user:001', 'session:abc123'"),
                                "value", Map.of(
                                        "type", "string",
                                        "description",
                                        "The value to store. Can be JSON string or plain text. | 要存储的值，支持JSON字符串或纯文本"),
                                "ttl", Map.of(
                                        "type", "integer",
                                        "description",
                                        "Time-to-live in seconds. Example: 3600 (1 hour) | 过期时间（秒）。示例: 3600 (1小时)")),
                        "required", List.of("key", "value")))
                .build();
    }

    /**
     * 执行 SET 命令
     *
     * @param args 参数 Map：
     *             <ul>
     *             <li>key (必填) - Redis 键</li>
     *             <li>value (必填) - 值</li>
     *             <li>ttl (可选) - 过期时间（秒）</li>
     *             </ul>
     * @return 工具执行结果，OK 或 OK (TTL: Xs)
     */
    @Override
    public ToolResult execute(Map<String, Object> args) {
        String key = (String) args.get("key");
        String value = (String) args.get("value");

        if (key == null || value == null) {
            return ToolResult.error("key 和 value 参数不能为空");
        }

        Integer ttl = args.containsKey("ttl") ? ((Number) args.get("ttl")).intValue() : null;

        try {
            // 如果设置了 TTL 且大于 0，则设置过期时间
            if (ttl != null && ttl > 0) {
                redis.opsForValue().set(key, value, ttl, TimeUnit.SECONDS);
                return ToolResult.text("OK (TTL: " + ttl + "s)");
            }
            redis.opsForValue().set(key, value);
            return ToolResult.text("OK");
        } catch (Exception e) {
            return ToolResult.error("Redis 操作失败: " + e.getMessage());
        }
    }
}
