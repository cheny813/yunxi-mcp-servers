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
 * Memory Add 工具 - 添加记忆条目
 * <p>
 * MCP 协议中的记忆添加工具，用于向用户的记忆中添加新的条目。
 * 支持两种存储目标：
 * </p>
 * <ul>
 *   <li>MEMORY.md - 存储通用记忆（热数据层），如偏好、模式、规则、知识等</li>
 *   <li>USER.md - 存储用户个人信息，如习惯、工作风格、期望等</li>
 * </ul>
 * <p>
 * 功能特性：
 * </p>
 * <ul>
 *   <li>多用户隔离 - 通过 userId 参数实现用户间的记忆隔离</li>
 *   <li>分类支持 - 支持自定义分类（category）便于记忆管理</li>
 *   <li>容量管理 - 支持容量限制检查，防止超出存储上限</li>
 *   <li>安全过滤 - 内置注入检测，防止 LLM 输出注入攻击</li>
 * </ul>
 * <p>
 * 使用示例：
 * </p>
 * <pre>
 * {
 *   "name": "memory_add",
 *   "arguments": {
 *     "userId": "user123",
 *     "target": "memory",
 *     "category": "preference",
 *     "content": "用户喜欢简洁的界面设计"
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
public class MemoryAddTool implements ToolHandler {

    /** 记忆存储服务接口 */
    private final MemoryStoreInterface memoryStore;

    /**
     * 构造函数
     *
     * @param memoryStore 记忆存储服务（由 Spring 注入）
     */
    public MemoryAddTool(MemoryStoreInterface memoryStore) {
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
                .name("memory_add")
                .description("添加新的记忆条目到 MEMORY.md 或 USER.md（支持多用户隔离）")
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
                                "category", Map.of(
                                        "type", "string",
                                        "description", "记忆分类（如：preference, fact, instruction, experience）"),
                                "content", Map.of(
                                        "type", "string",
                                        "description", "记忆内容")
                        ),
                        "required", new String[]{"userId", "target", "category", "content"}
                ))
                .build();
    }

    /**
     * 执行工具
     * <p>
     * 处理记忆添加请求，执行流程：
     * </p>
     * <ol>
     *   <li>参数验证 - 检查必填参数是否完整</li>
     *   <li>调用存储服务 - 添加记忆条目</li>
     *   <li>获取统计信息 - 返回当前容量使用情况</li>
     *   <li>构建响应 - 返回操作结果和容量信息</li>
     * </ol>
     *
     * @param arguments 工具输入参数
     * @return 操作结果，包含成功信息或错误原因
     */
    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        try {
            // 提取参数
            String userId = (String) arguments.get("userId");
            String target = (String) arguments.get("target");
            String category = (String) arguments.get("category");
            String content = (String) arguments.get("content");

            // 参数校验
            if (userId == null || userId.isBlank()) {
                return ToolResult.error("参数 userId 不能为空（用于多用户隔离）");
            }
            if (target == null || target.isBlank()) {
                return ToolResult.error("参数 target 不能为空");
            }
            if (category == null || category.isBlank()) {
                return ToolResult.error("参数 category 不能为空");
            }
            if (content == null || content.isBlank()) {
                return ToolResult.error("参数 content 不能为空");
            }

            // 调用存储服务添加记忆
            String entryId = memoryStore.addEntry(userId, target, category, content);

            // 获取容量统计
            MemoryStats stats = memoryStore.getMemoryStats(userId);

            // 构建响应结果
            StringBuilder result = new StringBuilder();
            result.append("记忆已成功添加\n");
            result.append("- 用户ID: ").append(userId).append("\n");
            result.append("- 条目ID: ").append(entryId).append("\n");
            result.append("- 目标: ").append(target).append("\n");
            result.append("- 分类: ").append(category).append("\n");
            result.append("\n使用情况:\n");
            result.append("- MEMORY.md: ").append(stats.getMemoryUsage()).append("/").append(stats.getMemoryLimit())
                    .append(" (").append(String.format("%.1f", stats.getMemoryUsagePercentage())).append("%)\n");
            result.append("- USER.md: ").append(stats.getUserUsage()).append("/").append(stats.getUserLimit())
                    .append(" (").append(String.format("%.1f", stats.getUserUsagePercentage())).append("%)\n");

            log.info("Memory entry added: userId={}, target={}, category={}, entryId={}", userId, target, category, entryId);

            return ToolResult.text(result.toString());

        } catch (IllegalArgumentException e) {
            // 容量超限
            log.warn("Memory limit exceeded: {}", e.getMessage());
            return ToolResult.error("记忆容量超限: " + e.getMessage());
        } catch (Exception e) {
            // 其他异常
            log.error("Failed to add memory entry", e);
            return ToolResult.error("添加记忆失败: " + e.getMessage());
        }
    }
}
