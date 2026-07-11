# MCP 工具参数校验指南（v2.0）

## 概述

v2.0 基于官方 MCP Java SDK 0.17.0，**不再提供独立的 `ParameterValidator` 框架**（v1.0 的 `ParameterValidator` / `AbstractMcpController` 等类已移除）。参数校验由两部分构成：

1. **声明式 Schema 校验**：在 `ToolHandler.getInputSchema()` 中返回标准 JSON Schema，由官方 SDK / 客户端负责 `type`、`required`、`pattern`、`enum` 等基础校验。
2. **业务校验**：在 `ToolHandler.execute(Map)` 方法中手动完成，校验失败时直接抛出 `RuntimeException`，框架层会自动包装为 `McpSchema.CallToolResult`（`isError=true`，内容含错误信息）。

## 参数定义（JSON Schema）

通过 `getInputSchema()` 返回 `Map<String, Object>`，结构为标准 JSON Schema：

```java
@Override
public Map<String, Object> getInputSchema() {
    return Map.of(
        "type", "object",
        "properties", Map.of(
            "user_id", Map.of(
                "type", "string",
                "description", "用户ID",
                "minLength", 1,
                "maxLength", 50,
                "pattern", "^[a-zA-Z0-9_]+$"
            ),
            "age", Map.of(
                "type", "integer",
                "description", "年龄",
                "minimum", 0,
                "maximum", 150
            ),
            "status", Map.of(
                "type", "string",
                "description", "状态",
                "enum", List.of("active", "inactive", "pending")
            )
        ),
        "required", List.of("user_id")
    );
}
```

## 支持的校验规则（JSON Schema 属性）

| 属性 | 说明 | 适用类型 |
|------|------|----------|
| `required` | 必填参数（顶层数组） | 所有类型 |
| `type` | 参数类型 | 所有类型 |
| `pattern` | 正则表达式 | string |
| `minLength` | 最小长度 | string |
| `maxLength` | 最大长度 | string |
| `minimum` | 最小值 | number/integer |
| `maximum` | 最大值 | number/integer |
| `enum` | 枚举值列表 | 所有类型 |

## 支持的类型

- `string` - 字符串
- `integer` / `int` - 整数
- `number` / `float` / `double` - 数值
- `boolean` / `bool` - 布尔值
- `array` / `list` - 数组
- `object` / `map` - 对象

## 错误响应

当 `execute` 抛出 `RuntimeException` 时，框架层自动包装为：

```json
{
  "jsonrpc": "2.0",
  "result": {
    "content": [
      { "type": "text", "text": "Error: 参数 'user_id' 为必填项" }
    ],
    "isError": true
  },
  "id": 1
}
```

> v1.0 中参数校验失败会返回 JSON-RPC 错误码 `-32602`；v2.0 改为在工具结果中以 `isError=true` 表达，更符合 MCP 协议约定。

## 使用示例

### 1. 定义带校验的工具

```java
@Component
public class QueryUserTool implements ToolHandler {

    @Override
    public String getName() {
        return "query_user";
    }

    @Override
    public String getDescription() {
        return "查询用户信息";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "user_id", Map.of(
                    "type", "string",
                    "description", "用户ID",
                    "pattern", "^[a-zA-Z0-9_]+$"
                )
            ),
            "required", List.of("user_id")
        );
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        // 业务校验：必填参数缺失直接抛异常
        String userId = (String) arguments.get("user_id");
        if (userId == null || userId.isBlank()) {
            throw new RuntimeException("参数 'user_id' 为必填项");
        }
        if (!userId.matches("^[a-zA-Z0-9_]+$")) {
            throw new RuntimeException("参数 'user_id' 格式错误，期望格式: ^[a-zA-Z0-9_]+$");
        }
        // ... 执行业务逻辑，返回结果文本
        return "user: " + userId;
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

**响应（工具结果 isError=true）：**

```json
{
  "jsonrpc": "2.0",
  "result": {
    "content": [
      { "type": "text", "text": "Error: 参数 'user_id' 为必填项" }
    ],
    "isError": true
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
  "result": {
    "content": [
      { "type": "text", "text": "Error: 参数 'user_id' 格式错误，期望格式: ^[a-zA-Z0-9_]+$" }
    ],
    "isError": true
  },
  "id": 1
}
```

## 自定义校验

v2.0 没有可重写的 `validateParameters` 钩子。如需集中式校验逻辑，可将校验封装为工具类或基类方法，在 `execute` 开头调用，校验失败抛 `RuntimeException`：

```java
private void validate(Map<String, Object> arguments) {
    String userId = (String) arguments.get("user_id");
    if (userId == null || userId.isBlank()) {
        throw new RuntimeException("参数 'user_id' 为必填项");
    }
    // 更多自定义校验规则...
}
```

## 禁用/简化校验

若某工具无需校验，直接忽略参数或仅做最小判空即可，无需任何配置开关。

## 最佳实践

1. **声明完整 Schema**：在 `getInputSchema()` 中明确 `type`、`description`、`required`、以及 `pattern`/`enum` 等约束，帮助客户端与模型理解参数。
2. **业务校验放 execute**：涉及取值域、业务规则等无法用 JSON Schema 表达的校验，在 `execute` 内完成并抛异常。
3. **错误信息清晰**：异常 message 应直接说明哪个参数、什么错误，便于模型自我修正。
4. **使用枚举**：有限选项的参数优先用 `enum` 约束取值范围。
