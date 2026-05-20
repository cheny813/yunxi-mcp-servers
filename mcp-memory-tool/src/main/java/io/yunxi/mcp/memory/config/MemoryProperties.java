package io.yunxi.mcp.memory.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 记忆存储配置属性
 * <p>
 * 配置记忆存储的行为参数，包括存储路径和容量限制。
 * </p>
 * <p>
 * 配置前缀：{@code yunxi.learning-loop.memory}
 * </p>
 * <p>
 * 默认值：
 * </p>
 * <ul>
 *   <li>enabled: true（启用记忆存储）</li>
 *   <li>storagePathDev: ./data/memory（开发模式本地存储）</li>
 *   <li>storagePathProd: /shared/data/memory（生产模式共享存储）</li>
 *   <li>maxMemoryLength: 2200（MEMORY.md 最大字符数）</li>
 *   <li>maxUserMemoryLength: 1375（USER.md 最大字符数）</li>
 * </ul>
 */
@Data
@ConfigurationProperties(prefix = "yunxi.learning-loop.memory")
public class MemoryProperties {

    /**
     * 是否启用记忆存储
     * 设为 false 可禁用记忆功能
     */
    private boolean enabled = true;

    /**
     * 开发模式存储路径（本地）
     * 适用于开发环境，每个应用实例独立存储
     */
    private String storagePathDev = "./data/memory";

    /**
     * 生产模式存储路径（共享文件系统）
     * 适用于生产环境，多个应用实例共享存储
     */
    private String storagePathProd = "/shared/data/memory";

    /**
     * MEMORY.md 最大字符数
     * 通用记忆存储容量限制，防止超出 LLM 上下文窗口
     */
    private int maxMemoryLength = 2200;

    /**
     * USER.md 最大字符数
     * 用户个人信息存储容量限制
     */
    private int maxUserMemoryLength = 1375;
}
