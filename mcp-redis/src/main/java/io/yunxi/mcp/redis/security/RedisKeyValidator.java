package io.yunxi.mcp.redis.security;

import io.yunxi.mcp.redis.config.RedisNamespaceConfig;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis Key 访问校验器
 * <p>
 * 提供 Key 命名空间隔离和访问控制功能。
 * </p>
 *
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public class RedisKeyValidator {

    private final RedisNamespaceConfig config;

    public RedisKeyValidator(RedisNamespaceConfig config) {
        this.config = config;
    }

    /**
     * 校验并处理 Key
     *
     * @param key 原始 Key
     * @return 校验结果
     */
    public ValidationResult validateAndProcess(String key) {
        if (key == null || key.isEmpty()) {
            return ValidationResult.failure("Key 不能为空");
        }

        // 检查 Key 是否允许访问
        if (!config.isKeyAllowed(key)) {
            log.warn("Key '{}' 不在允许访问列表中", key);
            return ValidationResult.failure("Key '" + key + "' 不在允许访问列表中");
        }

        // 添加命名空间前缀
        String namespacedKey = config.buildNamespacedKey(key);

        return ValidationResult.success(namespacedKey);
    }

    /**
     * 批量校验并处理 Keys
     *
     * @param keys 原始 Keys
     * @return 校验结果
     */
    public ValidationResult validateAndProcessMultiple(String... keys) {
        if (keys == null || keys.length == 0) {
            return ValidationResult.failure("Keys 不能为空");
        }

        String[] processedKeys = new String[keys.length];
        for (int i = 0; i < keys.length; i++) {
            ValidationResult result = validateAndProcess(keys[i]);
            if (!result.isValid()) {
                return result;
            }
            processedKeys[i] = result.getProcessedKey();
        }

        return ValidationResult.success(processedKeys);
    }

    /**
     * 从带命名空间的 Key 中提取原始 Key
     *
     * @param namespacedKey 带命名空间的 Key
     * @return 原始 Key
     */
    public String extractOriginalKey(String namespacedKey) {
        return config.stripNamespace(namespacedKey);
    }

    /**
     * 校验结果
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        private final String processedKey;
        private final String[] processedKeys;

        private ValidationResult(boolean valid, String message, String processedKey, String[] processedKeys) {
            this.valid = valid;
            this.message = message;
            this.processedKey = processedKey;
            this.processedKeys = processedKeys;
        }

        public static ValidationResult success(String processedKey) {
            return new ValidationResult(true, null, processedKey, null);
        }

        public static ValidationResult success(String[] processedKeys) {
            return new ValidationResult(true, null, null, processedKeys);
        }

        public static ValidationResult failure(String message) {
            return new ValidationResult(false, message, null, null);
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }

        public String getProcessedKey() {
            return processedKey;
        }

        public String[] getProcessedKeys() {
            return processedKeys;
        }
    }
}
