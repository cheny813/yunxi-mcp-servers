# mcp-nutrition · 营养配餐 MCP 服务

学校营养餐领域的 MCP 工具服务，基于 [yunxi MCP Servers](https://github.com/yunxi/mcp-servers) 框架构建。核心能力是 MySQL 结构化数据经过 BGE-M3 向量化后写入 Milvus 向量库，再通过 MCP 协议向外提供语义搜索和营养评估工具。

## 目录

1. [功能特性](#功能特性)
2. [架构概览](#架构概览)
3. [工具列表](#工具列表)
4. [数据同步](#数据同步)
5. [配置参考](#配置参考)
6. [启动运行](#启动运行)
7. [注册到平台](#注册到平台)
8. [依赖](#依赖)

---

## 功能特性

- 菜品语义搜索（按关键词 / 按分类）
- 食谱营养评估（基于《中国居民膳食指南》多维度评分）
- MySQL → Milvus 声明式 ETL 同步（6 条管道覆盖全领域数据）
- ID 级差异对比同步：重启仅处理增删差异，存量不动
- 嵌入向量化走本机 Ollama BGE-M3（1024 维），无外部 API 依赖

## 架构概览

```
MySQL (yunxi_nutrition)
  │
  ├── dish_class, food_ingredient_class, nutrient_standard, ...
  │
  ▼  SyncEngine
  │
  ├── 1. JDBC 查询 → 2. Transform (concat 模板) → 3. Ollama BGE-M3 向量化 → 4. Milvus insert
  │
  ▼  Milvus (6 个向量集合)
  │
  └── MCP Tools (search_public_dishes / evaluate_recipe / sync_data)
```

数据同步是启动时自动完成的一次性初始化，之后服务随时接受 MCP 工具调用。

## 工具列表

| 工具名 | 说明 |
|--------|------|
| `search_public_dishes` | 按关键词语义搜索公共菜品库，返回名称、分类、营养成分 |
| `evaluate_recipe` | 评估食谱营养评分，五项维度加权计算 |
| `sync_data` | 手动触发数据同步（type 可选：管道名 / dishes / all） |

## 数据同步

### 同步引擎

SyncEngine 管理 6 条声明式 ETL 管道（见 `sync-pipelines.yml`），将 MySQL 中的营养领域表数据批量向量化后写入 Milvus。

支持两种同步模式：

| 模式 | 触发场景 | 行为 |
|------|---------|------|
| **全量同步** | 首次启动（Milvus 空） | 全部向量化 + insert |
| **增量同步** | 定时 cron（`0 0 2 * * ?`） | 仅 upsert `update_time > 上次游标` 的行 |
| **Diff 同步** | 重启（Milvus 已有数据） | 对比 MySQL ↔ Milvus 主键 ID，仅处理增删差异 |

### Diff 同步机制（生产安全关键）

重启时不会清空 Milvus 全量重写，而是执行 ID 级差异对比：

1. 查询 Milvus 中已存在的所有主键 ID（分页 `queryAllIds`）
2. 与 MySQL 当前数据 ID 集合做 diff
3. MySQL 有 / Milvus 无 → 向量化后 insert（仅新增）
4. Milvus 有 / MySQL 无 → delete 过期记录
5. 两遍都有 → 跳过（存量不变，零向量化开销）

**收益**：数据无变化时重启秒级完成（跳过全部 6 条管道）；仅新增少量记录时只向量化差异部分，不会因全量重算导致长时间服务不可用。Milvus 查询失败时自动降级为全量清空+重写。

### 同步管道清单

| 管道名 | Milvus 集合 | 数据内容 |
|--------|------------|---------|
| `dish-classes-sync` | `dish_classes` | 菜品分类体系（含父子层级） |
| `ingredient-classes-sync` | `ingredient_classes` | 食材分类体系（主分类 + 子分类） |
| `nutrition-standards-sync` | `nutrition_standards` | 膳食营养素参考摄入量标准 |
| `nutrient-standard-details-sync` | `nutrient_standard_details` | 标准明细：每种营养素推荐量 |
| `ingredient-nutrients-sync` | `ingredient_nutrients` | 食材营养成分（蛋白质/脂肪/碳水等） |
| `score-index-sync` | `cook_book_score_index` | 评分指标维度及权重 |

所有管道均为声明式配置：source.sql / transform (concat 模板) / target (Milvus schema + 索引)，无需编写 Java 代码即可扩展新数据源。

## 配置参考

### 核心配置（application.yml）

```yaml
server:
  port: 40602

spring:
  datasource:
    # 营养数据库连接（sync_cursor 游标表 + 所有管道的 default 数据源）
    url: jdbc:mysql://host:3306/yunxi_nutrition?useUnicode=true&characterEncoding=utf-8
    username: root
    password: ${MYSQL_PASSWORD:root}

# 嵌入向量化（本机 Ollama BGE-M3）
embedding:
  provider: ollama
  ollama:
    base-url: http://localhost:11434
    model: bge-m3
    dimension: 1024

# Milvus 向量数据库
milvus:
  enabled: true
  host: ${MILVUS_HOST:localhost}
  port: ${MILVUS_PORT:19530}

# 数据同步
sync:
  enabled: true
  auto-sync-on-startup: true          # 启动时自动执行全量同步
  dish:
    cron: ${DISH_SYNC_CRON:0 0 2 * * ?}
```

### 管道配置（sync-pipelines.yml）

每条管道配置结构：

```yaml
sync:
  pipelines:
    - name: example-sync
      description: "管道说明"
      source:
        datasource: default
        sql: "SELECT id, name, ... FROM table WHERE deleted = 0"
        incremental:              # 可选：增量同步
          column: update_time
          initialValue: "2024-01-01 00:00:00"
      transform:
        - type: concat            # 文本拼接模板
          field: embedding_text
          template: "分类 {name} 说明 {description}"
      target:
        collection: example_collection
        primaryKey: id            # diff 同步的主键字段
        fields:
          - { name: id, type: Int64, primaryKey: true }
          - { name: name, type: VarChar }
          - { name: embedding_text, type: VarChar }
          - { name: embedding, type: FloatVector, dimension: 1024 }
        index:
          field: embedding
          metricType: COSINE
```

## 启动运行

```bash
# 1. 确保前置服务运行
#    - MySQL（含 yunxi_nutrition 数据库）
#    - Milvus（默认 localhost:19530）
#    - Ollama（默认 localhost:11434，需提前拉取 bge-m3 模型）

# 2. 构建
cd mcp-nutrition
mvn clean package -DskipTests

# 3. 启动（启动时会自动执行首次全量同步）
java -jar target/mcp-nutrition-*.jar

# 或通过环境变量覆盖配置
java -jar target/mcp-nutrition-*.jar \
  --server.port=40602 \
  --milvus.host=192.168.10.100 \
  --embedding.ollama.base-url=http://192.168.10.100:11434
```

默认端口：**40602**，SSE 端点：`GET /mcp/sse`，消息端点：`POST /mcp/message`。

## 注册到平台

在 yunxi-agent-platform 的 agent-definitions YAML 中注册：

```yaml
tools:
  mcpServers:
    - name: nutrition-mcp
      type: sse
      url: http://localhost:40602/mcp/sse
```

## 依赖

- Spring Boot 3.5.x + Web
- MySQL 数据库（yunxi_nutrition 库，含 sync_cursor 游标表自动建表）
- Milvus 向量数据库（6 个集合，BGE-M3 1024 维向量）
- Ollama（本机嵌入服务，bge-m3 模型）
- mcp-common（MCP 协议基础设施 + ToolHandler 注册机制）