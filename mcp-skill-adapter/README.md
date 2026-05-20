# Python Skill MCP Adapter

一个通用的 Python Skill MCP 适配器，允许直接运行网上下载的 Skill，并将其暴露为 MCP 工具。

## 功能特性

- 自动扫描并加载 skills 目录下的 Python Skill
- 标准化的 MCP 协议接口（HTTP + SSE）
- 支持原生 Python Skill 脚本
- 统一的错误处理和日志

## 快速开始

### 1. 安装依赖

```bash
pip install -r requirements.txt
```

### 2. 配置

```bash
# 复制并编辑配置
cp config.example.yaml config.yaml

# 设置必要的环境变量（如 API Keys）
export BAIDU_API_KEY="your-api-key"
```

### 3. 添加 Skill

将下载的 Skill 放入 `skills/` 目录，使用适配器包装：

```python
# skills/my_skill.py
from skill_adapter import BaseSkill

class MySkill(BaseSkill):
    name = "my_skill"
    description = "我的 Skill"
    
    def execute(self, **kwargs):
        # 直接调用原始逻辑
        return {"result": "success"}
```

### 4. 启动服务

```bash
python main.py
```

### 5. 测试

```bash
# 获取工具列表
curl http://localhost:8090/mcp/tools

# 调用工具
curl -X POST http://localhost:8090/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"baidu_search","arguments":{"query":"测试"}},"id":1}'
```

## 目录结构

```
mcp-skill-adapter/
├── main.py                 # 入口
├── skill_adapter/          # 核心框架
│   ├── __init__.py
│   ├── base.py            # 基础 Skill 类
│   ├── loader.py          # Skill 加载器
│   └── mcp_server.py      # MCP 服务器
├── skills/                 # Skill 存放目录
│   └── baidu_search/      # 示例 Skill
│       ├── skill.py       # 适配器
│       └── scripts/       # 原始脚本
├── config.example.yaml    # 配置示例
└── requirements.txt        # Python 依赖
```

## MCP 端点

| 端点 | 方法 | 描述 |
|------|------|------|
| `/mcp` | POST | JSON-RPC 调用 |
| `/mcp/tools` | GET | 获取工具列表 |
| `/mcp/sse` | GET | SSE 连接 |
| `/mcp/message` | POST | SSE 消息 |
| `/mcp/health` | GET | 健康检查 |

## 配置说明

```yaml
server:
  host: "0.0.0.0"
  port: 8090

skills:
  directory: "./skills"
  auto_load: true
  # Skill 特定配置
  env:
    BAIDU_API_KEY: ${BAIDU_API_KEY}
```

## 示例

参考 `skills/baidu_search` 目录下的示例，了解如何将原始 Python Skill 适配为 MCP 工具。