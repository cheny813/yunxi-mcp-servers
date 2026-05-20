# mcp-filesystem MCP 文件系统工具

提供文件系统操作能力的 MCP 工具模块。

## 功能特性

- 文件读写
- 目录列表
- 文件搜索
- 目录白名单控制

## 工具列表

| 工具名 | 说明 |
|--------|------|
| `read_file` | 读取文件 |
| `write_file` | 写入文件 |
| `list_directory` | 列出目录 |
| `search_files` | 搜索文件 |
| `get_file_info` | 获取文件信息 |

## 安全配置

```yaml
mcp:
  filesystem:
    security:
      # 允许访问的目录白名单
      allowed-directories:
        - /data/workspace
        - /data/uploads
      # 禁止访问的目录
      forbidden-directories:
        - /etc
        - /root
        - /var/log
      # 允许的文件扩展名
      allowed-extensions:
        - .txt
        - .md
        - .json
        - .yaml
        - .java
        - .py
```

## 使用示例

### 读取文件

```json
{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "read_file",
    "arguments": {
      "path": "/data/workspace/README.md"
    }
  },
  "id": 1
}
```

### 列出目录

```json
{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "list_directory",
    "arguments": {
      "path": "/data/workspace"
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

默认端口：40012
