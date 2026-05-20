# mcp-database MCP 数据库工具

提供数据库查询和管理能力的 MCP 工具模块。

## 功能特性

- SQL 查询执行（仅 SELECT）
- 数据库元数据获取
- 多数据源支持
- SQL 安全校验

## 工具列表

| 工具名 | 说明 |
|--------|------|
| `execute_query` | 执行 SQL 查询 |
| `get_schema` | 获取数据库结构 |
| `list_tables` | 列出所有表 |
| `describe_table` | 描述表结构 |

## 安全配置

```yaml
mcp:
  database:
    security:
      # SQL 类型白名单
      allowed-operations:
        - SELECT
        - SHOW
        - DESCRIBE
        - EXPLAIN
      # 禁止的操作
      forbidden-operations:
        - INSERT
        - UPDATE
        - DELETE
        - DROP
        - TRUNCATE
      # 表级访问控制
      allowed-tables:
        - users
        - orders
      forbidden-tables:
        - admin_users
        - passwords
```

## 使用示例

### 查询数据

```json
{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "execute_query",
    "arguments": {
      "sql": "SELECT * FROM users LIMIT 10"
    }
  },
  "id": 1
}
```

### 获取表结构

```json
{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "describe_table",
    "arguments": {
      "table": "users"
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

默认端口：40010
