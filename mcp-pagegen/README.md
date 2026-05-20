# MCP Page Generator Server

页面生成 MCP 服务器 - 支持生成统计仪表盘、列表页、详情页。

## 功能定位

### 核心能力
- **统计仪表盘** - 生成包含KPI卡片和图表的仪表盘
- **数据列表页** - 生成分页、筛选、排序的数据表格
- **详情页** - 生成信息展示和关联数据的详情页面
- **图表集成** - 集成 ECharts 图表（复用 mcp-chart）
- **数据导出** - 支持导出 Excel、PDF

### 页面类型

| 页面类型 | 功能 | 组件 |
|---------|------|------|
| **仪表盘** | 关键指标 + 多图表 | KPI卡片、图表容器 |
| **列表页** | 数据展示 + 分页 | 表格、搜索、筛选、导出 |
| **详情页** | 信息展示 | 卡片、关联数据、操作按钮 |
| **报表页** | 图表 + 汇总 | 图表、数据表格、统计信息 |

## MCP 工具列表

| 工具名称 | 功能 | 使用场景 |
|---------|------|---------|
| pagegen_dashboard | 生成统计仪表盘 | 数据统计、可视化大屏 |
| pagegen_list | 生成数据列表页 | 数据查询、列表展示 |
| pagegen_detail | 生成详情页 | 信息详情、关联数据 |

## 配置说明

### application.yml

```yaml
pagegen:
  theme: bootstrap5          # 主题：bootstrap5, adminlte, tailwind
  chart:
    enabled: true             # 是否启用图表
    chart-service-url: http://localhost:8087
  table:
    default-page-size: 20    # 默认每页显示条数
    export-enabled: true      # 是否启用导出
```

## 使用示例

### 1. 生成统计仪表盘

```json
{
  "name": "pagegen_dashboard",
  "arguments": {
    "title": "采购数据统计",
    "kpi_cards": [
      {
        "label": "今日采购量",
        "value": "1,234 kg",
        "trend": "+12%"
      },
      {
        "label": "采购金额",
        "value": "¥56,789",
        "trend": "+8%"
      }
    ],
    "charts": [
      {
        "title": "采购趋势",
        "config": "{\"xAxis\":{\"type\":\"category\"},\"yAxis\":{\"type\":\"value\"},\"series\":[{\"data\":[100,200,150]}]}"
      }
    ]
  }
}
```

### 2. 生成数据列表页

```json
{
  "name": "pagegen_list",
  "arguments": {
    "title": "采购记录列表",
    "columns": ["编号", "供应商", "商品", "数量", "金额", "状态"],
    "data": [
      {"编号": "PO001", "供应商A", "大米", "500kg", "¥2,500", "已审批"},
      {"编号": "PO002", "供应商B", "食用油", "200kg", "¥1,800", "待审批"}
    ]
  }
}
```

### 3. 生成详情页

```json
{
  "name": "pagegen_detail",
  "arguments": {
    "title": "采购单详情",
    "entity": {
      "编号": "PO001",
      "供应商": "供应商A",
      "联系人": "张三",
      "电话": "13800138000",
      "创建时间": "2024-03-16 10:30:00"
    },
    "related_data": [
      {
        "title": "采购明细",
        "data": [...]
      },
      {
        "title": "审批记录",
        "data": [...]
      }
    ]
  }
}
```

## 页面示例

### 统计仪表盘页面

生成的页面包含：
1. **KPI 卡片** - 关键指标展示（带趋势箭头）
2. **图表区域** - 多个图表（ECharts集成）
3. **响应式布局** - 适配不同屏幕

### 数据列表页

生成的页面包含：
1. **搜索栏** - 关键词搜索
2. **筛选器** - 多条件筛选
3. **数据表格** - 分页、排序、导出
4. **操作按钮** - 查看、编辑、删除

### 详情页

生成的页面包含：
1. **信息卡片** - 实体信息展示
2. **关联数据** - 相关数据列表
3. **操作按钮** - 编辑、删除、返回

## 页面输出

所有生成的页面都是完整的 HTML 文件，可以直接：
- 在浏览器中打开查看
- 嵌入现有系统（iframe）
- 部署为独立页面
- 导出为 PDF（可选功能）

## 运行服务

```bash
# Windows
cd d:\work\code\yunxi-mcp-servers\mcp-pagegen
start.bat

# Linux/Mac
./start.sh

# 或使用 Maven
mvn spring-boot:run
```

## 与 mcp-chart 的配合

mcp-pagegen 生成的页面可以调用 mcp-chart 服务生成图表：

```javascript
// 页面中通过 HTTP 调用 mcp-chart
fetch('http://localhost:8087/api/chart/bar', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(chartConfig)
})
  .then(res => res.json())
  .then(data => {
    // 显示生成的图表图片
    document.getElementById('chart-image').src = data.image;
  });
```

## 扩展功能

### 后续可以添加的功能

1. **模板管理** - 保存和复用页面模板
2. **组件库** - 可复用的 UI 组件（时间选择器、地区选择器等）
3. **主题切换** - 支持多套主题
4. **权限控制** - 根据用户权限显示不同组件
5. **数据刷新** - 定时刷新数据

## 开发提示

### 集成其他 MCP 服务

生成的页面可以集成：
- **mcp-chart** - 生成图表
- **mcp-database** - 查询数据库数据
- **mcp-redis** - 缓存数据
- **mcp-playwright** - 自动化测试生成的页面

### 自定义样式

可以通过以下方式自定义样式：
1. 修改 PageGenService 中的 HTML 模板
2. 添加自定义 CSS 样式
3. 集成前端框架（Vue、React）

## 注意事项

**当前版本为框架版本，支持基础页面生成，高级功能需根据实际需求完善：**

- Excel/PDF 导出功能
- 复杂的表单组件
- 实时数据刷新
- 用户权限管理

建议在实际部署时根据业务需求逐步完善这些功能。
