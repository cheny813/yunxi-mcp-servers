# MCP Memory Tool Server

## 概述

MCP Memory Tool Server 是基于 MCP (Model Context Protocol) 协议的独立服务，提供智能体的记忆管理功能。

## 功能特性

### 记忆管理工具

1. **memory_add** - 添加新记忆
   - 支持多用户隔离（每个用户独立的 MEMORY.md 和 USER.md）
   - 支持添加到 MEMORY.md 或 USER.md
   - 自动字符限制检查
   - 返回记忆使用统计

2. **memory_replace** - 替换现有记忆
   - 支持多用户隔离
   - 支持按条目ID替换
   - 原子写入保证一致性

3. **memory_remove** - 删除记忆条目
   - 支持多用户隔离
   - 支持精确删除
   - 失败时返回错误信息

4. **memory_get** - 获取记忆内容
   - 支持多用户隔离
   - 返回指定存储的所有内容
   - 包含使用统计信息
   - 支持上下文过滤

### 系统功能

- 健康检查端点
- 工具列表查询
- 工具定义查询
- 服务器信息查询

## 快速开始

### 1. 编译和打包

```bash
cd mcp-memory-tool
mvn clean package
```

### 2. 运行服务器

```bash
java -jar target/mcp-memory-tool-1.0.0.jar
```

### 3. 配置端口

默认端口：40304

可通过环境变量覆盖：
```bash
export MCP_PORT=40305
java -jar target/mcp-memory-tool-1.0.0.jar
```

## API 端点

### 健康检查
```
GET /mcp/health
```

响应：
```json
{
  "status": "UP",
  "service": "MCP Memory Tool Server"
}
```

### 服务器信息
```
GET /mcp/info
```

响应：
```json
{
  "name": "MCP Memory Tool Server",
  "version": "1.0.0",
  "description": "Memory management tools for MEMORY.md and USER.md",
  "tools": ["memory_add", "memory_replace", "memory_remove", "memory_get"],
  "endpoints": {
    "http": {
      "url": "/mcp",
      "method": "POST",
      "contentType": "application/json"
    },
    "sse": {
      "connectUrl": "/mcp/sse",
      "messageUrl": "/mcp/message",
      "method": "POST",
      "contentType": "application/json"
    }
  }
}
```

### 工具列表
```
GET /mcp/tools
```

响应（MCP 协议格式）：
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "tools": [
      {
        "name": "memory_add",
        "description": "添加新的记忆条目（支持多用户隔离，可选文件或Milvus存储）",
        "inputSchema": {
          "type": "object",
          "properties": {
            "userId": {
              "type": "string",
              "description": "用户ID（必填，用于多用户隔离）"
            },
            "target": {
              "type": "string",
              "description": "目标存储：'memory' 或 'user'",
              "enum": ["memory", "user"]
            },
            "category": {
              "type": "string",
              "description": "记忆分类（如：preference, fact, instruction, experience）"
            },
            "content": {
              "type": "string",
              "description": "记忆内容"
            }
          },
          "required": ["userId", "target", "category", "content"]
        }
      },
      ...
    ]
  }
}
```

### HTTP 模式调用工具
```
POST /mcp
Content-Type: application/json
```

请求体（MCP 协议格式）：
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tools/call",
  "params": {
    "name": "memory_add",
    "arguments": {
      "userId": "user-123",
      "target": "memory",
      "category": "preference",
      "content": "我喜欢使用 Python 进行数据分析"
    }
  }
}
```

