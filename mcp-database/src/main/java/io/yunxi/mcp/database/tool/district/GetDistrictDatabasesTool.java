package io.yunxi.mcp.database.tool.district;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import io.yunxi.mcp.database.service.DistrictService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 获取区县数据库列表工具
 */
public class GetDistrictDatabasesTool implements ToolHandler {

    private final DistrictService districtService;

    public GetDistrictDatabasesTool(DistrictService districtService) {
        this.districtService = districtService;
    }

    @Override
    public ToolDefinition getDefinition() {
        return ToolDefinition.builder()
                .name("get_district_databases")
                .description(
                        "获取指定区县的所有数据库配置。\n" +
                        "返回该区县配置的所有数据库连接信息，包括营养数据库、食安数据库、预算数据库等。\n" +
                        "用于了解区县可以访问哪些业务数据库。")
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
            var databases = districtService.getDatabases(districtId);
            
            if (databases == null || databases.isEmpty()) {
                return ToolResult.text("未找到区县数据库配置: " + districtId);
            }

            StringBuilder sb = new StringBuilder();
            sb.append("区县数据库配置 (").append(districtId).append("):\n\n");

            for (var db : databases) {
                sb.append("- 数据库: ").append(db.getDbType()).append("\n");
                sb.append("  主机: ").append(db.getHost()).append(":").append(db.getPort()).append("\n");
                sb.append("  库名: ").append(db.getDatabaseName()).append("\n");
                sb.append("  描述: ").append(db.getDescription()).append("\n");
                sb.append("\n");
            }

            return ToolResult.text(sb.toString());
        } catch (Exception e) {
            return ToolResult.error("获取区县数据库列表失败: " + e.getMessage());
        }
    }
}