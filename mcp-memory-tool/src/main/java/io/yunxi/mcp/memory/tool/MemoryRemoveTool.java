package io.yunxi.mcp.memory.tool;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import io.yunxi.mcp.memory.store.MemoryStoreInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Memory Remove 工具 - 删除记忆条目
 * <p>
 * MCP 协议中的记忆删除工具，用于从用户的记忆中删除指定的条目。
 * 删除操作基于 entryId（条目ID）进行精确匹配。
 * </p>
 * <p>
 * 功能特性：
 * </p>
 * <ul>
 *   <li>精确删除 - 通过 entryId 精确删除指定记忆条目</li>
 *   <li>多用户隔离 - 通过 userId 参数确保用户间的记忆隔离</li>
 *   <li>目标区分 - 支持选择删除 MEMORY.md 或 USER.md 中的条目</li>
 * </ul>
 * <p>
 * 使用示例：
 * </p>
 * <pre>
 * {
 *   "name": "memory_remove",
 *   "arguments": {
 *     "userId": "user123",
 *     "target": "memory",
 *     "entryId": "2024-01-15T10:30:00"
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
public class MemoryRemoveTool implements ToolHandler {

    /** 记忆存储服务接口 */
    private final MemoryStoreInterface memoryStore;

    /**
     * 构造函数
     *
     * @param memoryStore 记忆存储服务（由 Spring 注入）
     */
    public MemoryRemoveTool(MemoryStoreInterface memoryStore) {
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
                .name("memory_remove")
                .description("删除指定的记忆条目（支持多用户隔离）")
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
                                        "description", "要删除的记忆条目ID")
                        ),
                        "required", new String[]{"userId", "target", "entryId"}
                ))
                .build();
    }

    /**
     * 执行工具
     * <p>
     * 处理记忆删除请求，执行流程：
     * </p>
     * <ol>
     *   <li>参数验证 - 检查必填参数是否完整</li>
     *   <li>调用存储服务 - 删除指定条目</li>
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

            // 调用存储服务删除记忆
            memoryStore.removeEntry(userId, target, entryId);

            // 构建响应结果
            StringBuilder result = new StringBuilder();
            result.append("记忆已成功删除\n");
            result.append("- 用户ID: ").append(userId).append("\n");
            result.append("- 条目ID: ").append(entryId).append("\n");
            result.append("- 目标: ").append(target).append("\n");

            log.info("Memory entry removed: userId={}, target={}, entryId={}", userId, target, entryId);

            return ToolResult.text(result.toString());

        } catch (Exception e) {
            log.error("Failed to remove memory entry", e);
            return ToolResult.error("删除记忆失败: " + e.getMessage());
        }
    }
}
