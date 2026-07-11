# MCP 协议类型说明文档

> **v2.0 说明**：v2.0 已移除 v1.0 的自定义类 `ToolDefinition`、`ToolParameter`、`McpRequest`、`McpResponse`、`McpError`，
> 改由官方 MCP Java SDK 0.17.0 的数据模型承载：`McpSchema.Tool`、`McpSchema.JsonSchema`、`McpSchema.CallToolResult` 等。
> 下述 JSON 结构描述的是标准 MCP 线缆（wire）格式，服务端由官方 SDK 自动序列化/反序列化，业务代码无需直接构造这些对象。

## 协议版本

**MCP Protocol Version**: 2024-11-05

## 数据模型

### 工具定义（对应官方 SDK `McpSchema.Tool`）

工具定义结构。

```json
{
  "name": "query_database",
  "description": "查询数据库",
  "inputSchema": {
    "type": "object",
    "properties": {
      "sql": {
        "type": "string",
        "description": "SQL 查询语句"
      }
    },
    "required": ["sql"]
  }
}
```

> v1.0 中参数包裹在 `parameters` 字段下；v2.0 改为标准字段 `inputSchema`（与官方 SDK 一致）。

### 参数定义（对应 `JsonSchema` 的 `properties` 项）

单个参数的 JSON Schema 定义。

```json
{
  "sql": {
    "type": "string",
    "description": "SQL 查询语句",
    "pattern": "^SELECT.*",
    "minLength": 1,
    "maxLength": 1000
  }
}
```

> v2.0 的必填约束统一通过 `inputSchema.required`（顶层数组）表达，而非每个参数内部的 `required: true`。

### 请求（JSON-RPC 2.0，由官方 SDK 处理）

`tools/call` 请求格式（SDK 内部使用，业务无需手动构造）。

```json
{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "query_database",
    "arguments": {
      "sql": "SELECT * FROM users"
    }
  },
  "id": 1
}
```

### 响应（对应官方 SDK `McpSchema.CallToolResult`）

`tools/call` 响应格式。

```json
{
  "jsonrpc": "2.0",
  "result": {
    "content": [
      { "type": "text", "text": "查询结果..." }
    ],
    "isError": false
  },
  "id": 1
}
```

> 工具执行失败时将 `isError` 置为 `true` 并返回错误文本；在 `ToolHandler.execute` 中直接抛出 `RuntimeException` 即可，框架层自动包装。

### 错误（对应官方 SDK `McpSchema.JSONRPCResponse.error`）

JSON-RPC 错误格式。

```json
{
  "jsonrpc": "2.0",
  "error": {
    "code": -32602,
    "message": "Invalid params",
    "data": null
  },
  "id": 1
}
```

## 错误码

| 错误码 | 名称 | 说明 |
|--------|------|------|
| -32700 | PARSE_ERROR | 解析错误 |
| -32600 | INVALID_REQUEST | 无效请求 |
| -32601 | METHOD_NOT_FOUND | 方法未找到 |
| -32602 | INVALID_PARAMS | 无效参数 |
| -32603 | INTERNAL_ERROR | 内部错误 |
| -32000 | SERVER_ERROR | 服务端错误 |

## 方法列表

| 方法 | 说明 |
|------|------|
| `initialize` | 初始化连接 |
| `tools/list` | 获取工具列表 |
| `tools/call` | 调用工具 |
| `ping` | 心跳检测 |

## HTTP 端点（v2.0 SSE 传输）

v2.0 使用官方 MCP SDK 的 SSE 传输，仅暴露以下两个端点：

| 端点 | 方法 | 说明 |
|------|------|------|
| `/mcp/sse` | GET | 建立 SSE 连接，推送服务端消息 |
| `/mcp/message` | POST | 客户端发送 JSON-RPC 消息 |

> v1.0 的 `/mcp`（POST JSON-RPC）、`/mcp/health`、`/mcp/tools`、`/mcp/info` 自定义端点已在 v2.0 移除。工具列表/调用统一走 MCP 协议方法（`tools/list`、`tools/call`）。
