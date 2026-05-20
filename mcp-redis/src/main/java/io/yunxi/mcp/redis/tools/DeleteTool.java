package io.yunxi.mcp.redis.tools;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Map;

/**
 * Redis DEL 命令工具
 * <p>
 * 工具名称: {@code redis_delete}
 * </p>
 * <p>
 * 删除 Redis 中的一个或多个键。
 * </p>
 *
 * @author yunxi
 * @version 1.0.0
 * @since 1.0.0
 */
public class DeleteTool implements ToolHandler {
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
        public DeleteTool(StringRedisTemplate redis) {
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
                                .name("delete")
                                .description(
                                                "Delete one or more keys from Redis. " +
                                                                "删除Redis中的一个或多个键。 " +
                                                                "Use this when you need to remove expired data, clear cache entries, or delete user sessions. "
                                                                +
                                                                "适用于删除过期数据、清除缓存条目、删除用户会话等场景。 " +
                                                                "Common use cases: cache invalidation, session cleanup, removing test data. "
                                                                +
                                                                "典型用例：缓存失效、会话清理、删除测试数据。")
                                .inputSchema(Map.of(
                                                "type", "object",
                                                "properties", Map.of(
                                                                "keys", Map.of(
                                                                                "type", "array",
                                                                                "items", Map.of("type", "string"),
                                                                                "description",
                                                                                "Array of keys to delete. " +
                                                                                                "Examples: ['user:001'], ['session:abc', 'session:def'] | "
                                                                                                +
                                                                                                "要删除的键列表。示例: ['user:001'], ['session:abc', 'session:def']")),
                                                "required", List.of("keys")))
                                .build();
        }

        /**
         * 执行 DEL 命令
         *
         * @param args 参数 Map：
         *             <ul>
         *             <li>keys (必填) - 要删除的键列表</li>
         *             </ul>
         * @return 工具执行结果，删除的键数量
         */
        @Override
        @SuppressWarnings("unchecked")
        public ToolResult execute(Map<String, Object> args) {
                List<String> keys = (List<String>) args.get("keys");
                // 批量删除键并返回删除数量
                Long count = redis.delete(keys);
                return ToolResult.text("Deleted " + count + " key(s)");
        }
}
