package io.yunxi.mcp.redis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Redis 命名空间配置
 * <p>
 * 提供 Redis Key 的命名空间隔离功能，实现多租户数据隔离。
 * </p>
 *
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "mcp.redis")
public class RedisNamespaceConfig {

    /**
     * 全局 Key 前缀
     * <p>
     * 所有 Redis Key 都会自动添加此前缀，用于与其他应用隔离。
     * 例如：mcp
     * </p>
     */
    private String globalPrefix = "mcp";

    /**
     * 命名空间分隔符
     */
    private String namespaceSeparator = ":";

    /**
     * 当前租户/应用标识
     * <p>
     * 用于多租户场景下的数据隔离。
     * 例如：tenant1, app-prod
     * </p>
     */
    private String currentNamespace = "default";

    /**
     * 允许的 Key 前缀白名单
     * <p>
     * 如果配置了白名单，只有匹配这些前缀的 Key 才能被访问。
     * 例如：["cache:", "session:", "data:"]
     * </p>
     */
    private List<String> allowedKeyPrefixes;

    /**
     * 禁止的 Key 前缀黑名单
     * <p>
     * 匹配这些前缀的 Key 将被拒绝访问。
     * 例如：["system:", "config:", "internal:"]
     * </p>
     */
    private List<String> deniedKeyPrefixes;

    /**
     * 是否为 Key 添加命名空间前缀
     */
    private boolean enableNamespace = true;

    /**
     * 构建带命名空间的 Key
     *
     * @param key 原始 Key
     * @return 带命名空间的 Key
     */
    public String buildNamespacedKey(String key) {
        if (!enableNamespace || key == null || key.isEmpty()) {
            return key;
        }

        // 如果 Key 已经以全局前缀开头，不再添加
        if (key.startsWith(globalPrefix + namespaceSeparator)) {
            return key;
        }

        return globalPrefix + namespaceSeparator + currentNamespace + namespaceSeparator + key;
    }

    /**
     * 移除命名空间前缀，获取原始 Key
     *
     * @param namespacedKey 带命名空间的 Key
     * @return 原始 Key
     */
    public String stripNamespace(String namespacedKey) {
        if (namespacedKey == null || namespacedKey.isEmpty()) {
            return namespacedKey;
        }

        String prefix = globalPrefix + namespaceSeparator + currentNamespace + namespaceSeparator;
        if (namespacedKey.startsWith(prefix)) {
            return namespacedKey.substring(prefix.length());
        }

        return namespacedKey;
    }

    /**
     * 校验 Key 是否允许访问
     *
     * @param key 原始 Key（不含命名空间）
     * @return 是否允许访问
     */
    public boolean isKeyAllowed(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }

        // 检查黑名单
        if (deniedKeyPrefixes != null && !deniedKeyPrefixes.isEmpty()) {
            for (String denied : deniedKeyPrefixes) {
                if (key.startsWith(denied)) {
                    return false;
                }
            }
        }

        // 检查白名单（如果配置了）
        if (allowedKeyPrefixes != null && !allowedKeyPrefixes.isEmpty()) {
            for (String allowed : allowedKeyPrefixes) {
                if (key.startsWith(allowed)) {
                    return true;
                }
            }
            return false; // 不在白名单中
        }

        return true;
    }
}
