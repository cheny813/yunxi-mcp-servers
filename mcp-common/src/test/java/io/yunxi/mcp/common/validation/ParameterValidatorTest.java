package io.yunxi.mcp.common.validation;

import io.yunxi.mcp.common.model.ToolParameter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MCP 工具参数验证器单元测试
 *
 * <p>
 * 验证必填校验、类型校验、枚举值校验、格式校验、范围校验等功能。
 * </p>
 */
class ParameterValidatorTest {

    private ParameterValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ParameterValidator();
    }

    @Nested
    @DisplayName("必填参数校验")
    class RequiredValidationTests {

        @Test
        @DisplayName("必填参数为空时应失败")
        void requiredParamNull_shouldFail() {
            ToolParameter param = ToolParameter.builder()
                    .name("username")
                    .type("string")
                    .required(true)
                    .build();

            ParameterValidator.ValidationResult result = validator.validate(param, null);

            assertFalse(result.isValid());
            assertTrue(result.getErrorMessage().contains("必填项"));
        }

        @Test
        @DisplayName("必填字符串为空时应失败")
        void requiredStringEmpty_shouldFail() {
            ToolParameter param = ToolParameter.builder()
                    .name("username")
                    .type("string")
                    .required(true)
                    .build();

            ParameterValidator.ValidationResult result = validator.validate(param, "");

            assertFalse(result.isValid());
            assertTrue(result.getErrorMessage().contains("必填项"));
        }

        @Test
        @DisplayName("必填参数有值时应通过")
        void requiredParamWithValue_shouldPass() {
            ToolParameter param = ToolParameter.builder()
                    .name("username")
                    .type("string")
                    .required(true)
                    .build();

            ParameterValidator.ValidationResult result = validator.validate(param, "john");

            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("非必填参数为空时应通过")
        void optionalParamNull_shouldPass() {
            ToolParameter param = ToolParameter.builder()
                    .name("description")
                    .type("string")
                    .required(false)
                    .build();

            ParameterValidator.ValidationResult result = validator.validate(param, null);

            assertTrue(result.isValid());
        }
    }

    @Nested
    @DisplayName("类型校验")
    class TypeValidationTests {

        @Test
        @DisplayName("字符串类型应接受字符串值")
        void stringType_shouldAcceptString() {
            ToolParameter param = ToolParameter.builder()
                    .name("text")
                    .type("string")
                    .build();

            assertTrue(validator.validate(param, "hello").isValid());
        }

        @Test
        @DisplayName("字符串类型不应接受数字")
        void stringType_shouldRejectNumber() {
            ToolParameter param = ToolParameter.builder()
                    .name("text")
                    .type("string")
                    .build();

            ParameterValidator.ValidationResult result = validator.validate(param, 123);

            assertFalse(result.isValid());
            assertTrue(result.getErrorMessage().contains("类型错误"));
        }

        @Test
        @DisplayName("整数类型应接受整数")
        void integerType_shouldAcceptInteger() {
            ToolParameter param = ToolParameter.builder()
                    .name("count")
                    .type("integer")
                    .build();

            assertTrue(validator.validate(param, 42).isValid());
            assertTrue(validator.validate(param, 42L).isValid());
        }

        @Test
        @DisplayName("整数类型不应接受小数")
        void integerType_shouldRejectDecimal() {
            ToolParameter param = ToolParameter.builder()
                    .name("count")
                    .type("integer")
                    .build();

            ParameterValidator.ValidationResult result = validator.validate(param, 3.14);

            assertFalse(result.isValid());
        }

        @Test
        @DisplayName("数字类型应接受任何数字")
        void numberType_shouldAcceptAnyNumber() {
            ToolParameter param = ToolParameter.builder()
                    .name("value")
                    .type("number")
                    .build();

            assertTrue(validator.validate(param, 42).isValid());
            assertTrue(validator.validate(param, 3.14).isValid());
            assertTrue(validator.validate(param, 42L).isValid());
        }

        @Test
        @DisplayName("布尔类型应接受布尔值")
        void booleanType_shouldAcceptBoolean() {
            ToolParameter param = ToolParameter.builder()
                    .name("enabled")
                    .type("boolean")
                    .build();

            assertTrue(validator.validate(param, true).isValid());
            assertTrue(validator.validate(param, false).isValid());
        }

        @Test
        @DisplayName("布尔类型不应接受字符串")
        void booleanType_shouldRejectString() {
            ToolParameter param = ToolParameter.builder()
                    .name("enabled")
                    .type("boolean")
                    .build();

            assertFalse(validator.validate(param, "true").isValid());
        }

        @Test
        @DisplayName("数组类型应接受列表")
        void arrayType_shouldAcceptList() {
            ToolParameter param = ToolParameter.builder()
                    .name("items")
                    .type("array")
                    .build();

            assertTrue(validator.validate(param, List.of("a", "b")).isValid());
        }

        @Test
        @DisplayName("数组类型应接受数组")
        void arrayType_shouldAcceptArray() {
            ToolParameter param = ToolParameter.builder()
                    .name("items")
                    .type("array")
                    .build();

            assertTrue(validator.validate(param, new String[]{"a", "b"}).isValid());
        }

        @Test
        @DisplayName("对象类型应接受Map")
        void objectType_shouldAcceptMap() {
            ToolParameter param = ToolParameter.builder()
                    .name("config")
                    .type("object")
                    .build();

            assertTrue(validator.validate(param, Map.of("key", "value")).isValid());
        }
    }

    @Nested
    @DisplayName("枚举值校验")
    class EnumValidationTests {

        @Test
        @DisplayName("值在枚举列表中时应通过")
        void valueInEnum_shouldPass() {
            ToolParameter param = ToolParameter.builder()
                    .name("status")
                    .type("string")
                    .enumValues(List.of("active", "inactive", "pending"))
                    .build();

            assertTrue(validator.validate(param, "active").isValid());
        }

        @Test
        @DisplayName("值不在枚举列表中时应失败")
        void valueNotInEnum_shouldFail() {
            ToolParameter param = ToolParameter.builder()
                    .name("status")
                    .type("string")
                    .enumValues(List.of("active", "inactive"))
                    .build();

            ParameterValidator.ValidationResult result = validator.validate(param, "unknown");

            assertFalse(result.isValid());
            assertTrue(result.getErrorMessage().contains("不在允许范围内"));
        }
    }

    @Nested
    @DisplayName("格式校验（正则）")
    class PatternValidationTests {

        @Test
        @DisplayName("符合正则格式的值应通过")
        void valueMatchingPattern_shouldPass() {
            ToolParameter param = ToolParameter.builder()
                    .name("email")
                    .type("string")
                    .pattern("^[A-Za-z0-9+_.-]+@(.+)$")
                    .build();

            assertTrue(validator.validate(param, "test@example.com").isValid());
        }

        @Test
        @DisplayName("不符合正则格式的值应失败")
        void valueNotMatchingPattern_shouldFail() {
            ToolParameter param = ToolParameter.builder()
                    .name("email")
                    .type("string")
                    .pattern("^[A-Za-z0-9+_.-]+@(.+)$")
                    .build();

            ParameterValidator.ValidationResult result = validator.validate(param, "invalid-email");

            assertFalse(result.isValid());
            assertTrue(result.getErrorMessage().contains("格式错误"));
        }
    }

    @Nested
    @DisplayName("范围校验")
    class RangeValidationTests {

        @Test
        @DisplayName("数值在最小值范围内应通过")
        void numberAboveMinimum_shouldPass() {
            ToolParameter param = ToolParameter.builder()
                    .name("age")
                    .type("integer")
                    .minimum(0.0)
                    .build();

            assertTrue(validator.validate(param, 18).isValid());
        }

        @Test
        @DisplayName("数值低于最小值应失败")
        void numberBelowMinimum_shouldFail() {
            ToolParameter param = ToolParameter.builder()
                    .name("age")
                    .type("integer")
                    .minimum(0.0)
                    .build();

            ParameterValidator.ValidationResult result = validator.validate(param, -5);

            assertFalse(result.isValid());
            assertTrue(result.getErrorMessage().contains("不能小于"));
        }

        @Test
        @DisplayName("数值在最大值范围内应通过")
        void numberBelowMaximum_shouldPass() {
            ToolParameter param = ToolParameter.builder()
                    .name("score")
                    .type("number")
                    .maximum(100.0)
                    .build();

            assertTrue(validator.validate(param, 85).isValid());
        }

        @Test
        @DisplayName("数值超过最大值应失败")
        void numberAboveMaximum_shouldFail() {
            ToolParameter param = ToolParameter.builder()
                    .name("score")
                    .type("number")
                    .maximum(100.0)
                    .build();

            ParameterValidator.ValidationResult result = validator.validate(param, 150);

            assertFalse(result.isValid());
            assertTrue(result.getErrorMessage().contains("不能大于"));
        }
    }

    @Nested
    @DisplayName("长度校验")
    class LengthValidationTests {

        @Test
        @DisplayName("字符串长度在最小范围内应通过")
        void stringAboveMinLength_shouldPass() {
            ToolParameter param = ToolParameter.builder()
                    .name("password")
                    .type("string")
                    .minLength(8)
                    .build();

            assertTrue(validator.validate(param, "password123").isValid());
        }

        @Test
        @DisplayName("字符串长度低于最小值应失败")
        void stringBelowMinLength_shouldFail() {
            ToolParameter param = ToolParameter.builder()
                    .name("password")
                    .type("string")
                    .minLength(8)
                    .build();

            ParameterValidator.ValidationResult result = validator.validate(param, "short");

            assertFalse(result.isValid());
            assertTrue(result.getErrorMessage().contains("长度不能小于"));
        }

        @Test
        @DisplayName("字符串长度在最大范围内应通过")
        void stringBelowMaxLength_shouldPass() {
            ToolParameter param = ToolParameter.builder()
                    .name("username")
                    .type("string")
                    .maxLength(20)
                    .build();

            assertTrue(validator.validate(param, "john_doe").isValid());
        }

        @Test
        @DisplayName("字符串长度超过最大值应失败")
        void stringAboveMaxLength_shouldFail() {
            ToolParameter param = ToolParameter.builder()
                    .name("username")
                    .type("string")
                    .maxLength(10)
                    .build();

            ParameterValidator.ValidationResult result = validator.validate(param, "very_long_username");

            assertFalse(result.isValid());
            assertTrue(result.getErrorMessage().contains("长度不能大于"));
        }
    }

    @Nested
    @DisplayName("批量校验")
    class BatchValidationTests {

        @Test
        @DisplayName("所有参数有效时应通过")
        void allParamsValid_shouldPass() {
            List<ToolParameter> params = List.of(
                    ToolParameter.builder().name("name").type("string").required(true).build(),
                    ToolParameter.builder().name("age").type("integer").required(true).minimum(0.0).build()
            );

            Map<String, Object> arguments = Map.of(
                    "name", "John",
                    "age", 25
            );

            ParameterValidator.ValidationResult result = validator.validateAll(params, arguments);

            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("多个参数无效时应收集所有错误")
        void multipleParamsInvalid_shouldCollectAllErrors() {
            List<ToolParameter> params = List.of(
                    ToolParameter.builder().name("name").type("string").required(true).build(),
                    ToolParameter.builder().name("age").type("integer").required(true).minimum(0.0).build()
            );

            Map<String, Object> arguments = Map.of(
                    "name", "",
                    "age", -5
            );

            ParameterValidator.ValidationResult result = validator.validateAll(params, arguments);

            assertFalse(result.isValid());
            assertEquals(2, result.getErrors().size());
        }

        @Test
        @DisplayName("空参数定义列表应通过")
        void emptyParamDefs_shouldPass() {
            ParameterValidator.ValidationResult result = validator.validateAll(List.of(), Map.of());

            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("null参数值应正确处理")
        void nullArguments_shouldHandleGracefully() {
            List<ToolParameter> params = List.of(
                    ToolParameter.builder().name("optional").type("string").required(false).build()
            );

            ParameterValidator.ValidationResult result = validator.validateAll(params, null);

            assertTrue(result.isValid());
        }
    }

    @Nested
    @DisplayName("ValidationResult 测试")
    class ValidationResultTests {

        @Test
        @DisplayName("成功结果应正确创建")
        void successResult_shouldBeCreated() {
            ParameterValidator.ValidationResult result = ParameterValidator.ValidationResult.success();

            assertTrue(result.isValid());
            assertTrue(result.getErrors().isEmpty());
        }

        @Test
        @DisplayName("单错误失败结果应正确创建")
        void singleErrorFailure_shouldBeCreated() {
            ParameterValidator.ValidationResult result = ParameterValidator.ValidationResult.failure("error message");

            assertFalse(result.isValid());
            assertEquals(1, result.getErrors().size());
            assertEquals("error message", result.getErrorMessage());
        }

        @Test
        @DisplayName("多错误失败结果应正确创建")
        void multipleErrorsFailure_shouldBeCreated() {
            List<String> errors = List.of("error1", "error2");
            ParameterValidator.ValidationResult result = ParameterValidator.ValidationResult.failure(errors);

            assertFalse(result.isValid());
            assertEquals(2, result.getErrors().size());
            assertTrue(result.getErrorMessage().contains("error1"));
            assertTrue(result.getErrorMessage().contains("error2"));
        }
    }
}
