# mcp-redis MCP Redis 工具

提供 Redis 操作能力的 MCP 工具模块。

## 功能特性

- Key-Value 操作
- 列表操作
- 哈希操作
- 集合操作

## 工具列表

| 工具名 | 说明 |
|--------|------|
| `redis_get` | 获取值 |
| `redis_set` | 设置值 |
| `redis_delete` | 删除键 |
| `redis_list` | 列出键 |
| `redis_expire` | 设置过期时间 |

## 安全配置

```yaml
mcp:
  redis:
    security:
      # Key 前缀命名空间隔离
      key-prefix: "mcp:"
      # 禁止的命令
      forbidden-commands:
        - FLUSHALL
        - FLUSHDB
        - CONFIG
        - DEBUG
```

## 使用示例

### 获取值

```json
{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "redis_get",
    "arguments": {
      "key": "user:001"
    }
  },
  "id": 1
}
```

### 设置值

```json
{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "redis_set",
    "arguments": {
      "key": "user:001",
      "value": "{\"name\":\"张三\"}",
      "expire": 3600
    }
  },
  "id": 1
}
```

## 启动

```bash
# Windows
start.bat

# Linux/Mac
./start.sh
```

默认端口：40011
