# MCP 错误码规范

## JSON-RPC 标准错误码

| 错误码 | 名称 | HTTP 状态码 | 说明 |
|--------|------|-------------|------|
| -32700 | PARSE_ERROR | 400 | 解析错误，无效的 JSON |
| -32600 | INVALID_REQUEST | 400 | 无效请求，无效的 JSON-RPC |
| -32601 | METHOD_NOT_FOUND | 404 | 方法未找到 |
| -32602 | INVALID_PARAMS | 400 | 无效参数 |
| -32603 | INTERNAL_ERROR | 500 | 内部错误 |

## 服务端错误码范围

| 错误码范围 | 说明 |
|------------|------|
| -32000 ~ -32099 | 服务端错误 |

## MCP 自定义错误码

| 错误码 | 名称 | HTTP 状态码 | 说明 |
|--------|------|-------------|------|
| -32001 | TOOL_NOT_FOUND | 404 | 工具未找到 |
| -32002 | TOOL_EXECUTION_ERROR | 500 | 工具执行错误 |
| -32003 | AUTHENTICATION_FAILED | 401 | 认证失败 |
| -32004 | AUTHORIZATION_FAILED | 403 | 授权失败 |
| -32005 | RATE_LIMIT_EXCEEDED | 429 | 请求过于频繁 |
| -32006 | VALIDATION_ERROR | 400 | 参数校验错误 |

## 错误响应格式

```json
{
  "jsonrpc": "2.0",
  "error": {
    "code": -32602,
    "message": "Invalid params",
    "data": {
      "details": "参数 'sql' 为必填项",
      "field": "sql"
    }
  },
  "id": 1,
  "traceId": "trace-123"
}
```

## 使用示例

### 参数缺失

```json
{
  "jsonrpc": "2.0",
  "error": {
    "code": -32602,
    "message": "Missing required parameter: sql"
  },
  "id": 1
}
```

### 认证失败

```json
{
  "jsonrpc": "2.0",
  "error": {
    "code": -32003,
    "message": "Authentication failed: Invalid token"
  },
  "id": 1
}
```

### 限流

```json
{
  "jsonrpc": "2.0",
  "error": {
    "code": -32005,
    "message": "Rate limit exceeded",
    "data": {
      "retryAfter": 60
    }
  },
  "id": 1
}
```
