# MCP Playwright Server

Playwright 浏览器自动化 MCP 服务器 - 支持页面读取、表单填写、自动提交。

## 功能定位

### 核心能力
- **页面导航** - 打开传统系统页面
- **表单读取** - 提取输入框、下拉框、表格数据
- **表单填写** - AI处理后自动回填表单
- **自动提交** - 触发保存或提交操作
- **截图保存** - 记录操作结果

### 典型应用场景

```
传统采购系统页面
       ↓
1. Playwright 打开页面
       ↓
2. 提取表单数据（供应商、金额、商品等）
       ↓
3. 传给 AI Agent 智能处理（审批、风险检查等）
       ↓
4. AI 返回处理结果
       ↓
5. Playwright 自动回填审批结果
       ↓
6. 自动提交表单
```

## MCP 工具列表

| 工具名称 | 功能 | 使用场景 |
|---------|------|---------|
| playwright_start_browser | 启动浏览器 | 开始自动化操作 |
| playwright_navigate | 导航到URL | 打开指定页面 |
| playwright_get_content | 获取页面内容 | 读取页面HTML |
| playwright_click | 点击元素 | 点击按钮、链接 |
| playwright_type | 输入文本 | 填写输入框 |
| playwright_close_browser | 关闭浏览器 | 结束自动化 |

## 配置说明

### application.yml

```yaml
playwright:
  browser: chromium        # 浏览器类型：chromium, firefox, webkit
  headless: true            # 无头模式（无界面）
  viewport-width: 1920      # 视口宽度
  viewport-height: 1080     # 视口高度
  timeout:
    default: 30000         # 默认超时
    navigation: 60000       # 导航超时
```

## 使用示例

### 1. 自动化表单填写流程

```json
{
  "name": "playwright_start_browser"
}
```

```json
{
  "name": "playwright_navigate",
  "arguments": {
    "url": "https://erp.yunxi.com/purchase/create"
  }
}
```

```json
{
  "name": "playwright_type",
  "arguments": {
    "selector": "#supplier-name",
    "text": "供应商A"
  }
}
```

```json
{
  "name": "playwright_click",
  "arguments": {
    "selector": "#submit-button"
  }
}
```

```json
{
  "name": "playwright_close_browser"
}
```

### 2. 复合工作流示例

**场景：自动审批采购申请**

```
1. 打开采购申请页面
2. 读取申请单数据
3. AI 智能审批（检查资质、金额、历史记录）
4. 填写审批意见
5. 点击提交
```

## 支持的表单元素

| 元素类型 | 操作 | 示例 |
|---------|------|------|
| 输入框 | fill/type | 文本输入框 |
| 下拉框 | selectOption | 选择下拉选项 |
| 复选框 | check/uncheck | 勾选/取消勾选 |
| 单选框 | check | 选择单选按钮 |
| 按钮 | click | 点击按钮 |
| 链接 | click | 点击链接 |
| 文件上传 | setInputFiles | 上传文件 |

## 开发提示

### Playwright 安装浏览器驱动
```bash
# 首次运行时需要安装浏览器驱动
mvn exec:exec -Dexec.executable="playwright" -Dexec.args="install chromium"
```

### 常见定位器

```css
/* ID */
#submit-button

/* Class */
.btn-primary

/* Name */
input[name="username"]

/* CSS Selector */
form > div:first-child > input

/* XPath */
//button[@type="submit"]
```

## 运行服务

```bash
# Windows
cd d:\work\code\yunxi-mcp-servers\mcp-playwright
start.bat

# Linux/Mac
./start.sh

# 或使用 Maven
mvn spring-boot:run
```

## 安全建议

1. **权限控制** - 限制可访问的URL白名单
2. **敏感数据** - 不记录密码等敏感信息
3. **日志脱敏** - 日志中隐藏表单数据
4. **超时设置** - 设置合理的超时时间
5. **错误处理** - 完善的异常处理和回滚机制

## 注意事项

**当前版本为框架版本，核心功能需要根据实际需求完善：**

- Playwright Java SDK 的完整集成
- 表单数据提取的完整实现
- 支持更多表单元素类型
- 文件上传、日期选择器等高级功能

建议在实际部署时根据 Playwright SDK 文档进行完整实现。
