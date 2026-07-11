# yunxi MCP Servers

MCP (Model Context Protocol) 服务器实现集合，为 AI Agent 提供工具调用能力。

---

## 目录

1. [项目简介](#一项目简介)
2. [端口快速参考](#二端口快速参考)
3. [模块说明](#三模块说明)
4. [在 Agent 框架中配置](#四在-agent-框架中配置)
5. [工具描述最佳实践](#五工具描述最佳实践)
6. [开发自定义 MCP Server](#六开发自定义-mcp-server)
7. [参考链接](#七参考链接)

---

## 一、项目简介

MCP 服务器按部署模式分为两类：

- **HTTP/SSE 模式**（基于 Spring + 官方 MCP SDK 的 SSE 传输）：通过 `GET /mcp/sse` 建立 SSE 连接，`POST /mcp/message` 发送 JSON-RPC 消息。工具由 `McpServerFactory` 自动发现并注册。
- **Stdio 模式**（无 Spring 依赖，基于 `AbstractStdioMcpServer`）：通过标准输入输出与宿主进程通信，适用于作为本地子进程被 Agent 框架拉起（如 `mcp-database-stdio`、`mcp-api-gateway`）。

> v2.0 统一使用官方 MCP Java SDK 0.17.0 的 SSE 传输，**不再提供独立的 `POST /mcp` 请求/响应端点**。Stdio 模式仅通过 stdin/stdout 通信。

### 项目结构

```
yunxi-mcp-servers/
├── mcp-common/              # 公共模块 - MCP 协议实现
├── mcp-database-stdio/      # 数据库 MCP 服务器（stdio 模式）
├── mcp-database/            # 数据库 MCP 服务器（HTTP/SSE 模式）
├── mcp-redis/              # Redis MCP 服务器
├── mcp-milvus/             # Milvus 向量数据库 MCP 服务器
├── mcp-qdrant/             # Qdrant 向量数据库 MCP 服务器
├── mcp-logging/            # 日志查询 MCP 服务器
├── mcp-monitoring/         # 系统监控 MCP 服务器
├── mcp-knowledge/          # 知识库 MCP 服务器
├── mcp-code-search/        # 代码搜索 MCP 服务器
├── mcp-code-edit/          # 代码编辑 MCP 服务器
├── mcp-filesystem/          # 文件系统 MCP 服务器
├── mcp-git/                # Git MCP 服务器
├── mcp-github/              # GitHub MCP 服务器
├── mcp-s3/                 # AWS S3 MCP 服务器
├── mcp-mongodb/            # MongoDB MCP 服务器
├── mcp-elasticsearch/      # Elasticsearch MCP 服务器
├── mcp-docker/             # Docker MCP 服务器
├── mcp-k8s/                # Kubernetes MCP 服务器
├── mcp-email/              # 邮件发送 MCP 服务器
├── mcp-wikipedia/          # Wikipedia MCP 服务器
├── mcp-baidu-ocr/          # 百度 OCR MCP 服务器
├── mcp-baidu-asr/          # 百度语音识别 MCP 服务器
├── mcp-baidu-search/       # 百度搜索 MCP 服务器
├── mcp-dingtalk/           # 钉钉集成 MCP 服务器
├── mcp-wechat/             # 企微集成 MCP 服务器
├── mcp-api-gateway/        # API 网关 MCP 服务器
├── mcp-playwright/         # 浏览器自动化 MCP 服务器
├── mcp-chart/              # 图表生成 MCP 服务器
├── mcp-pagegen/            # 页面生成 MCP 服务器
├── mcp-memory-tool/        # 记忆管理 MCP 服务器
├── mcp-pdf/                # PDF 处理 MCP 服务器
├── mcp-xlsx/               # Excel 处理 MCP 服务器
├── mcp-pptx/               # PPT 处理 MCP 服务器
├── mcp-mqtt/               # MQTT 消息队列 MCP 服务器
├── mcp-formfill/           # 表单填写 MCP 服务器
├── mcp-nutrition/          # 营养配餐 MCP 服务器
├── mcp-skill-adapter/      # Python Skill 适配器（Python）
└── README.md
```

---

## 二、端口快速参考

### 2.1 核心平台

| 服务 | 端口 | 访问地址 | 项目 |
|------|------|---------|------|
| **agent-core** (含 A2A Server) | 40001 | http://localhost:40001 | yunxi-agent-platform |
| **agent-rule-engine** | 40002 | http://localhost:40002 | yunxi-agent-platform |
| **agent-app** (聚合启动) | 40001 | http://localhost:40001 | yunxi-agent-platform |

### 2.2 数据库与存储

| 服务 | 端口 | 说明 |
|------|------|------|
| **mcp-database** | 40101 | MySQL MCP 服务 |
| **mcp-redis** | 40102 | Redis MCP 服务 |
| **mcp-milvus** | 40103 | Milvus 向量数据库 |
| **mcp-qdrant** | 40104 | Qdrant 向量数据库 |
| **mcp-logging** | 40105 | 日志查询 |
| **mcp-monitoring** | 40106 | 系统监控 |
| **mcp-knowledge** | 40107 | 知识库 |
| **mcp-code-edit** | 40108 | 代码编辑 |
| **mcp-code-search** | 40106 | 代码搜索 |

### 2.3 外部服务

| 服务 | 端口 | 说明 |
|------|------|------|
| **mcp-github** | 40201 | GitHub 集成 |
| **mcp-baidu-ocr** | 40202 | 百度 OCR |
| **mcp-baidu-asr** | 40203 | 百度语音识别 |
| **mcp-baidu-search** | 40204 | 百度搜索 |
| **mcp-dingtalk** | 40205 | 钉钉集成 |
| **mcp-wechat** | 40206 | 企微集成 |

### 2.4 AI/ML 服务

| 服务 | 端口 | 说明 |
|------|------|------|
| **mcp-playwright** | 40301 | 浏览器自动化 |
| **mcp-chart** | 40302 | 图表生成 |
| **mcp-pagegen** | 40303 | 页面生成 |
| **mcp-memory-tool** | 40304 | 记忆管理 |

### 2.5 文档处理

| 服务 | 端口 | 说明 |
|------|------|------|
| **mcp-pdf** | 40401 | PDF 处理 |
| **mcp-xlsx** | 40402 | Excel 处理 |
| **mcp-pptx** | 40403 | PPT 处理 |

### 2.6 基础设施

| 服务 | 端口 | 说明 |
|------|------|------|
| **mcp-filesystem** | 40501 | 文件系统 |
| **mcp-mqtt** | 40502 | MQTT 消息队列 |

### 2.7 其他服务

| 服务 | 端口 | 说明 |
|------|------|------|
| **mcp-formfill** | 40601 | 表单填写 |
| **mcp-nutrition** | 40602 | 营养配餐 |
| **mcp-git** | 8085 | Git 仓库操作 |
| **mcp-s3** | 8086 | AWS S3 对象存储 |
| **mcp-mongodb** | 8087 | MongoDB 数据库 |
| **mcp-elasticsearch** | 8088 | Elasticsearch 搜索 |
| **mcp-docker** | 8089 | Docker 容器管理 |
| **mcp-k8s** | 8090 | Kubernetes 集群管理 |
| **mcp-email** | 8091 | 邮件发送 |
| **mcp-wikipedia** | 8092 | Wikipedia 搜索 |

### 2.8 Agent 扩展

| 服务 | 端口 | 说明 |
|------|------|------|
| **mcp-skill-adapter** | 40801 | Python Skill 适配器 |

### 2.10 端口规则

```text
4xxxx: 统一前缀（避免常见端口冲突）

400xx: 核心平台
401xx: 数据库与存储
402xx: 外部服务
403xx: AI/ML
404xx: 文档处理
405xx: 基础设施
406xx: 其他服务
407xx: 网关适配器（预留）
408xx: Agent 扩展
8xxx: 工具类服务（默认端口）
```

### 2.11 快速检查

```bash
# Windows
netstat -ano | findstr "40"

# Linux/Mac
netstat -tuln | grep 40
```

---

## 三、模块说明

### mcp-common

MCP 协议的公共实现（**v2.0，基于官方 MCP Java SDK 0.17.0**），包含：

- `ToolHandler` - 工具处理器接口（`getName` / `getDescription` / `getInputSchema` / `execute`）
- `McpServerFactory` - HTTP/SSE 模式服务器工厂（Spring 自动发现所有 `ToolHandler` Bean 并注册）
- `AbstractStdioMcpServer` - Stdio 模式服务器抽象基类（`registerTool` + `start`）
- `ToolResult` - 兼容 shim（`text` / `error` 静态方法返回 `String`，仅用于向后兼容）
- `io.modelcontextprotocol.sdk:mcp` / `mcp-core` / `mcp-json-jackson2` - 官方 MCP SDK 依赖

> 注意：v1.0 的自定义类 `ToolDefinition`、`ToolResult`（v1 模型）、`McpRequest`、`McpResponse`、`AbstractMcpServer` 已在 v2.0 中移除，改由官方 SDK 的 `McpSchema.Tool`、`JsonSchema`、`CallToolResult` 承载。

### mcp-database-stdio

数据库操作 MCP 服务器（stdio 模式），支持：

- `query` - 执行 SQL 查询
- `list_tables` - 列出数据库表
- `describe_table` - 获取表结构

**启动命令**：
```bash
java -jar mcp-database-stdio.jar \
  --db.url=jdbc:mysql://localhost:3306/test \
  --db.username=root \
  --db.password=root
```

### mcp-database

数据库操作 MCP 服务器（HTTP 模式），支持：

- `query` - 执行 SQL 查询
- `list_tables` - 列出数据库表
- `describe_table` - 获取表结构

**启动命令**：
```bash
java -jar mcp-database.jar --server.port=40101 \
  --spring.datasource.url=jdbc:mysql://localhost:3306/test \
  --spring.datasource.username=root \
  --spring.datasource.password=root
```

**端点**：
- HTTP: `POST http://localhost:40101/mcp`
- SSE: `GET http://localhost:40101/mcp/sse` (消息端点: `/mcp/message`)

### mcp-filesystem

文件系统操作 MCP 服务器，支持：

- `read_file` - 读取文件内容
- `write_file` - 写入文件
- `list_directory` - 列出目录内容
- `search_files` - 搜索文件

**启动命令**：
```bash
java -jar mcp-filesystem.jar --server.port=40501 \
  --mcp.allowed-directory=./workspace
```

**端点**：
- HTTP: `POST http://localhost:40501/mcp`
- SSE: `GET http://localhost:40501/mcp/sse` (消息端点: `/mcp/message`)

### mcp-redis

Redis 操作 MCP 服务器，支持：

- `get` / `set` - 字符串操作
- `delete` - 删除键
- `keys` - 查找键
- `list_ops` - 列表操作（lpush, rpush, lrange, lpop, rpop）
- `hash_ops` - 哈希操作（hset, hget, hgetall, hdel）

**启动命令**：
```bash
java -jar mcp-redis.jar --server.port=40102 \
  --spring.data.redis.host=localhost \
  --spring.data.redis.port=6379
```

**端点**：
- HTTP: `POST http://localhost:40102/mcp`
- SSE: `GET http://localhost:40102/mcp/sse` (消息端点: `/mcp/message`)

### mcp-github

GitHub 操作 MCP 服务器，支持：

- `list_repositories` - 列出用户仓库
- `get_repository` - 获取仓库信息
- `list_issues` - 列出 Issue
- `create_issue` - 创建 Issue
- `list_pull_requests` - 列出 Pull Request

**启动命令**：
```bash
java -jar mcp-github.jar --server.port=40201 \
  --github.token=your_github_token
```

**端点**：
- HTTP: `POST http://localhost:40201/mcp`
- SSE: `GET http://localhost:40201/mcp/sse` (消息端点: `/mcp/message`)

### mcp-git

Git 仓库操作 MCP 服务器，支持：

- `git_status` - 获取 Git 仓库状态
- `git_log` - 获取 Git 提交日志
- `git_branches` - 获取分支列表

**启动命令**：
```bash
java -jar mcp-git.jar --server.port=8085 \
  --git.repo-path=/path/to/git/repo
```

**端点**：
- HTTP: `POST http://localhost:8085/mcp`
- SSE: `GET http://localhost:8085/mcp/sse` (消息端点: `/mcp/message`)

### mcp-s3

AWS S3 操作 MCP 服务器，支持：

- `s3_list_buckets` - 列出所有存储桶
- `s3_list_objects` - 列出存储桶中的对象
- `s3_put_object` - 上传对象到存储桶
- `s3_get_object` - 下载存储桶中的对象
- `s3_delete_object` - 删除存储桶中的对象

**启动命令**：
```bash
java -jar mcp-s3.jar --server.port=8086 \
  --aws.region=us-east-1
# 或使用 MinIO 等 S3 兼容存储
java -jar mcp-s3.jar --server.port=8086 \
  --aws.endpoint=http://localhost:9000 \
  --aws.region=us-east-1
```

**端点**：
- HTTP: `POST http://localhost:8086/mcp`
- SSE: `GET http://localhost:8086/mcp/sse` (消息端点: `/mcp/message`)

### mcp-mongodb

MongoDB 操作 MCP 服务器，支持：

- `mongodb_list_databases` - 列出所有数据库
- `mongodb_list_collections` - 列出数据库中的集合
- `mongodb_find` - 查询文档
- `mongodb_insert` - 插入文档
- `mongodb_delete` - 删除文档

**启动命令**：
```bash
java -jar mcp-mongodb.jar --server.port=8087 \
  --mongodb.connection-string=mongodb://localhost:27017
```

**端点**：
- HTTP: `POST http://localhost:8087/mcp`
- SSE: `GET http://localhost:8087/mcp/sse` (消息端点: `/mcp/message`)

### mcp-elasticsearch

Elasticsearch 操作 MCP 服务器，支持：

- `es_search` - 搜索文档
- `es_index_document` - 索引文档
- `es_list_indices` - 列出索引
- `es_delete_document` - 删除文档

**启动命令**：
```bash
java -jar mcp-elasticsearch.jar --server.port=8088 \
  --elasticsearch.host=localhost \
  --elasticsearch.port=9200
```

**端点**：
- HTTP: `POST http://localhost:8088/mcp`
- SSE: `GET http://localhost:8088/mcp/sse` (消息端点: `/mcp/message`)

### mcp-docker

Docker 操作 MCP 服务器，支持：

- `docker_list_containers` - 列出容器
- `docker_list_images` - 列出镜像
- `docker_pull_image` - 拉取镜像
- `docker_start_container` - 启动容器
- `docker_stop_container` - 停止容器

**启动命令**：
```bash
java -jar mcp-docker.jar --server.port=8089
```

**端点**：
- HTTP: `POST http://localhost:8089/mcp`
- SSE: `GET http://localhost:8089/mcp/sse` (消息端点: `/mcp/message`)

### mcp-k8s

Kubernetes 操作 MCP 服务器，支持：

- `k8s_list_pods` - 列出 Pod
- `k8s_list_services` - 列出 Service
- `k8s_list_namespaces` - 列出 Namespace

**启动命令**：
```bash
java -jar mcp-k8s.jar --server.port=8090
```

**端点**：
- HTTP: `POST http://localhost:8090/mcp`
- SSE: `GET http://localhost:8090/mcp/sse` (消息端点: `/mcp/message`)

### mcp-email

邮件发送 MCP 服务器，支持：

- `send_email` - 发送邮件

**启动命令**：
```bash
java -jar mcp-email.jar --server.port=8091 \
  --spring.mail.host=smtp.example.com \
  --spring.mail.port=587 \
  --spring.mail.username=your-email@example.com \
  --spring.mail.password=your-password
```

**端点**：
- HTTP: `POST http://localhost:8091/mcp`
- SSE: `GET http://localhost:8091/mcp/sse` (消息端点: `/mcp/message`)

### mcp-wikipedia

Wikipedia 操作 MCP 服务器，支持：

- `wikipedia_search` - 搜索 Wikipedia 文章
- `wikipedia_summary` - 获取 Wikipedia 文章摘要

**启动命令**：
```bash
java -jar mcp-wikipedia.jar --server.port=8092
```

**端点**：
- HTTP: `POST http://localhost:8092/mcp`
- SSE: `GET http://localhost:8092/mcp/sse` (消息端点: `/mcp/message`)

### mcp-api-gateway

HTTP API 调用 MCP 服务器（仅支持 Stdio 模式），支持：

- `http_get` - HTTP GET 请求
- `http_post` - HTTP POST 请求

**注意**：此模块仅支持 Stdio 模式，不提供 HTTP/SSE 远程调用能力。

**启动命令**：
```bash
java -jar mcp-api-gateway.jar
```

---

## 四、在 Agent 框架中配置

### 4.1 stdio 模式配置

在 `application-agentscope.yml` 中配置：

```yaml
agentscope:
  mcp-servers:
    mysql:
      enabled: true
      type: stdio
      command: java
      args:
        - "-jar"
        - "path/to/mcp-database-stdio.jar"
        - "--db.url=jdbc:mysql://localhost:3306/test"
        - "--db.username=root"
        - "--db.password=root"

    api:
      enabled: true
      type: stdio
      command: java
      args:
        - "-jar"
        - "path/to/mcp-api-gateway.jar"
```

### 4.2 SSE 远程调用配置

```yaml
agentscope:
  mcp-servers:
    filesystem:
      enabled: true
      type: sse
      url: http://localhost:40501/mcp/sse

    redis:
      enabled: true
      type: sse
      url: http://localhost:40102/mcp/sse

    github:
      enabled: true
      type: sse
      url: http://localhost:40201/mcp/sse

    git:
      enabled: true
      type: sse
      url: http://localhost:8085/mcp/sse

    s3:
      enabled: true
      type: sse
      url: http://localhost:8086/mcp/sse

    mongodb:
      enabled: true
      type: sse
      url: http://localhost:8087/mcp/sse

    elasticsearch:
      enabled: true
      type: sse
      url: http://localhost:8088/mcp/sse

    docker:
      enabled: true
      type: sse
      url: http://localhost:8089/mcp/sse

    k8s:
      enabled: true
      type: sse
      url: http://localhost:8090/mcp/sse

    email:
      enabled: true
      type: sse
      url: http://localhost:8091/mcp/sse

    wikipedia:
      enabled: true
      type: sse
      url: http://localhost:8092/mcp/sse
```

---

## 五、工具描述最佳实践

### 5.1 核心原则

**MCP 服务器端写好工具描述是最佳实践，配置文件只需说明使用哪些服务器即可！**

这样做的好处：
- 单一定义，避免重复
- 工具更新后自动生效
- 维护成本降低
- 大模型理解更准确
- 配置文件更简洁

### 5.2 描述结构

```java
.description(
    "[简短功能描述]. " +                       // 1. 一句话说明功能
    "Use this when [使用场景1], [使用场景2], or [使用场景3]. " +  // 2. 什么时候使用（英文）
    "适用于：[场景1]、[场景2]、[场景3]等场景。" +  // 3. 什么时候使用（中文）
)
```

### 5.3 参数描述规范

```java
"key", Map.of(
    "type", "string",
    "description", "[参数用途]. Example: '[示例值]'"  // 必须包含示例
)
```

### 5.4 枚举值约束

```java
"operation", Map.of(
    "type", "string",
    "description", "Operation type. Options: " +
        "'op1' - description, " +
        "'op2' - description",
    "enum", List.of("op1", "op2")  // 添加枚举约束
)
```

### 5.5 双语描述示例

```java
.description(
    "Set key-value pair in Redis with optional TTL. " +         // 英文描述
    "设置 Redis 键值对，支持设置过期时间。 " +                  // 中文描述
    "Use this when you need to store data for caching or session management. " +  // 英文场景
    "适用于：数据缓存、会话管理、临时数据存储等场景。"           // 中文场景
)
```

### 5.6 检查清单

#### 基础检查
- [ ] 工具名称简洁（动词+名词格式，不超过 20 字符）
- [ ] 包含简短功能描述（第一句话）
- [ ] 说明使用场景（"Use this when..."）
- [ ] 描述长度：2-3 句话
- [ ] 中文版本使用"适用于："格式

#### 参数定义检查
- [ ] 每个参数都有 `type` 定义
- [ ] 每个参数都有 `description`
- [ ] description 包含参数用途说明
- [ ] description 包含至少 1 个示例值
- [ ] 必填参数在 `required` 列表中

#### 枚举类型参数
- [ ] 添加 `enum` 字段列出所有允许值
- [ ] description 说明每个枚举值的含义

### 5.7 对比总结

| 维度 | 优化前 | 优化后 |
|------|--------|--------|
| **描述长度** | 2-4 个单词 | 2-3 句完整描述 |
| **使用场景** | 无 | 明确说明 |
| **参数说明** | 只有类型 | 包含描述和示例 |
| **枚举约束** | 无 | 添加枚举值 |
| **大模型理解** | 模糊 | 清晰明确 |

### 5.8 已优化的 MCP 模块

以下模块已完成工具描述优化：

| 模块 | 工具 | 状态 |
|------|------|------|
| mcp-redis | set, get, keys, delete, list_ops, hash_ops | 已完成 |
| mcp-wikipedia | search | 已完成 |
| mcp-mongodb | query, insert, update, delete | 已完成 |
| mcp-git | commit, branch, status, log | 已完成 |
| mcp-github | repo, issue, pr | 已完成 |
| mcp-email | send | 已完成 |
| mcp-elasticsearch | search, index, delete | 已完成 |
| mcp-docker | ps, run, stop, logs | 已完成 |
| mcp-filesystem | read, write, list, search | 已完成 |
| mcp-database | query, execute, list_tables | 已完成 |
| mcp-baidu-search | search | 已完成 |
| mcp-chart | generate | 已完成 |
| mcp-pdf | pdf_operation | 已完成 |
| mcp-xlsx | xlsx_operation | 已完成 |
| mcp-pptx | pptx_operation | 已完成 |
| mcp-memory-tool | memory_query, memory_save | 已完成 |
| mcp-milvus | vector_search, vector_insert | 已完成 |
| mcp-logging | log_query | 已完成 |
| mcp-knowledge | knowledge_query | 已完成 |
| mcp-code-search | code_search | 已完成 |

### 5.9 快速参考模板

```java
@Override
public String getName() {
    return "tool_name";  // 动词+名词
}

@Override
public String getDescription() {
    return "[功能描述]. " +
           "Use this when you need to: [场景1], [场景2], [场景3]. " +
           "适用于：[场景1]、[场景2]、[场景3]等场景。";
}

@Override
public Map<String, Object> getInputSchema() {
    return Map.of(
        "type", "object",
        "properties", Map.of(
            "param1", Map.of(
                "type", "string",
                "description", "参数用途. Example: '示例值'"
            )
        ),
        "required", List.of("param1")
    );
}

@Override
public String execute(Map<String, Object> arguments) {
    String param1 = (String) arguments.get("param1");
    // 执行失败直接抛出 RuntimeException，框架自动包装为 isError=true
    if (param1 == null) {
        throw new RuntimeException("param1 is required");
    }
    return "Result: " + param1;
}
```

### 5.10 验证方法

#### 查看工具列表

v2.0 使用官方 MCP SDK 的 SSE 传输，工具列表通过 MCP `tools/list` 协议方法获取（无独立的 HTTP `/tools` GET 端点）。可通过支持 MCP 的客户端（如 Agent 框架、MCP Inspector）连接 `http://localhost:PORT/mcp/sse` 后调用 `tools/list`，或在日志中观察 `McpServerFactory` 启动时打印的 `注册 MCP 工具: <name>`。

#### 检查返回格式

`tools/list` 返回的工具结构（对应官方 SDK `McpSchema.Tool` / `JsonSchema`）：
```json
{
  "name": "set",
  "description": "Set key-value pair in Redis...",
  "inputSchema": {
    "type": "object",
    "properties": {
      "key": {
        "type": "string",
        "description": "The Redis key to set. Example: 'user:001'"
      }
    },
    "required": ["key"]
  }
}
```

### 5.11 参考代码

- mcp-redis: `src/main/java/io/yunxi/mcp/redis/tools/`
- mcp-baidu-search: `src/main/java/io/yunxi/mcp/baidusearch/tool/`
- mcp-pdf: `src/main/java/io/yunxi/mcp/pdf/PdfTools.java`
- mcp-xlsx: `src/main/java/io/yunxi/mcp/xlsx/XlsxTools.java`
- mcp-pptx: `src/main/java/io/yunxi/mcp/pptx/PptxTools.java`

---

## 六、开发自定义 MCP Server

v2.0 基于官方 MCP Java SDK 0.17.0，工具统一实现 `ToolHandler` 接口。根据部署模式分为两类。

### 6.1 创建工具处理器（两种模式通用）

实现 `io.yunxi.mcp.common.handler.ToolHandler` 接口，四个方法：

```java
public class MyTool implements ToolHandler {

    @Override
    public String getName() {
        return "my_tool";
    }

    @Override
    public String getDescription() {
        return "My custom tool";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "input", Map.of("type", "string")
                ),
                "required", List.of("input")
        );
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        String input = (String) arguments.get("input");
        return "Result: " + input;   // 执行失败可抛出 RuntimeException
    }
}
```

### 6.2 模式一：HTTP/SSE 模式（Spring 自动装配）

HTTP/SSE 模块依赖 Spring。`ToolHandler` 标注 `@Component` 后，`McpServerFactory` 会自动发现并注册所有工具，传输层使用官方 SDK 的 SSE（`/mcp/sse` + `/mcp/message`）。

```java
@Component                       // 标注为 Spring Bean
public class MyTool implements ToolHandler { /* ... 同上 ... */ }
```

```yaml
# application.yml
mcp:
  server:
    name: yunxi-mcp-my
    version: 1.0.0
server:
  port: 40199
```

启动后端点：
- SSE: `GET http://localhost:40199/mcp/sse`（消息端点 `/mcp/message`）

### 6.3 模式二：Stdio 模式（无 Spring 依赖）

Stdio 模块继承 `AbstractStdioMcpServer`，在构造器中 `registerTool` 并调用 `start()`，由 JVM 通过标准输入输出与宿主通信。

```java
public class MyMcpServer extends AbstractStdioMcpServer {

    public MyMcpServer(String name, String version) {
        super(name, version);
        registerTool(new MyTool());
    }

    public static void main(String[] args) {
        new MyMcpServer("my-mcp-server", "1.0.0").start();
    }
}
```

### 6.4 打包运行

```bash
mvn clean package
java -jar my-mcp-server.jar
```

---

## 七、参考链接

- [MCP 官方规范](https://spec.modelcontextprotocol.io/)
- [MCP GitHub](https://github.com/modelcontextprotocol)
- [JSON-RPC 2.0 规范](https://www.jsonrpc.org/specification)

---

## 许可证

MIT License
