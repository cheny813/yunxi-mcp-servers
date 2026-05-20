package io.yunxi.mcp.qdrant;

import io.yunxi.mcp.qdrant.tool.QdrantTools;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Qdrant MCP 控制器
 * <p>
 * 提供 Qdrant 向量数据库操作的 MCP 端点。
 * </p>
 *
 * <h3>提供的工具</h3>
 * <ul>
 * <li>qdrant_search - 向量搜索</li>
 * <li>qdrant_list_collections - 列出集合</li>
 * <li>qdrant_collection_info - 获取集合信息</li>
 * </ul>
 *
 * @author yunxi-mcp-servers
 */
@RestController
@RequestMapping("/mcp")
@ConditionalOnProperty(name = "qdrant.enabled", havingValue = "true", matchIfMissing = true)
public class QdrantMcpController {

    private final QdrantTools.SearchTool searchTool;
    private final QdrantTools.ListCollectionsTool listCollectionsTool;
    private final QdrantTools.GetCollectionInfoTool getCollectionInfoTool;

    public QdrantMcpController(
            QdrantTools.SearchTool searchTool,
            QdrantTools.ListCollectionsTool listCollectionsTool,
            QdrantTools.GetCollectionInfoTool getCollectionInfoTool) {
        this.searchTool = searchTool;
        this.listCollectionsTool = listCollectionsTool;
        this.getCollectionInfoTool = getCollectionInfoTool;
    }

    @GetMapping("/tools")
    public List<ToolDefinition> listTools() {
        return List.of(
                searchTool.getDefinition(),
                listCollectionsTool.getDefinition(),
                getCollectionInfoTool.getDefinition());
    }

    @PostMapping("/execute")
    public ToolResult execute(@RequestBody Map<String, Object> request) {
        String toolName = (String) request.get("name");
        @SuppressWarnings("unchecked")
        Map<String, Object> args = (Map<String, Object>) request.get("arguments");

        if (toolName == null) {
            return ToolResult.error("Error: tool name is required");
        }

        return switch (toolName) {
            case "qdrant_search" -> searchTool.execute(args != null ? args : Map.of());
            case "qdrant_list_collections" -> listCollectionsTool.execute(args != null ? args : Map.of());
            case "qdrant_collection_info" -> getCollectionInfoTool.execute(args != null ? args : Map.of());
            default -> ToolResult.error("Error: unknown tool: " + toolName);
        };
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "healthy",
                "service", "qdrant",
                "tools", List.of("qdrant_search", "qdrant_list_collections", "qdrant_collection_info"));
    }
}