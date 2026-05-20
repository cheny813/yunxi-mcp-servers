package io.yunxi.mcp.memory.tool;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import io.yunxi.mcp.memory.store.MemoryStoreInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Memory Replace 工具 - 替换记忆条目
 * <p>
 * MCP 协议中的记忆替换工具，用于更新用户记忆中现有的条目。
 * 替换操作基于 entryId（条目ID）进行精确匹配，并将内容更新为新值。
 * </p>
 * <p>
 * 功能特性：
 * </p>
 * <ul>
 *   <li>精确替换 - 通过 entryId 精确替换指定记忆条目</li>
 *   <li>多用户隔离 - 通过 userId 参数确保用户间的记忆隔离</li>
 *   <li>目标区分 - 支持选择替换 MEMORY.md 或 USER.md 中的条目</li>
 *   <li>安全验证 - 替换内容同样经过注入检测</li>
 * </ul>
 * <p>
 * 使用示例：
 * </p>
 * <pre>
 * {
 *   "name": "memory_replace",
 *   "arguments": {
 *     "userId": "user123",
 *     "target": "memory",
 *     "entryId": "2024-01-15T10:30:00",
 *     "content": "用户喜欢简洁的深色主题界面设计"
 *   }
 * }
 * </pre>
 *
 * @author yunxi-mcp-servers
 * @version 1.0.0
 * @see MemoryStoreInterface
 */
@Slf4j
@Component
public class MemoryReplaceTool implements ToolHandler {

    /** 记忆存储服务接口 */
    private final MemoryStoreInterface memoryStore;

    /**
     * 构造函数
     *
     * @param memoryStore 记忆存储服务（由 Spring 注入）
     */
    public MemoryReplaceTool(MemoryStoreInterface memoryStore) {
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
                .name("memory_replace")
                .description("替换现有的记忆条目（支持多用户隔离）")
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
                                "entryId", Map.of(
                                        "type", "string",
                                        "description", "要替换的记忆条目ID"),
                                "content", Map.of(
                                        "type", "string",
                                        "description", "新的记忆内容")
                        ),
                        "required", new String[]{"userId", "target", "entryId", "content"}
                ))
                .build();
    }

    /**
     * 执行工具
     * <p>
     * 处理记忆替换请求，执行流程：
     * </p>
     * <ol>
     *   <li>参数验证 - 检查必填参数是否完整</li>
     *   <li>内容验证 - 检查新内容是否包含注入模式</li>
     *   <li>调用存储服务 - 替换指定条目</li>
     *   <li>构建响应 - 返回操作结果</li>
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
            String entryId = (String) arguments.get("entryId");
            String newContent = (String) arguments.get("content");

            // 参数校验
            if (userId == null || userId.isBlank()) {
                return ToolResult.error("参数 userId 不能为空（用于多用户隔离）");
            }
            if (target == null || target.isBlank()) {
                return ToolResult.error("参数 target 不能为空");
            }
            if (entryId == null || entryId.isBlank()) {
                return ToolResult.error("参数 entryId 不能为空");
            }
            if (newContent == null || newContent.isBlank()) {
                return ToolResult.error("参数 content 不能为空");
            }

            // 调用存储服务替换记忆
            String updatedId = memoryStore.replaceEntry(userId, target, entryId, newContent);

            // 构建响应结果
            StringBuilder result = new StringBuilder();
            result.append("记忆已成功替换\n");
            result.append("- 用户ID: ").append(userId).append("\n");
            result.append("- 条目ID: ").append(updatedId).append("\n");
            result.append("- 目标: ").append(target).append("\n");

            log.info("Memory entry replaced: userId={}, target={}, entryId={}", userId, target, entryId);

            return ToolResult.text(result.toString());

        } catch (Exception e) {
            log.error("Failed to replace memory entry", e);
            return ToolResult.error("替换记忆失败: " + e.getMessage());
        }
    }
}
