package io.yunxi.mcp.memory.store;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 记忆统计信息
 * <p>
 * 存储用户的记忆容量使用统计数据，包括 MEMORY.md 和 USER.md 两个存储目标的使用情况。
 * 用于向用户展示当前存储容量，帮助其管理记忆内容。
 * </p>
 * <p>
 * 字段说明：
 * </p>
 * <ul>
 *   <li>memoryUsage - MEMORY.md 当前使用的字符数</li>
 *   <li>memoryLimit - MEMORY.md 的最大容量限制（默认 2200 字符）</li>
 *   <li>memoryUsagePercentage - MEMORY.md 使用百分比</li>
 *   <li>userUsage - USER.md 当前使用的字符数</li>
 *   <li>userLimit - USER.md 的最大容量限制（默认 1375 字符）</li>
 *   <li>userUsagePercentage - USER.md 使用百分比</li>
 * </ul>
 *
 * @author yunxi-mcp-servers
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MemoryStats {

    /** MEMORY.md 当前使用的字符数 */
    private int memoryUsage;

    /** MEMORY.md 的最大容量限制 */
    private int memoryLimit;

    /** MEMORY.md 使用百分比 */
    private double memoryUsagePercentage;

    /** USER.md 当前使用的字符数 */
    private int userUsage;

    /** USER.md 的最大容量限制 */
    private int userLimit;

    /** USER.md 使用百分比 */
    private double userUsagePercentage;
}
