# MCP MQTT Server

MQTT 实时订阅/发布 MCP 服务器 - 集成 EMQ X，提供实时传感器数据订阅和设备控制。

## 功能定位

### ✅ mcp-mqtt 负责
- **实时 MQTT 订阅/发布**
- **实时传感器数据接收**（内存缓存）
- **设备控制**（发布消息）

### ❌ mcp-mqtt 不负责
- ~~历史数据存储~~ → 传统程序处理，存入 MySQL
- ~~电子秤 HTTPS 接口~~ → 传统程序处理，存入 MySQL
- ~~历史数据查询~~ → 使用 **mcp-database** 查询 MySQL

## 架构说明

```
┌─────────────────────────────────────────────────────────────┐
│                    数据接入层（传统程序）                      │
├─────────────────────────────────────────────────────────────┤
│  温湿度传感器 ─── MQTT ──→ EMQ X ──→ 传统程序 ──→ MySQL     │
│  电子秤 ──────── HTTPS ─────────→ 传统程序 ──→ MySQL        │
│  页面填写 ─────────────────────→ 传统程序 ──→ MySQL        │
└─────────────────────────────────────────────────────────────┘
                              ↓
                         MySQL 数据库
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                    MCP 工具层（AI Agent）                     │
├─────────────────────────────────────────────────────────────┤
│  mcp-mqtt     ──→ 实时订阅/发布、设备控制                      │
│  mcp-database ──→ 查询 MySQL 历史数据                         │
│  mcp-vector   ──→ 查询向量库知识文档（提示词）                 │
└─────────────────────────────────────────────────────────────┘
```

## MCP 工具列表

| 工具名称 | 功能 | 使用场景 |
|---------|------|---------|
| mqtt_subscribe | 订阅 MQTT 主题 | 开始接收传感器实时数据 |
| mqtt_unsubscribe | 取消订阅 | 停止接收某主题数据 |
| mqtt_publish | 发布消息 | 控制设备、发送命令 |
| mqtt_query_realtime | 查询实时数据 | 获取最近的传感器数据（内存缓存） |
| mqtt_status | 获取连接状态 | 查看连接状态和订阅列表 |

## 配置说明

### EMQ X 安装（Docker）

```bash
docker run -d --name emqx \
  -p 1883:1883 \
  -p 8083:8083 \
  -p 8883:8883 \
  -p 8084:8084 \
  -p 18083:18083 \
  emqx/emqx
```

### 默认端口

| 端口 | 说明 |
|-----|------|
| 1883 | MQTT over TCP |
| 8883 | MQTT over SSL/TLS |
| 8083 | MQTT over WebSocket |
| 8084 | MQTT over Secure WebSocket |
| 18083 | Dashboard（管理界面） |

### 默认账号

- **用户名**: admin
- **密码**: public
- **Dashboard**: http://localhost:18083

### application.yml 配置

```yaml
mqtt:
  broker:
    host: localhost      # EMQ X 服务器地址
    port: 1883          # MQTT 端口
    clientId: yunxi-mcp-server
  
  cache:
    max-size: 100       # 内存缓存大小

  subscriptions:
    - topic: sensor/temperature/#
      qos: 1
    - topic: sensor/humidity/#
      qos: 1
    - topic: sensor/camera/#
      qos: 0
```

## 使用示例

### 1. 订阅温度传感器

```json
{
  "name": "mqtt_subscribe",
  "arguments": {
    "topic": "sensor/temperature/#",
    "qos": 1
  }
}
```

### 2. 发布设备控制命令

```json
{
  "name": "mqtt_publish",
  "arguments": {
    "topic": "device/ac/control",
    "payload": "{\"action\": \"turn_on\", \"temperature\": 26}"
  }
}
```

### 3. 查询实时数据

```json
{
  "name": "mqtt_query_realtime",
  "arguments": {
    "topic": "sensor/temperature",
    "limit": 10
  }
}
```

## 与其他 MCP 的配合

### 查询历史数据 → mcp-database

```
Agent: "查询过去1小时的温度数据"
1. 调用 mcp-database: SELECT * FROM sensor_temperature WHERE time > NOW() - INTERVAL 1 HOUR
2. 返回结果给 Agent
```

### 实时监控 → mcp-mqtt

```
Agent: "监控温度传感器的实时数据"
1. 调用 mcp-mqtt: mqtt_subscribe("sensor/temperature/#")
2. 实时接收数据并处理
```

### 知识查询 → mcp-vector

```
Agent: "食品安全法规中关于温度的要求是什么？"
1. 调用 mcp-vector: 语义搜索 "食品安全 温度要求"
2. 返回相关文档作为提示词
```

## 运行服务

```bash
# Windows
start.bat

# Linux/Mac
./start.sh

# 或使用 Maven
mvn spring-boot:run
```

## 生产环境建议

1. **启用 SSL/TLS**
2. **配置认证**
3. **使用 EMQ X 集群**
4. **配置 ACL 访问控制**

## 许可证

[根据您的项目许可证填写]
