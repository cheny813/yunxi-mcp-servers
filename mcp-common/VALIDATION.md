# MCP 工具参数校验指南

## 概述

mcp-common 提供统一的参数校验机制，支持必填校验、类型校验、格式校验、枚举值校验等多种校验规则。

## 参数定义

在定义工具时，可以通过 `ToolParameter` 声明参数约束：

```java
ToolDefinition definition = ToolDefinition.builder()
    .name("query_user")
    .description("查询用户信息")
    .parameters(List.of(
        ToolParameter.builder()
            .name("user_id")
            .description("用户ID")
            .type("string")
            .required(true)
            .minLength(1)
            .maxLength(50)
            .pattern("^[a-zA-Z0-9_]+$")
            .build(),
        ToolParameter.builder()
            .name("age")
            .description("年龄")
            .type("integer")
            .minimum(0.0)
            .maximum(150.0)
            .build(),
        ToolParameter.builder()
            .name("status")
            .description("状态")
            .type("string")
            .enumValues(List.of("active", "inactive", "pending"))
            .build()
    ))
    .build();
```

## 支持的校验规则

| 属性 | 说明 | 适用类型 |
|------|------|----------|
| `required` | 是否必填 | 所有类型 |
| `type` | 参数类型 | 所有类型 |
| `pattern` | 正则表达式 | string |
| `minLength` | 最小长度 | string |
| `maxLength` | 最大长度 | string |
| `minimum` | 最小值 | number/integer |
| `maximum` | 最大值 | number/integer |
| `enumValues` | 枚举值列表 | 所有类型 |

## 支持的类型

- `string` - 字符串
- `integer` / `int` - 整数
- `number` / `float` / `double` - 数值
- `boolean` / `bool` - 布尔值
- `array` / `list` - 数组
- `object` / `map` - 对象

## 错误响应

当参数校验失败时，返回标准错误格式：

```json
{
  "jsonrpc": "2.0",
  "error": {
    "code": -32602,
    "message": "参数校验失败: 参数 'user_id' 为必填项; 参数 'age' 不能大于 150",
    "data": null
  },
  "id": 1
}
```

## 使用示例

### 1. 定义带校验的工具

```java
@Component
public class QueryUserTool implements ToolHandler {

    @Override
    public ToolDefinition getDefinition() {
        return ToolDefinition.builder()
            .name("query_user")
            .description("查询用户信息")
            .parameters(List.of(
                ToolParameter.builder()
                    .name("user_id")
                    .description("用户ID")
                    .type("string")
                    .required(true)
                    .pattern("^[a-zA-Z0-9_]+$")
                    .build()
            ))
            .build();
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        // 参数已通过校验，直接使用
        String userId = (String) arguments.get("user_id");
        // ... 执行业务逻辑
        return ToolResult.success(result);
    }
}
```

### 2. 测试校验

**请求（缺少必填参数）：**

```json
{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "query_user",
    "arguments": {}
  },
  "id": 1
}
```

**响应：**

```json
{
  "jsonrpc": "2.0",
  "error": {
    "code": -32602,
    "message": "参数校验失败: 参数 'user_id' 为必填项"
  },
  "id": 1
}
```

**请求（格式错误）：**

```json
{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "query_user",
    "arguments": {
      "user_id": "user@123"
    }
  },
  "id": 1
}
```

**响应：**

```json
{
  "jsonrpc": "2.0",
  "error": {
    "code": -32602,
    "message": "参数校验失败: 参数 'user_id' 格式错误，期望格式: ^[a-zA-Z0-9_]+$"
  },
  "id": 1
}
```

## 自定义校验

如果需要更复杂的校验逻辑，可以重写 `AbstractMcpEndpoint` 的 `validateParameters` 方法：

```java
@RestController
public class CustomMcpController extends AbstractMcpController {

    @Autowired
    private ParameterValidator parameterValidator;

    @Override
    protected ParameterValidator.ValidationResult validateParameters(
            List<ToolParameter> paramDefs, Map<String, Object> arguments) {
        // 使用默认校验
        ParameterValidator.ValidationResult result = 
            parameterValidator.validateAll(paramDefs, arguments);
        
        if (!result.isValid()) {
            return result;
        }
        
        // 添加自定义校验逻辑
        // ...
        
        return ParameterValidator.ValidationResult.success();
    }
}
```

## 禁用校验

如需禁用参数校验，可在子类中重写方法并直接返回成功：

```java
@Override
protected ParameterValidator.ValidationResult validateParameters(
        List<ToolParameter> paramDefs, Map<String, Object> arguments) {
    return ParameterValidator.ValidationResult.success();
}
```

## 最佳实践

1. **明确声明参数类型**：帮助用户理解参数格式
2. **使用合理的约束**：避免过于严格或宽松的校验
3. **提供清晰的描述**：帮助用户理解参数用途
4. **使用枚举值**：对于有限选项的参数，使用 `enumValues` 限制取值范围
