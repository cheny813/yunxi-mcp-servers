# MCP 安全认证配置指南

## 概述

mcp-common 提供统一的 MCP 服务认证机制，支持 API Token 和 Bearer Token (JWT) 两种认证方式。

## 快速开始

### 1. 添加依赖

确保你的 MCP 模块依赖 mcp-common：

```xml

<dependency>
    <groupId>io.yunxi</groupId>
    <artifactId>mcp-common</artifactId>
    <version>${project.version}</version>
</dependency>
```

### 2. 配置认证

#### API Token 模式（推荐）

```yaml
mcp:
  auth:
    enabled: true
    type: api-token
    token: ${MCP_API_TOKEN:your-secure-token}
    whitelist:
      - /mcp/health
      - /mcp/info
```

#### Bearer Token (JWT) 模式

```yaml
mcp:
  auth:
    enabled: true
    type: bearer
    jwt-secret: ${MCP_JWT_SECRET:your-jwt-secret-at-least-256-bits}
    whitelist:
      - /mcp/health
```

#### 禁用认证（开发环境）

```yaml
mcp:
  auth:
    enabled: false
```

## 配置说明

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `mcp.auth.enabled` | 是否启用认证 | `false` |
| `mcp.auth.type` | 认证类型：`api-token` / `bearer` / `none` | `api-token` |
| `mcp.auth.token` | API Token（api-token 模式使用） | - |
| `mcp.auth.jwt-secret` | JWT Secret（bearer 模式使用） | - |
| `mcp.auth.whitelist` | 白名单路径列表 | `/mcp/health`, `/mcp/info` |
| `mcp.auth.header-name` | API Token 请求头名称 | `X-MCP-Token` |
| `mcp.auth.bearer-prefix` | Bearer Token 前缀 | `Bearer ` |

## 使用示例

### API Token 认证

**请求示例：**

```http
POST /mcp
X-MCP-Token: your-secure-token
Content-Type: application/json

{
  "jsonrpc": "2.0",
  "method": "tools/list",
  "id": 1
}
```

**或使用 Authorization 头：**

```http
POST /mcp
Authorization: Bearer your-secure-token
Content-Type: application/json

{
  "jsonrpc": "2.0",
  "method": "tools/list",
  "id": 1
}
```

### Bearer Token (JWT) 认证

**请求示例：**

```http
POST /mcp
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
Content-Type: application/json

{
  "jsonrpc": "2.0",
  "method": "tools/list",
  "id": 1
}
```

## 错误响应

### 缺少认证令牌

```json
{
  "error": "Unauthorized",
  "message": "Missing authentication token",
  "status": 401
}
```

### 无效的认证令牌

```json
{
  "error": "Unauthorized",
  "message": "Invalid authentication token",
  "status": 401
}
```

## 安全建议

1. **使用环境变量**：不要在配置文件中硬编码 Token
   ```yaml
   token: ${MCP_API_TOKEN}
   ```

2. **使用强 Token**：API Token 建议至少 32 位随机字符串
   ```bash
   openssl rand -base64 32
   ```

3. **JWT Secret 长度**：Bearer 模式的 JWT Secret 至少 256 位

4. **生产环境启用**：生产环境务必启用认证

5. **最小白名单**：白名单路径应最小化，仅保留必要的健康检查端点

## 集成到现有 MCP 模块

如果你的 MCP 模块已经继承了 `AbstractMcpController`，认证会自动生效：

1. 添加 mcp-common 依赖
2. 在 application.yml 中配置认证
3. 重启服务

无需修改代码，认证过滤器会自动拦截 `/mcp/*` 路径。
