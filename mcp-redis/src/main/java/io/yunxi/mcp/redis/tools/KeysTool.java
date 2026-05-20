package io.yunxi.mcp.redis.tools;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Redis KEYS 命令工具
 * <p>
 * 工具名称: {@code redis_keys}
 * </p>
 * <p>
 * 根据模式查找 Redis 中的键。
 * </p>
 *
 * @author yunxi
 * @version 1.0.0
 * @since 1.0.0
 */
public class KeysTool implements ToolHandler {
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
    public KeysTool(StringRedisTemplate redis) {
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
                .name("keys")
                .description(
                        "Find Redis keys matching a pattern. " +
                                "根据模式查找Redis键。 " +
                                "Use this when you need to search for keys, list all keys, or discover data structures in Redis. "
                                +
                                "适用于搜索键、列出所有键、发现数据结构等场景。 " +
                                "Common use cases: finding user sessions, listing cache keys, discovering prefixed keys. "
                                +
                                "典型用例：查找用户会话、列出缓存键、发现特定前缀的键。")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "pattern", Map.of(
                                        "type", "string",
                                        "description", "Pattern to match keys. " +
                                                "Examples: '*' (all keys), 'user:*' (keys starting with user:), '*:session' (keys ending with :session) | "
                                                +
                                                "键匹配模式。示例: '*' (所有键), 'user:*' (以user:开头的键), '*:session' (以:session结尾的键)")),
                        "required", List.of("pattern")))
                .build();
    }

    /**
     * 执行 KEYS 命令
     *
     * @param args 参数 Map：
     *             <ul>
     *             <li>pattern (必填) - 键匹配模式（如 *, user:*）</li>
     *             </ul>
     * @return 工具执行结果，匹配的键列表或 (empty)
     */
    @Override
    public ToolResult execute(Map<String, Object> args) {
        Set<String> keys = redis.keys((String) args.get("pattern"));
        if (keys == null || keys.isEmpty())
            return ToolResult.text("(empty)");
        // 格式化输出匹配的键列表
        StringBuilder sb = new StringBuilder("Found ").append(keys.size()).append(" key(s):\n");
        keys.stream().sorted().forEach(k -> sb.append("- ").append(k).append("\n"));
        return ToolResult.text(sb.toString());
    }
}
