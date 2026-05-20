package io.yunxi.mcp.memory.store;

import io.yunxi.mcp.memory.store.MemoryStats;

/**
 * 记忆存储接口
 * <p>
 * 定义记忆存储的标准接口，所有存储实现类都需要实现此接口。
 * 提供对用户记忆的增删改查操作，支持多用户隔离。
 * </p>
 * <p>
 * 主要功能：
 * </p>
 * <ul>
 *   <li>添加记忆 - 向指定目标添加新的记忆条目</li>
 *   <li>替换记忆 - 更新现有记忆条目的内容</li>
 *   <li>删除记忆 - 移除指定的记忆条目</li>
 *   <li>获取记忆 - 检索记忆内容，支持语义过滤</li>
 *   <li>容量统计 - 获取存储使用情况</li>
 * </ul>
 * <p>
 * 实现类：
 * </p>
 * <ul>
 *   <li>{@link io.yunxi.mcp.memory.store.FileMemoryStore} - 文件存储实现</li>
 *   <li>{@link io.yunxi.mcp.memory.store.MilvusMemoryStore} - Milvus 向量存储实现</li>
 * </ul>
 *
 * @author yunxi-mcp-servers
 */
public interface MemoryStoreInterface {

    /**
     * 添加记忆条目
     * <p>
     * 向指定的存储目标添加新的记忆条目。
     * </p>
     *
     * @param userId   用户ID（必填，用于多用户隔离）
     * @param target   目标存储：memory 或 user
     * @param category 分类（如：preference, fact, instruction, experience）
     * @param content  记忆内容
     * @return 条目ID（时间戳格式）
     */
    String addEntry(String userId, String target, String category, String content);

    /**
     * 替换现有条目
     * <p>
     * 根据 entryId 查找并替换记忆内容。
     * </p>
     *
     * @param userId     用户ID（必填，用于多用户隔离）
     * @param target     目标存储：memory 或 user
     * @param entryId    条目ID（用于定位要替换的条目）
     * @param newContent 新的记忆内容
     * @return 替换后的条目ID
     */
    String replaceEntry(String userId, String target, String entryId, String newContent);

    /**
     * 删除条目
     * <p>
     * 根据 entryId 删除指定的记忆条目。
     * </p>
     *
     * @param userId  用户ID（必填，用于多用户隔离）
     * @param target  目标存储：memory 或 user
     * @param entryId 条目ID（用于定位要删除的条目）
     */
    void removeEntry(String userId, String target, String entryId);

    /**
     * 获取记忆内容
     * <p>
     * 检索指定用户的记忆内容。如果提供了 context 参数，
     * 使用语义检索返回相关的记忆；否则返回全部记忆。
     * </p>
     *
     * @param userId  用户ID（必填，用于多用户隔离）
     * @param target  目标存储：memory 或 user
     * @param context 上下文（可选，用于语义过滤）
     * @return 记忆内容文本
     */
    String getRelevantMemory(String userId, String target, String context);

    /**
     * 获取记忆统计
     * <p>
     * 返回当前用户的存储使用情况，包括已使用字符数和容量限制。
     * </p>
     *
     * @param userId 用户ID（必填，用于多用户隔离）
     * @return 统计信息对象
     */
    MemoryStats getMemoryStats(String userId);
}