响应：
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "content": [
      {
        "type": "text",
        "text": "记忆已成功添加\n- 用户ID: user-123\n- 条目ID: 2024-01-15T10:30:00\n- 目标: memory\n- 分类: preference\n\n使用情况:\n- MEMORY.md: 120/2200 (5.0%)\n- USER.md: 45/1375 (3.0%)\n"
      }
    ],
    "isError": false
  }
}
```

### SSE 模式连接
```
GET /mcp/sse
```

响应：Server-Sent Events 流

### SSE 模式发送消息
```
POST /mcp/message
Content-Type: application/json
```

请求体和响应格式与 HTTP 模式相同。

## API 端点

### 健康检查
```
GET /mcp/memory/health
```

响应：
```json
{
  "status": "UP",
  "service": "MCP MemoryToolServer",
  "tools": 4,
  "timestamp": 1640995200000
}
```

### 工具列表
```
GET /mcp/memory/tools
```

响应：
```json
{
  "success": true,
  "message": "获取工具列表成功",
  "data": [
    {
      "name": "memory_add",
      "description": "添加新的记忆条目到 MEMORY.md 或 USER.md",
      "parameters": {...}
    },
    ...
  ]
}
```

### 调用工具
```
POST /mcp/memory/invoke
Content-Type: application/json
```

请求体：
```json
{
  "toolName": "memory_add",
  "arguments": {
    "userId": "user-123",
    "target": "memory",
    "category": "preference",
    "content": "我喜欢使用 Python 进行数据分析"
  }
}
```

响应：
```json
{
  "success": true,
  "message": "记忆已成功添加",
  "data": {
    "entryId": "2024-01-15T10:30:00",
    "target": "memory",
    "usage": {
      "memory": {
        "current": 120,
        "limit": 2200,
        "percentage": 5
      },
      "user": {
        "current": 45,
        "limit": 1375,
        "percentage": 3
      }
    }
  }
}
```

## 配置说明

### 存储引擎选择

系统支持两种存储引擎，可通过配置切换：

#### 1. 文件存储（File Storage）- 默认

**特点：**
- 使用 MEMORY.md + USER.md 文件存储
- 人类可读，方便调试
- 有字符限制（MEMORY.md 2200 字符，USER.md 1375 字符）
- 集群环境需要共享文件系统（NFS/GlusterFS）

**适用场景：**
- 小规模部署（单用户或少用户）
- 开发测试环境
- 需要手动查看和编辑记忆文件

#### 2. Milvus 向量存储（Vector Storage）

**特点：**
- 使用 Milvus 向量数据库存储
- 支持语义检索，智能匹配相关记忆
- 海量存储能力，无字符限制
- 原生支持多租户，集群友好
- 高性能，适合大规模部署

**适用场景：**
- 生产环境多用户部署
- 需要智能语义搜索
- 海量记忆存储需求

### application.yml

```yaml
server:
  port: 40304

# 存储引擎配置
memory:
  store:
    type: ${MEMORY_STORE_TYPE:file}  # file: 文件存储, milvus: 向量存储

yunxi:
  environment:
    mode: prod  # 环境模式：dev（开发）或 prod（生产）

  learning-loop:
    memory:
      enabled: true
      storage-path-dev: ./data/memory  # 开发模式：本地存储（仅 file 模式）
      storage-path-prod: /shared/data/memory  # 生产模式：共享存储（仅 file 模式）
      max-memory-length: 2200  # 每个 MEMORY.md 的最大字符数（仅 file 模式）
      max-user-memory-length: 1375  # 每个 USER.md 的最大字符数（仅 file 模式）

