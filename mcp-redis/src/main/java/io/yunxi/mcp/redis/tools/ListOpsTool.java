package io.yunxi.mcp.redis.tools;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Map;

/**
 * Redis List 操作工具
 * <p>
 * 工具名称: {@code redis_list_ops}
 * </p>
 * <p>
 * 支持 Redis List 类型的常用操作，包括：
 * <ul>
 * <li>lpush - 从左侧推入元素</li>
 * <li>rpush - 从右侧推入元素</li>
 * <li>lrange - 获取列表范围</li>
 * <li>lpop - 从左侧弹出元素</li>
 * <li>rpop - 从右侧弹出元素</li>
 * </ul>
 * </p>
 *
 * @author yunxi
 * @version 1.0.0
 * @since 1.0.0
 */
public class ListOpsTool implements ToolHandler {
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
    public ListOpsTool(StringRedisTemplate redis) {
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
                .name("list_ops")
                .description(
                        "Perform Redis List operations. " +
                                "执行Redis列表操作。 " +
                                "Use this when you need to implement queues, stacks, message queues, or ordered collections. "
                                +
                                "适用于实现队列、栈、消息队列或有序集合等场景。 " +
                                "Common use cases: task queues, log buffers, recent activity lists, chat message history. "
                                +
                                "典型用例：任务队列、日志缓冲、最近活动列表、聊天消息历史。")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "operation", Map.of(
                                        "type", "string",
                                        "description", "List operation to perform. " +
                                                "Options: 'lpush' (左侧推入), 'rpush' (右侧推入), 'lrange' (获取范围), " +
                                                "'lpop' (左侧弹出), 'rpop' (右侧弹出), 'llen' (获取长度) | " +
                                                "列表操作类型。选项: 'lpush', 'rpush', 'lrange', 'lpop', 'rpop', 'llen'",
                                        "enum", List.of("lpush", "rpush", "lrange", "lpop", "rpop", "llen")),
                                "key", Map.of(
                                        "type", "string",
                                        "description",
                                        "Redis list key. Example: 'queue:tasks' | Redis列表键。示例: 'queue:tasks', 'log:errors'"),
                                "values", Map.of(
                                        "type", "array",
                                        "items", Map.of("type", "string"),
                                        "description",
                                        "Array of values to push (required for lpush/rpush). Example: ['task1', 'task2'] | "
                                                +
                                                "要推入的值列表（lpush/rpush需要）。示例: ['task1', 'task2']"),
                                "start", Map.of(
                                        "type", "integer",
                                        "description", "Start index for lrange (0-based, default 0). Example: 0 | " +
                                                "起始索引（从0开始，默认0）。示例: 0"),
                                "stop", Map.of(
                                        "type", "integer",
                                        "description",
                                        "Stop index for lrange (-1 means last, default -1). Example: -1 or 9 | " +
                                                "结束索引（-1表示最后一个，默认-1）。示例: -1 或 9")),
                        "required", List.of("operation", "key")))
                .build();
    }

    /**
     * 执行 List 操作
     *
     * @param args 参数 Map：
     *             <ul>
     *             <li>operation (必填) - 操作类型（lpush/rpush/lrange/lpop/rpop）</li>
     *             <li>key (必填) - List 键</li>
     *             <li>values (可选，lpush/rpush 需要) - 要添加的值列表</li>
     *             <li>start (可选，lrange 需要) - 起始索引</li>
     *             <li>stop (可选，lrange 需要) - 结束索引</li>
     *             </ul>
     * @return 工具执行结果
     */
    @Override
    @SuppressWarnings("unchecked")
    public ToolResult execute(Map<String, Object> args) {
        String operation = (String) args.get("operation");
        if (operation == null) {
            return ToolResult.error("operation 参数不能为空");
        }
        String op = operation.toLowerCase();
        String key = (String) args.get("key");

        if (key == null) {
            return ToolResult.error("key 参数不能为空");
        }

        try {
            switch (op) {
                case "lpush" -> {
                    // 从左侧推入元素
                    @SuppressWarnings("rawtypes")
                    List vals = (List) args.get("elements");
                    if (vals == null) {
                        vals = (List) args.get("values");
                    }
                    if (vals == null) {
                        return ToolResult.error("elements 参数不能为空");
                    }
                    // 转换为 String[]
                    String[] elements = new String[vals.size()];
                    for (int i = 0; i < vals.size(); i++) {
                        elements[i] = String.valueOf(vals.get(i));
                    }
                    Long c = redis.opsForList().leftPushAll(key, elements);
                    return ToolResult.text("LPUSH " + c);
                }
                case "rpush" -> {
                    // 从右侧推入元素
                    @SuppressWarnings("rawtypes")
                    List vals = (List) args.get("elements");
                    if (vals == null) {
                        vals = (List) args.get("values");
                    }
                    if (vals == null) {
                        return ToolResult.error("elements 参数不能为空");
                    }
                    // 转换为 String[]
                    String[] elements = new String[vals.size()];
                    for (int i = 0; i < vals.size(); i++) {
                        elements[i] = String.valueOf(vals.get(i));
                    }
                    Long c = redis.opsForList().rightPushAll(key, elements);
                    return ToolResult.text("RPUSH " + c);
                }
                case "lrange" -> {
                    // 获取列表范围
                    int start = args.containsKey("start") ? ((Number) args.get("start")).intValue() : 0;
                    int stop = args.containsKey("stop") ? ((Number) args.get("stop")).intValue() : -1;
                    return ToolResult.text("LRANGE: " + redis.opsForList().range(key, start, stop));
                }
                case "lpop" -> {
                    // 从左侧弹出元素
                    String val = redis.opsForList().leftPop(key);
                    return ToolResult.text("LPOP: " + (val != null ? val : "(nil)"));
                }
                case "rpop" -> {
                    // 从右侧弹出元素
                    String val = redis.opsForList().rightPop(key);
                    return ToolResult.text("RPOP: " + (val != null ? val : "(nil)"));
                }
                case "llen" -> {
                    // 获取列表长度
                    Long len = redis.opsForList().size(key);
                    return ToolResult.text("LLEN: " + len);
                }
                default -> {
                    return ToolResult.error("不支持的操作: " + op);
                }
            }
        } catch (Exception e) {
            return ToolResult.error("Redis 操作失败: " + e.getMessage());
        }
    }
}
