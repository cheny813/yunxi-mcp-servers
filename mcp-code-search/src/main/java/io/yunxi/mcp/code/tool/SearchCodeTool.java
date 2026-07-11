package io.yunxi.mcp.code.tool;

import org.springframework.stereotype.Component;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.code.service.CodeSearchService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 搜索代码工具
 * <p>
 * 在代码库中搜索指定关键词，返回匹配的代码片段和文件位置。
 * 支持按文件路径过滤，可限制返回结果数量。
 * </p>
 *
 * <h3>功能特性</h3>
 * <ul>
 * <li>关键词搜索 - 不区分大小写的模糊匹配</li>
 * <li>文件过滤 - 支持文件路径模式匹配</li>
 * <li>并行搜索 - 使用并行流提高搜索效率</li>
 * <li>智能截断 - 限制每文件返回的匹配行数</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * 
 * <pre>
 * {
 *   "keyword": "UserService",
 *   "filePattern": "*.java",
 *   "maxResults": 10
 * }
 * </pre>
 *
 * @author yunxi-mcp-servers
 * @see CodeSearchService
 * @since 1.0.0
 */
@Component
public class SearchCodeTool implements ToolHandler {

    /** 代码搜索服务 */
    private final CodeSearchService codeSearchService;

    /**
     * 构造方法
     *
     * @param codeSearchService 代码搜索服务实例
     */
    public SearchCodeTool(CodeSearchService codeSearchService) {
        this.codeSearchService = codeSearchService;
    }

    /**
     * 获取工具定义
     *
     * @return 工具定义，包含名称、描述和参数模式
     */
    @Override
    public String getName() {
        return "search_code";
    }

    @Override
    public String getDescription() {
        return "搜索代码库中的代码。\n根据关键词搜索所有代码文件，返回匹配的代码片段和文件位置。\n用于理解代码结构、查找实现位置、分析业务逻辑。";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getInputSchema() {
        return (Map<String, Object>) (Map) Map.of(
                "type", "object",
                "properties", Map.of(
                        "keyword", Map.of(
                                "type", "string",
                                "description", "搜索关键词"),
                        "filePattern", Map.of(
                                "type", "string",
                                "description", "文件路径过滤，如 *.java, **/service/*.java"),
                        "maxResults", Map.of(
                                "type", "number",
                                "description", "最大结果数，默认10")));
    }

    /**
     * 执行代码搜索
     *
     * @param arguments 搜索参数，包含 keyword、filePattern、maxResults
     * @return 搜索结果，包含匹配的文件和代码片段
     */
    @Override
    public String execute(Map<String, Object> arguments) {
        String keyword = (String) arguments.get("keyword");
        String filePattern = (String) arguments.get("filePattern");
        int maxResults = arguments.containsKey("maxResults")
                ? ((Number) arguments.get("maxResults")).intValue()
                : 10;

        if (keyword == null || keyword.isBlank()) {
            throw new RuntimeException("keyword is required");
        }

        try {
            List<CodeSearchService.CodeSearchResult> results = codeSearchService.search(keyword, filePattern,
                    maxResults);

            if (results.isEmpty()) {
                return "未找到匹配的代码";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("找到 ").append(results.size()).append(" 个匹配结果:\n\n");

            for (var result : results) {
                sb.append("📄 ").append(result.fileName()).append("\n");
                sb.append("   路径: ").append(result.filePath()).append("\n");
                sb.append("   匹配:\n");
                result.matchedLines().stream().limit(3).forEach(line -> sb.append("   > ").append(line).append("\n"));
                if (result.matchedLines().size() > 3) {
                    sb.append("   ... 还有 ").append(result.matchedLines().size() - 3).append(" 行\n");
                }
                sb.append("\n");
            }

            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("搜索代码失败: " + e.getMessage());
        }
    }
}