# Milvus 配置（仅当 memory.store.type=milvus 时生效）
milvus:
  enabled: ${MILVUS_ENABLED:false}
  host: ${MILVUS_HOST:localhost}
  port: ${MILVUS_PORT:19530}
  uri: ${MILVUS_URI:http://localhost:19530}
  token: ${MILVUS_TOKEN:}
  collection-name: ${MILVUS_COLLECTION_NAME:agent_memories}
  embedding-dimension: ${MILVUS_EMBEDDING_DIMENSION:1024}

# Embedding 配置（仅当使用 Milvus 时需要）
embedding:
  provider: ${EMBEDDING_PROVIDER:mock}  # mock, dashscope, openai
  default-dimension: ${EMBEDDING_DIMENSION:1024}
  dashscope:
    api-key: ${DASHSCOPE_API_KEY:}
    model: ${DASHSCOPE_MODEL:text-embedding-v3}
  openai:
    api-key: ${OPENAI_API_KEY:}
    model: ${OPENAI_MODEL:text-embedding-ada-002}
  mock:
    dimension: 1024

# Spring Profile 配置
spring:
  profiles:
    active: ${ENVIRONMENT:prod}  # 通过环境变量控制
```

### 存储引擎切换

#### 切换到 Milvus 向量存储

**方式1：环境变量（推荐）**
```bash
export MEMORY_STORE_TYPE=milvus
export MILVUS_ENABLED=true
export EMBEDDING_PROVIDER=dashscope
export DASHSCOPE_API_KEY=your-dashscope-api-key
java -jar target/mcp-memory-tool-1.0.0.jar
```

**方式2：启动脚本**
```bash
# Windows
set MEMORY_STORE_TYPE=milvus
set MILVUS_ENABLED=true
set EMBEDDING_PROVIDER=dashscope
set DASHSCOPE_API_KEY=your-dashscope-api-key
start.bat

# Linux/Mac
export MEMORY_STORE_TYPE=milvus
export MILVUS_ENABLED=true
export EMBEDDING_PROVIDER=dashscope
export DASHSCOPE_API_KEY=your-dashscope-api-key
./start.sh
```

**Milvus 部署要求：**
1. 安装 Milvus（Docker 或 Kubernetes）
2. 配置 DashScope API Key 或其他 Embedding Provider
3. 确保 Milvus 服务可访问

#### Milvus Embedding Provider

**Mock Provider（测试用）：**
```yaml
embedding:
  provider: mock
  mock:
    dimension: 1024
```
- 不需要 API Key
- 使用哈希生成固定向量
- 适合测试和开发

**DashScope Provider（推荐）：**
```yaml
embedding:
  provider: dashscope
  dashscope:
    api-key: your-dashscope-api-key
    model: text-embedding-v3
```
- 需要 DashScope API Key
- 向量维度：1024
- 中文语义效果好

**OpenAI Provider：**
```yaml
embedding:
  provider: openai
  openai:
    api-key: your-openai-api-key
    model: text-embedding-ada-002
```
- 需要 OpenAI API Key
- 向量维度：1536
- 英文语义效果好

### 环境模式（文件存储模式）

**开发模式（dev）：**
- 使用本地存储 `./data/memory`
- 每个实例独立数据，适合本地开发和测试
- 不需要共享文件系统

**生产模式（prod）：**
- 使用共享存储 `/shared/data/memory`
- 多实例共享数据，支持集群部署
- 需要配置 NFS/GlusterFS 等共享文件系统

**模式切换：**

方式1：环境变量
```bash
# 开发模式
ENVIRONMENT=dev java -jar target/mcp-memory-tool-1.0.0.jar

# 生产模式
ENVIRONMENT=prod java -jar target/mcp-memory-tool-1.0.0.jar
```

方式2：启动脚本
```bash
# 开发模式
./start-dev.sh

# 生产模式
./start.sh
```

方式3：Docker 环境变量
```yaml
environment:
  - ENVIRONMENT=prod
  - MEMORY_STORAGE_PATH_PROD=/shared/data/memory
```

### 多用户支持

本服务支持多用户隔离，每个用户拥有独立的记忆文件：

**存储结构：**
```
[存储路径]/
├── user-123/
│   ├── MEMORY.md (2200 字符限制)
│   └── USER.md (1375 字符限制)
├── user-456/
│   ├── MEMORY.md
│   └── USER.md
└── default/  # 默认用户（未提供 userId 时使用）
    ├── MEMORY.md
    └── USER.md
```

**集群环境支持：**

本服务支持集群部署，通过环境模式配置保证数据一致性：

**开发模式（dev）：**
- 每个实例独立数据
- 适用于本地开发和测试
- 不需要共享文件系统

**生产模式（prod）：**
- 所有实例访问共享文件系统（NFS/GlusterFS/CephFS）
- 确保多实例数据一致性
- 需要配置共享存储

**部署要求：**
1. **共享文件系统**
   - 使用 NFS、GlusterFS、CephFS 等 POSIX 兼容的共享存储
   - 所有实例挂载到相同的路径

2. **路径配置**
   - 所有实例配置相同的 `MEMORY_STORAGE_PATH_PROD`
   - Spring Profile 设置为 `prod`

3. **数据一致性保证**
   - 每次操作都从文件读取（不使用本地缓存）
   - 文件锁 + atomic write 保证并发安全

4. **性能优化（可选）**
   ```bash
   # 挂载共享文件系统时添加 noatime（减少磁盘I/O）
   sudo mount -t nfs -o noatime server:/path /shared/data/memory
   ```

**部署示例（Docker + NFS + 多实例）：**
```yaml
version: '3.8'
services:
  # NFS 服务器
  nfs-server:
    driver: local
    driver_opts:
      type: nfs
      o: addr=nfs-server,rw
      device::/path/to/shared

  # mcp-memory-tool 实例1
  mcp-memory-tool-1:
    image: mcp-memory-tool:1.0.0
    ports:
      - "40304:40304"
    volumes:
      - nfs-server:/data/memory  # 共享存储
    environment:
      - ENVIRONMENT=prod
      - MEMORY_STORAGE_PATH_PROD=/data/memory
    restart: unless-stopped

  # mcp-memory-tool 实例2（外部端口不同，但内部访问相同路径）
  mcp-memory-tool-2:
    image: mcp-memory-tool:1.0.0
    ports:
      - "40305:40304"  # 外部端口40305映射到内部40304
    volumes:
      - nfs-server:/data/memory  # 同一共享存储
    environment:
      - ENVIRONMENT=prod
      - MEMORY_STORAGE_PATH_PROD=/data/memory
    restart: unless-stopped
```

**验证集群一致性：**
```bash
# 实例1写入
curl -X POST http://instance1:40304/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"memory_add","arguments":{"userId":"test-user","target":"memory","category":"test","content":"from instance1"}}}'

# 实例2读取（应该能获取到实例1写入的数据）
curl -X POST http://instance2:40304/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"memory_get","arguments":{"userId":"test-user","target":"memory"}}}'
```

### 环境变量

| 变量名 | 说明 | 开发模式默认值 | 生产模式默认值 |
|---------|------|---------------|---------------|
| ENVIRONMENT | 环境模式 | dev | prod |
| MCP_PORT | MCP 服务端口 | 40304 | 40304 |
| MCP_HOST | MCP 服务主机 | localhost | 实例IP |
| LOG_LEVEL | 日志级别 | INFO | INFO |
| MEMORY_STORAGE_PATH_DEV | 开发存储路径 | ./data/memory | - |
| MEMORY_STORAGE_PATH_PROD | 生产存储路径 | - | /shared/data/memory |

## Docker 部署

### Dockerfile

```dockerfile
FROM openjdk:17-slim

WORKDIR /app

COPY target/mcp-memory-tool-1.0.0.jar app.jar

EXPOSE  80304

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### docker-compose.yml

```yaml
version: '3.8'

services:
  mcp-memory-tool:
    build: .
    ports:
      - "40304:40304"
    environment:
      - MCP_PORT= 80304
      - LOG_LEVEL=INFO
      - MEMORY_STORAGE_PATH=./data/memory
    volumes:
      - ./data/memory:/app/data/memory  # 持久化存储
    restart: unless-stopped
```

### 运行 Docker

```bash
docker-compose up -d
```

## 监控和日志

### Prometheus Metrics

服务暴露以下指标：

- `self_improving_mcp_tool_call_total`
- `self_improving_mcp_tool_success_total`
- `self_improving_mcp_tool_failure_total`
- `self_improving_mcp_tool_execution_duration_seconds`

访问：`http://localhost: 80304/actuator/prometheus`

### 日志输出

```log
2024-01-15 10:30:00.123 INFO  o.y.m.M.Application - Starting MemoryToolApplication
2024-01-15 10:30:00.456 INFO  o.y.m.M.M.T.M.T - MCP tool call recorded: tool=memory_add, userId=user-001
2024-01-15 10:30:00.789 INFO  o.y.m.M.M.T.M.T - MCP tool success recorded: tool=memory_add
```

## 故障排查

### 常见问题

1. **端口占用**
   ```
   Error: Address already in use
   解决：检查  80304 端口是否被占用，修改配置或停止占用进程
   ```

2. **连接被拒绝**
   ```
   Error: Connection refused
   解决：检查 MCP 服务器是否启动，网络连接是否正常
   ```

3. **记忆保存失败**
   ```
   Error: Memory limit exceeded
   解决：检查 MEMORY.md 或 USER.md 是否超限，清理旧记忆
   ```

### 调试模式

启用调试模式：

```bash
java -jar target/mcp-memory-tool-1.0.0.jar --debug
```

或设置环境变量：

```bash
export LOG_LEVEL=DEBUG
java -jar target/mcp-memory-tool-1.0.0.jar
```

## 版本信息

- **当前版本**: 1.0.0
- **MCP 协议版本**: 1.0
- **兼容性**: yunxi-agent-platform v2.0.0+

## 相关资源

- [MCP 协议规范](https://modelcontextprotocol.com)
- [yunxi-agent-platform 文档](https://gitcode.com/chenyao813/yunxi-agent-platform)
- [API 参考文档](./API_REFERENCE.md)

## 许可证

[添加许可证信息]

## 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request
