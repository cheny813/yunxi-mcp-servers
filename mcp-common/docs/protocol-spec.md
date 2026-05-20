# MCP 协议类型说明文档

## 协议版本

**MCP Protocol Version**: 2024-11-05

## 数据模型

### ToolDefinition

工具定义结构。

```json
{
  "name": "query_database",
  "description": "查询数据库",
  "parameters": {
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

### ToolParameter

工具参数定义。

```json
{
  "name": "sql",
  "type": "string",
  "description": "SQL 查询语句",
  "required": true,
  "pattern": "^SELECT.*",
  "minLength": 1,
  "maxLength": 1000
}
```

### McpRequest

JSON-RPC 请求格式。

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
  "id": 1,
  "traceId": "trace-123"
}
```

### McpResponse

JSON-RPC 响应格式。

```json
{
  "jsonrpc": "2.0",
  "result": {
    "content": [...],
    "isError": false
  },
  "id": 1
}
```

### McpError

错误格式。

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

## HTTP 端点

| 端点 | 方法 | 说明 |
|------|------|------|
| `/mcp` | POST | JSON-RPC 请求 |
| `/mcp/sse` | GET | SSE 连接 |
| `/mcp/message` | POST | SSE 消息 |
| `/mcp/health` | GET | 健康检查 |
| `/mcp/tools` | GET | 工具列表 |
| `/mcp/info` | GET | 服务器信息 |
