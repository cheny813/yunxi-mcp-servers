# mcp-formfill

通用表单填写 MCP 服务器。接收上游 LLM 已映射好的表单数据，通过 WebSocket 把填写指令下发给前端执行。

**服务端不做字段映射或值翻译**。映射由 LLM 根据 `formfill_get_structure` 返回的真实页面字段（前端回写的 DOM 结构）完成，服务端仅做透传转发。

## 工作流程

```
前端页面 → 回写 DOM 字段（PageStructureRegistry）
                ↓
LLM 调用 formfill_get_structure → 获取真实页面字段列表
LLM 调用 formfill_fill（formData 键=表单元素 ID）→ 服务端
                ↓
        WebSocket 下发到前端 → 前端按元素 ID 填写
```

## 工具列表

| 工具 | 说明 |
|------|------|
| `formfill_fill` | 下发填写指令：传入 `scene` + `formData` + `params` |
| `formfill_get_structure` | 获取某场景的真实页面字段和周计划维度 |
| `formfill_list_scenes` | 列出所有已注册场景 |

## 快速开始

```bash
mvn clean package
java -jar target/mcp-formfill-1.0.0.jar
```

WebSocket 端点：`/ws/formfill`（STOMP，允许跨域）。填写指令下发到主题 `/topic/formfill`，前端反馈发送到 `/app/formfill`。

## 配置场景

在 `src/main/resources/scenarios/` 下新增场景 JSON：

```json
{
  "id": "order-form",
  "name": "订单表单",
  "description": "订单提交场景"
}
```

服务启动时自动加载，随后通过 `formfill_fill(scene="order-form", ...)` 调用。

## 场景 JSON 字段说明

| 字段 | 含义 |
|------|------|
| `id` | 场景唯一标识，作为 `formfill_fill` 的 `scene` 参数 |
| `name` | 展示名称，由 `formfill_list_scenes` 返回 |
| `description` | 场景描述 |

## 依赖

- Spring Boot（Web + WebSocket）
- `mcp-common`（本仓库公共模块）
- 前端通过 STOMP over WebSocket 接收填写指令
