# 表单填写 MCP 服务器

## 概述

`mcp-formfill` 是一个智能表单填写 MCP 服务器，通过 WebSocket 实现前端和后端的实时双向通信，支持 AI Agent 直接调用工具填写表单。

## 核心特性

1. **多场景支持**：食谱（单日/周计划）、食安、经费、通用表单
2. **实时双向通信**：基于 WebSocket（STOMP 协议）
3. **字段自动映射**：支持中英文字段映射
4. **批量填写**：支持周计划批量填写
5. **实时反馈**：填写结果实时返回给后端

## 架构

```
AI Agent (Platform)
    ↓ 调用 MCP 工具
FormFiller MCP Server (3003)
    ↓ WebSocket 推送
前端页面 (FormFillClient)
    ↓ 填写表单
结果反馈到后端
```

## 使用方式

### 1. 启动 MCP 服务器

```bash
# Windows
start.bat

# Linux/Mac
./start.sh

# 或使用 Maven
mvn spring-boot:run
```

服务器启动后监听端口：`3003`

### 2. 前端集成

在 HTML 中引入依赖：

```html
<!-- SockJS 和 STOMP -->
<script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/@stomp/stompjs@7.2.0/bundles/stomp.umd.min.js"></script>

<!-- 或直接引入 formfill-client.js（自动加载依赖） -->
<script src="/js/formfill-client.js"></script>
```

初始化客户端：

```javascript
FormFillClient.init({
    wsUrl: 'ws://127.0.0.1:3003/ws/formfill',
    onFormFilled: function(result) {
        console.log('表单填写完成:', result);
    },
    onError: function(error) {
        console.error('WebSocket 错误:', error);
    }
});
```

### 3. AI 调用示例

#### 填写单个食谱

```json
{
  "tool": "formfill_recipe",
  "arguments": {
    "recipeData": {
      "菜谱名称": "全麦鸡蛋三明治",
      "分类": "主食",
      "适用人群": "学生",
      "主要配料": "全麦面包2片、鸡蛋1个、生菜2片",
      "热量": 480,
      "蛋白质": 22,
      "脂肪": 15,
      "碳水": 52
    }
  }
}
```

#### 填写周计划食谱

```json
{
  "tool": "formfill_weekly_recipe",
  "arguments": {
    "weeklyPlan": {
      "monday": {
        "breakfast": { "菜谱名称": "牛奶燕麦", "热量": 350 },
        "lunch": { "菜谱名称": "红烧肉", "热量": 500 },
        "dinner": { "菜谱名称": "清蒸鱼", "热量": 400 }
      },
      "tuesday": {
        "breakfast": { "菜谱名称": "豆浆油条", "热量": 400 },
        "lunch": { "菜谱名称": "宫保鸡丁", "热量": 450 },
        "dinner": { "菜谱名称": "西红柿鸡蛋", "热量": 380 }
      }
    }
  }
}
```

#### 填写食品安全表单

```json
{
  "tool": "formfill_safety",
  "arguments": {
    "safetyData": {
      "样品编号": "SMP-2024-001",
      "样品名称": "鸡蛋",
      "检测日期": "2024-03-18",
      "检测项目": "细菌总数",
      "检测结果": "合格",
      "检测值": 100,
      "标准值": 1000,
      "单位": "CFU/g",
      "检测人": "张三"
    }
  }
}
```

#### 填写经费表单

```json
{
  "tool": "formfill_budget",
  "arguments": {
    "budgetData": {
      "项目编号": "EXP-2024-001",
      "项目名称": "食材采购",
      "费用类别": "ingredients",
      "预算金额": 50000,
      "实际支出": 48000,
      "支付状态": "paid",
      "供应商": "某某供应商"
    }
  }
}
```

## 表单元素 ID 规范

### 单个食谱

```
dishName
category
targetAudience
ingredients
calories
protein
fat
carbs
steps
nutritionNotes
```

### 周计划食谱

```
recipe-monday-breakfast-dishName
recipe-monday-breakfast-calories
recipe-monday-lunch-dishName
recipe-tuesday-dinner-dishName
...
```

格式：`recipe-{day}-{meal}-{field}`

- day: monday, tuesday, wednesday, thursday, friday, saturday, sunday
- meal: breakfast, lunch, dinner
- field: dishName, calories, protein, fat, carbs 等

### 食安表单

```
sampleId
sampleName
testDate
testItem
testResult
testValue
standardValue
unit
inspector
notes
```

### 经费表单

```
expenseId
expenseName
category
budgetAmount
actualAmount
paymentStatus
paymentDate
supplier
notes
```

## 字段映射

### 食谱表单

| 中文 | 英文 ID |
|------|---------|
| 菜谱名称 | dishName |
| 分类 | category |
| 适用人群 | targetAudience |
| 主要配料 | ingredients |
| 热量 | calories |
| 蛋白质 | protein |
| 脂肪 | fat |
| 碳水 | carbs |

### 分类映射

| 中文 | 英文 |
|------|------|
| 主食 | main |
| 副菜 | side |
| 汤品 | soup |
| 甜品 | dessert |
| 早餐 | breakfast |
| 午餐 | lunch |
| 晚餐 | dinner |

### 适用人群映射

| 中文 | 英文 |
|------|------|
| 学生 | students |
| 教师 | teachers |
| 全体 | all |

## API 文档

### 可用工具

1. **formfill_recipe** - 填写食谱表单
2. **formfill_weekly_recipe** - 批量填写周计划食谱
3. **formfill_safety** - 填写食品安全表单
4. **formfill_budget** - 填写经费管理表单
5. **formfill_generic** - 通用表单填写
6. **formfill_get_structure** - 获取表单结构

### WebSocket 端点

- 连接端点：`ws://127.0.0.1:3003/ws/formfill`
- 消息主题：`/topic/formfill`
- 应用前缀：`/app`

## 调试

### 查看后端日志

```bash
# 查看 WebSocket 连接日志
tail -f logs/mcp-formfill.log | grep "WebSocket"

# 查看填写指令日志
tail -f logs/mcp-formfill.log | grep "填写指令"
```

### 查看前端日志

打开浏览器控制台，查看以下日志：
- `[FormFillClient] WebSocket 已连接`
- `[FormFillClient] 收到填写指令`
- `[FormFill] 表单填写完成`

## 常见问题

### 1. WebSocket 连接失败

检查：
- MCP 服务器是否启动（端口 3003）
- WebSocket URL 是否正确
- 防火墙是否阻止连接

### 2. 表单未填写成功

检查：
- 表单元素 ID 是否符合规范
- WebSocket 消息是否正确发送
- 前端控制台是否有错误

### 3. AI 调用失败

检查：
- MCP 工具是否正确注册到 Platform
- 工具参数格式是否正确
- 后端日志是否有错误信息

## 扩展开发

### 添加新场景

1. 在 `FormFillerTools` 中添加新工具方法
2. 在 `FormFillerService` 中添加场景配置
3. 在 `FormFillClient` 中添加对应的处理逻辑
4. 更新表单元素 ID 规范

### 添加新字段

1. 更新场景的 `FIELD_MAPPING`
2. 更新表单 HTML，添加对应的元素
3. 测试字段映射和填写功能

## 技术栈

- **后端**：Spring Boot 3.2.0, Spring WebSocket, STOMP
- **前端**：SockJS, STOMP.js
- **通信协议**：WebSocket (STOMP over SockJS)
- **消息格式**：JSON

## 许可证

MIT License
