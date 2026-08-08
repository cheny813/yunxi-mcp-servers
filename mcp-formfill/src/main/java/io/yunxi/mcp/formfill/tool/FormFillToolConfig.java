package io.yunxi.mcp.formfill.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.formfill.service.FormFillerService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * 表单填写 MCP 工具注册
 *
 * 通过 mcp-common 的 {@link ToolHandler} 机制注册工具，由 McpServerFactory 自动暴露为 MCP 工具。
 *
 * 对外暴露的工具（均场景配置驱动，工具名不含业务词汇）：
 * - formfill_fill          根据场景标识(scene)填写表单（通用，推荐）
 * - formfill_get_structure 获取某场景的表单结构
 * - formfill_list_scenes  列出所有已注册场景
 */
@Configuration
@RequiredArgsConstructor
public class FormFillToolConfig {

    private final FormFillerService formFillerService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    @Bean
    public ToolHandler formfillFill() {
        return new ToolHandler() {
            @Override
            public String getName() { return "formfill_fill"; }

            @Override
            public String getDescription() {
                return "将 LLM 已映射好的表单数据通过 WebSocket 下发到前端执行填写。"
                        + "可用场景通过 formfill_list_scenes 查询。params 可包含 targetPage、requestId、waitFeedback 等透传参数。";
            }

            @Override
            public Map<String, Object> getInputSchema() {
                return Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "scene", Map.of("type", "string", "description", "场景标识"),
                                "formData", Map.of("type", "object", "description", "表单数据，键为表单元素 ID（通过 formfill_get_structure 获取），值为待填写内容"),
                                "params", Map.of("type", "object", "description", "透传参数：targetPage、requestId、waitFeedback 等")
                        ),
                        "required", List.of("scene", "formData")
                );
            }

            @Override
            public String execute(Map<String, Object> arguments) {
                String scene = (String) arguments.get("scene");
                @SuppressWarnings("unchecked")
                Map<String, Object> formData = (Map<String, Object>) arguments.getOrDefault("formData", Map.of());
                @SuppressWarnings("unchecked")
                Map<String, Object> params = (Map<String, Object>) arguments.getOrDefault("params", Map.of());
                try {
                    return toJson(formFillerService.fillForm(scene, formData, params));
                } catch (Exception e) {
                    return "{\"success\":false,\"error\":\"" + e.getMessage() + "\"}";
                }
            }
        };
    }

    @Bean
    public ToolHandler formfillGetStructure() {
        return new ToolHandler() {
            @Override
            public String getName() { return "formfill_get_structure"; }

            @Override
            public String getDescription() {
                return "获取指定场景的表单结构（前端回写的真实页面字段），LLM 应据此产出 formData。";
            }

            @Override
            public Map<String, Object> getInputSchema() {
                return Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "scene", Map.of("type", "string", "description", "场景标识")
                        ),
                        "required", List.of("scene")
                );
            }

            @Override
            public String execute(Map<String, Object> arguments) {
                return toJson(formFillerService.getFormStructure((String) arguments.get("scene")));
            }
        };
    }

    @Bean
    public ToolHandler formfillListScenes() {
        return new ToolHandler() {
            @Override
            public String getName() { return "formfill_list_scenes"; }

            @Override
            public String getDescription() { return "列出所有已注册的场景配置（id、名称、描述）。"; }

            @Override
            public Map<String, Object> getInputSchema() {
                return Map.of("type", "object", "properties", Map.of());
            }

            @Override
            public String execute(Map<String, Object> arguments) {
                return toJson(Map.of("scenes", formFillerService.listScenes(), "count", formFillerService.listScenes().size()));
            }
        };
    }
}
