package io.yunxi.mcp.common.validation;

import io.yunxi.mcp.common.model.ToolParameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Pattern;

/**
 * MCP 工具参数验证器
 * <p>
 * 提供统一的参数校验功能：
 * <ul>
 *   <li>必填参数校验</li>
 *   <li>类型校验</li>
 *   <li>格式校验</li>
 *   <li>枚举值校验</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Slf4j
@Component
public class ParameterValidator {

    /**
     * 验证参数
     *
     * @param paramDef 参数定义
     * @param value    参数值
     * @return 验证结果
     */
    public ValidationResult validate(ToolParameter paramDef, Object value) {
        List<String> errors = new ArrayList<>();

        // 1. 必填校验
        if (paramDef.isRequired()) {
            if (value == null || (value instanceof String && !StringUtils.hasText((String) value))) {
                errors.add(String.format("参数 '%s' 为必填项", paramDef.getName()));
                return ValidationResult.failure(errors);
            }
        }

        // 如果值为空且不是必填，跳过其他校验
        if (value == null) {
            return ValidationResult.success();
        }

        // 2. 类型校验
        if (!validateType(paramDef.getType(), value)) {
            errors.add(String.format("参数 '%s' 类型错误，期望: %s，实际: %s",
                    paramDef.getName(), paramDef.getType(), value.getClass().getSimpleName()));
        }

        // 3. 枚举值校验
        if (paramDef.getEnumValues() != null && !paramDef.getEnumValues().isEmpty()) {
            if (!paramDef.getEnumValues().contains(value.toString())) {
                errors.add(String.format("参数 '%s' 值 '%s' 不在允许范围内，可选值: %s",
                        paramDef.getName(), value, paramDef.getEnumValues()));
            }
        }

        // 4. 格式校验（正则）
        if (StringUtils.hasText(paramDef.getPattern())) {
            if (!Pattern.matches(paramDef.getPattern(), value.toString())) {
                errors.add(String.format("参数 '%s' 格式错误，期望格式: %s",
                        paramDef.getName(), paramDef.getPattern()));
            }
        }

        // 5. 范围校验（数值）
        if (value instanceof Number) {
            double numValue = ((Number) value).doubleValue();
            if (paramDef.getMinimum() != null && numValue < paramDef.getMinimum()) {
                errors.add(String.format("参数 '%s' 不能小于 %s",
                        paramDef.getName(), paramDef.getMinimum()));
            }
            if (paramDef.getMaximum() != null && numValue > paramDef.getMaximum()) {
                errors.add(String.format("参数 '%s' 不能大于 %s",
                        paramDef.getName(), paramDef.getMaximum()));
            }
        }

        // 6. 长度校验（字符串）
        if (value instanceof String) {
            String strValue = (String) value;
            if (paramDef.getMinLength() != null && strValue.length() < paramDef.getMinLength()) {
                errors.add(String.format("参数 '%s' 长度不能小于 %d",
                        paramDef.getName(), paramDef.getMinLength()));
            }
            if (paramDef.getMaxLength() != null && strValue.length() > paramDef.getMaxLength()) {
                errors.add(String.format("参数 '%s' 长度不能大于 %d",
                        paramDef.getName(), paramDef.getMaxLength()));
            }
        }

        if (errors.isEmpty()) {
            return ValidationResult.success();
        } else {
            return ValidationResult.failure(errors);
        }
    }

    /**
     * 批量验证参数
     *
     * @param paramDefs 参数定义列表
     * @param arguments 实际参数值
     * @return 验证结果
     */
    public ValidationResult validateAll(List<ToolParameter> paramDefs, Map<String, Object> arguments) {
        List<String> allErrors = new ArrayList<>();

        for (ToolParameter paramDef : paramDefs) {
            Object value = arguments != null ? arguments.get(paramDef.getName()) : null;
            ValidationResult result = validate(paramDef, value);
            if (!result.isValid()) {
                allErrors.addAll(result.getErrors());
            }
        }

        if (allErrors.isEmpty()) {
            return ValidationResult.success();
        } else {
            return ValidationResult.failure(allErrors);
        }
    }

    /**
     * 类型校验
     */
    private boolean validateType(String expectedType, Object value) {
        if (expectedType == null) {
            return true;
        }

        return switch (expectedType.toLowerCase()) {
            case "string" -> value instanceof String;
            case "integer", "int" -> value instanceof Integer || value instanceof Long;
            case "number", "float", "double" -> value instanceof Number;
            case "boolean", "bool" -> value instanceof Boolean;
            case "array", "list" -> value instanceof List || value.getClass().isArray();
            case "object", "map" -> value instanceof Map;
            default -> true;
        };
    }

    /**
     * 验证结果
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        private ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, Collections.emptyList());
        }

        public static ValidationResult failure(List<String> errors) {
            return new ValidationResult(false, errors);
        }

        public static ValidationResult failure(String error) {
            return new ValidationResult(false, List.of(error));
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public String getErrorMessage() {
            return String.join("; ", errors);
        }
    }
}
