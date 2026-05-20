package io.yunxi.mcp.redis.tools;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Map;

/**
 * Redis Hash 操作工具
 * <p>
 * 工具名称: {@code redis_hash_ops}
 * </p>
 * <p>
 * 支持 Redis Hash 类型的常用操作，包括：
 * <ul>
 * <li>hset - 设置哈希字段值</li>
 * <li>hget - 获取哈希字段值</li>
 * <li>hgetall - 获取哈希所有字段和值</li>
 * <li>hdel - 删除哈希字段</li>
 * </ul>
 * </p>
 *
 * @author yunxi
 * @version 1.0.0
 * @since 1.0.0
 */
public class HashOpsTool implements ToolHandler {
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
    public HashOpsTool(StringRedisTemplate redis) {
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
                .name("hash_ops")
                .description(
                        "Perform Redis Hash operations. " +
                                "执行Redis哈希操作。 " +
                                "Use this when you need to store structured objects with multiple fields, manage user profiles, product details, or configuration data. "
                                +
                                "适用于存储多字段结构化对象、管理用户资料、产品详情或配置数据等场景。 " +
                                "Common use cases: user data storage, product attributes, session data with multiple fields. "
                                +
                                "典型用例：用户数据存储、产品属性、多字段会话数据。")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "operation", Map.of(
                                        "type", "string",
                                        "description", "Hash operation to perform. " +
                                                "Options: 'hset' (设置字段值), 'hget' (获取字段值), 'hgetall' (获取所有字段), 'hdel' (删除字段) | "
                                                +
                                                "哈希操作类型。选项: 'hset', 'hget', 'hgetall', 'hdel'",
                                        "enum", List.of("hset", "hget", "hgetall", "hdel")),
                                "key", Map.of(
                                        "type", "string",
                                        "description",
                                        "Redis hash key. Example: 'user:001' | Redis哈希键。示例: 'user:001', 'product:123'"),
                                "field", Map.of(
                                        "type", "string",
                                        "description",
                                        "Hash field name (required for hset/hget/hdel). Example: 'name', 'email' | " +
                                                "哈希字段名（hset/hget/hdel需要）。示例: 'name', 'email', 'price'"),
                                "value", Map.of(
                                        "type", "string",
                                        "description",
                                        "Hash field value (required for hset). Example: 'John', 'john@example.com' | " +
                                                "哈希字段值（hset需要）。示例: 'John', 'john@example.com', '99.99'")),
                        "required", List.of("operation", "key")))
                .build();
    }

    /**
     * 执行 Hash 操作
     *
     * @param args 参数 Map：
     *             <ul>
     *             <li>operation (必填) - 操作类型（hset/hget/hgetall/hdel）</li>
     *             <li>key (必填) - Hash 键</li>
     *             <li>field (可选，hset/hget/hdel 需要) - 字段名</li>
     *             <li>value (可选，hset 需要) - 字段值</li>
     *             </ul>
     * @return 工具执行结果
     */
    @Override
    public ToolResult execute(Map<String, Object> args) {
        String operation = (String) args.get("operation");
        if (operation == null) {
            return ToolResult.error("operation 参数不能为空");
        }
        String op = operation.toLowerCase();
        String key = (String) args.get("key");

        switch (op) {
            case "hset" -> {
                // 设置哈希字段值
                redis.opsForHash().put(key, args.get("field"), args.get("value"));
                return ToolResult.text("HSET OK");
            }
            case "hget" -> {
                // 获取哈希字段值
                Object v = redis.opsForHash().get(key, args.get("field"));
                return ToolResult.text(v != null ? v.toString() : "(nil)");
            }
            case "hgetall" -> {
                // 获取哈希所有字段和值
                Map<Object, Object> entries = redis.opsForHash().entries(key);
                if (entries.isEmpty())
                    return ToolResult.text("(empty hash)");
                // 格式化输出所有字段和值
                StringBuilder sb = new StringBuilder();
                entries.forEach((k, v) -> sb.append(k).append(": ").append(v).append("\n"));
                return ToolResult.text(sb.toString());
            }
            case "hdel" -> {
                // 删除哈希字段
                return ToolResult.text("HDEL " + redis.opsForHash().delete(key, args.get("field")));
            }
            default -> {
                return ToolResult.error("Unknown operation: " + op);
            }
        }
    }
}
