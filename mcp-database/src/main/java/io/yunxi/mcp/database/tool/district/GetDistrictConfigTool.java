package io.yunxi.mcp.database.tool.district;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import io.yunxi.mcp.database.service.DistrictService;

import java.util.List;
import java.util.Map;

/**
 * 获取区县配置工具
 */
public class GetDistrictConfigTool implements ToolHandler {

    private final DistrictService districtService;

    public GetDistrictConfigTool(DistrictService districtService) {
        this.districtService = districtService;
    }

    @Override
    public ToolDefinition getDefinition() {
        return ToolDefinition.builder()
                .name("get_district_config")
                .description(
                        "获取指定区县的配置信息。\n" +
                        "用于查询区县的名称、省份、联系人等基本信息。\n" +
                        "当需要了解特定区县的配置时使用。")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "districtId", Map.of(
                                        "type", "string",
                                        "description", "区县ID，如 district_001")),
                        "required", List.of("districtId")))
                .build();
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        String districtId = (String) arguments.get("districtId");
        
        if (districtId == null || districtId.isBlank()) {
            return ToolResult.error("districtId is required");
        }

        try {
            var district = districtService.getDistrict(districtId);
            if (district == null) {
                return ToolResult.text("未找到区县配置: " + districtId);
            }

            StringBuilder sb = new StringBuilder();
            sb.append("区县配置:\n");
            sb.append("- ID: ").append(district.getId()).append("\n");
            sb.append("- 名称: ").append(district.getName()).append("\n");
            sb.append("- 省份: ").append(district.getProvince()).append("\n");
            sb.append("- 联系人: ").append(district.getContact()).append("\n");
            sb.append("- 状态: ").append(district.getStatus()).append("\n");

            return ToolResult.text(sb.toString());
        } catch (Exception e) {
            return ToolResult.error("获取区县配置失败: " + e.getMessage());
        }
    }
}