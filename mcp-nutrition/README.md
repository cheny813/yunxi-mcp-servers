# mcp-nutrition MCP 营养数据工具

提供学校营养餐领域数据查询能力的 MCP 服务端，基于 [yunxi MCP Servers](https://github.com/yunxi/mcp-servers) 框架构建。

## 功能特性

- 菜品搜索（按关键词 / 按分类）
- 食谱营养评估（基于《中国居民膳食指南》多维度评分）
- 数据直接查询营养数据库，无需 REST 中间层

## 工具列表

| 工具名 | 说明 |
|--------|------|
| `searchDishes` | 按关键词搜索菜品，返回名称、分类、营养成分 |
| `evaluateRecipe` | 评估食谱营养评分，五项维度加权计算 |

## 使用示例

### 搜索菜品

```json
{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "searchDishes",
    "arguments": {
      "keyword": "番茄",
      "limit": 5
    }
  },
  "id": 1
}
```

### 评估食谱

```json
{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "evaluateRecipe",
    "arguments": {
      "crowdType": "小学生",
      "calorie": 1650,
      "protein": 55,
      "fat": 45,
      "carbs": 240,
      "dishCount": 4,
      "mealTypes": ["早餐", "午餐"],
      "schoolType": "普通城市"
    }
  },
  "id": 1
}
```

## 启动

```bash
# 构建
mvn clean package -DskipTests

# Windows
start.bat

# Linux/Mac
./start.sh
```

默认端口：40112

## 注册到平台

在 yunxi-agent-platform 的 agent-definitions YAML 中注册：

```yaml
tools:
  mcpServers:
    - name: nutrition-mcp
      type: http
      url: http://localhost:40112
```

## 依赖

- Spring Boot 3.5.x + Web
- MySQL 数据库（school_nutrition）
- mcp-common（MCP 协议基础设施）

## 数据源配置

在 `application.yml` 中配置数据库连接：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/school_nutrition?useUnicode=true&characterEncoding=utf-8
    username: root
    password: ${MYSQL_PASSWORD:root}
```