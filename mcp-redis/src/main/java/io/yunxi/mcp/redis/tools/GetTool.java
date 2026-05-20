package io.yunxi.mcp.redis.tools;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Map;

/**
 * Redis GET 命令工具
 * <p>
 * 工具名称: {@code redis_get}
 * </p>
 * <p>
 * 获取 Redis 中指定键的值。
 * </p>
 *
 * @author yunxi
 * @version 1.0.0
 * @since 1.0.0
 */
public class GetTool implements ToolHandler {
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
    public GetTool(StringRedisTemplate redis) {
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
                .name("get")
                .description(
                        "Retrieve the value of a key from Redis. " +
                                "从Redis获取指定键的值。 " +
                                "Use this when you need to read cached data, fetch session information, or get stored configuration values. "
                                +
                                "适用于读取缓存数据、获取会话信息、读取存储的配置值等场景。 " +
                                "Common use cases: reading cache, fetching user sessions, getting config values. " +
                                "典型用例：读取缓存、获取用户会话、读取配置值。")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "key", Map.of(
                                        "type", "string",
                                        "description",
                                        "The Redis key to retrieve. Example: 'user:001' | Redis键名。示例: 'user:001', 'session:abc123'")),
                        "required", List.of("key")))
                .build();
    }

    /**
     * 执行 GET 命令
     *
     * @param args 参数 Map：
     *             <ul>
     *             <li>key (必填) - Redis 键</li>
     *             </ul>
     * @return 工具执行结果，包含键值或 (nil)
     */
    @Override
    public ToolResult execute(Map<String, Object> args) {
        String key = (String) args.get("key");
        if (key == null) {
            return ToolResult.error("key 参数不能为空");
        }

        try {
            String value = redis.opsForValue().get(key);
            return ToolResult.text(value != null ? value : "(nil)");
        } catch (Exception e) {
            return ToolResult.error("Redis 操作失败: " + e.getMessage());
        }
    }
}
