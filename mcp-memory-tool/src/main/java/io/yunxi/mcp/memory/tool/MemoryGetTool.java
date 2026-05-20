package io.yunxi.mcp.memory.tool;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import io.yunxi.mcp.memory.store.MemoryStats;
import io.yunxi.mcp.memory.store.MemoryStoreInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Memory Get 工具 - 获取记忆内容
 * <p>
 * MCP 协议中的记忆获取工具，用于检索用户的记忆内容。
 * 支持获取指定目标（MEMORY.md 或 USER.md）的所有记忆，
 * 并可通过 context 参数进行语义检索过滤。
 * </p>
 * <p>
 * 功能特性：
 * </p>
 * <ul>
 *   <li>多用户隔离 - 通过 userId 参数确保用户间的记忆隔离</li>
 *   <li>语义检索 - 可选 context 参数支持基于向量相似度的记忆过滤</li>
 *   <li>容量统计 - 返回当前存储容量使用情况</li>
 *   <li>全文获取 - 支持获取目标存储的全部记忆内容</li>
 * </ul>
 * <p>
 * 使用示例：
 * </p>
 * <pre>
 * // 获取用户的所有通用记忆
 * {
 *   "name": "memory_get",
 *   "arguments": {
 *     "userId": "user123",
 *     "target": "memory"
 *   }
 * }
 *
 * // 获取与"工作习惯"相关的记忆
 * {
 *   "name": "memory_get",
 *   "arguments": {
 *     "userId": "user123",
 *     "target": "user",
 *     "context": "工作习惯"
 *   }
 * }
 * </pre>
 *
 * @author yunxi-mcp-servers
 * @version 1.0.0
 * @see MemoryStoreInterface
 * @see MemoryStats
 */
@Slf4j
@Component
public class MemoryGetTool implements ToolHandler {

    /** 记忆存储服务接口 */
    private final MemoryStoreInterface memoryStore;

    /**
     * 构造函数
     *
     * @param memoryStore 记忆存储服务（由 Spring 注入）
     */
    public MemoryGetTool(MemoryStoreInterface memoryStore) {
        this.memoryStore = memoryStore;
    }

    /**
     * 获取工具定义
     * <p>
     * 返回 MCP 协议所需的工具定义，包括名称、描述和输入参数模式。
     * </p>
     *
     * @return 工具定义对象
     */
    @Override
    public ToolDefinition getDefinition() {
        return ToolDefinition.builder()
                .name("memory_get")
                .description("获取指定存储的所有记忆内容（支持多用户隔离）")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "userId", Map.of(
                                        "type", "string",
                                        "description", "用户ID（必填，用于多用户隔离）"),
                                "target", Map.of(
                                        "type", "string",
                                        "description", "目标存储：'memory' 或 'user'",
                                        "enum", new String[]{"memory", "user"}),
                                "context", Map.of(
                                        "type", "string",
                                        "description", "上下文（可选，用于过滤相关记忆）")
                        ),
                        "required", new String[]{"userId", "target"}
                ))
                .build();
    }

    /**
     * 执行工具
     * <p>
     * 处理记忆获取请求，执行流程：
     * </p>
     * <ol>
     *   <li>参数验证 - 检查必填参数是否完整</li>
     *   <li>获取记忆 - 调用存储服务获取记忆内容</li>
     *   <li>获取统计 - 返回当前容量使用情况</li>
     *   <li>构建响应 - 返回记忆内容和容量信息</li>
     * </ol>
     * <p>
     * 特别注意：如果提供了 context 参数，将使用向量检索获取与上下文相关的记忆；
     * 如果未提供，则返回目标存储的全部记忆内容。
     * </p>
     *
     * @param arguments 工具输入参数
     * @return 操作结果，包含记忆内容或错误原因
     */
    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        try {
            // 提取参数
            String userId = (String) arguments.get("userId");
            String target = (String) arguments.get("target");
            String context = (String) arguments.get("context");

            // 参数校验
            if (userId == null || userId.isBlank()) {
                return ToolResult.error("参数 userId 不能为空（用于多用户隔离）");
            }
            if (target == null || target.isBlank()) {
                return ToolResult.error("参数 target 不能为空");
            }

            // 获取记忆内容（如果提供了 context，使用语义检索）
            String content = memoryStore.getRelevantMemory(userId, target, context);
            // 获取容量统计
            MemoryStats stats = memoryStore.getMemoryStats(userId);

            // 构建响应结果
            StringBuilder result = new StringBuilder();
            result.append("获取记忆成功\n");
            result.append("- 用户ID: ").append(userId).append("\n");
            result.append("- 目标: ").append(target).append("\n");
            result.append("\n使用情况:\n");
            result.append("- MEMORY.md: ").append(stats.getMemoryUsage()).append("/").append(stats.getMemoryLimit())
                    .append(" (").append(String.format("%.1f", stats.getMemoryUsagePercentage())).append("%)\n");
            result.append("- USER.md: ").append(stats.getUserUsage()).append("/").append(stats.getUserLimit())
                    .append(" (").append(String.format("%.1f", stats.getUserUsagePercentage())).append("%)\n");
            result.append("\n记忆内容:\n");
            result.append(content != null && !content.isBlank() ? content : "(暂无记忆)");

            log.info("Memory retrieved: userId={}, target={}, length={}", userId, target,
                    content != null ? content.length() : 0);

            return ToolResult.text(result.toString());

        } catch (Exception e) {
            log.error("Failed to get memory", e);
            return ToolResult.error("获取记忆失败: " + e.getMessage());
        }
    }
}
