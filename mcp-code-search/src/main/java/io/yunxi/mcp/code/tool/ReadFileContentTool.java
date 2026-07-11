package io.yunxi.mcp.code.tool;

import org.springframework.stereotype.Component;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.code.service.CodeSearchService;

import java.util.Map;

/**
 * 读取文件内容工具
 * <p>
 * 提供代码文件内容读取功能，支持查看具体代码实现。
 * </p>
 *
 * <h3>功能特性</h3>
 * <ul>
 * <li>按相对路径读取文件内容</li>
 * <li>自动限制返回行数（默认200行）</li>
 * <li>支持多种代码文件类型</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * 
 * <pre>
 * {
 *   "path": "src/main/java/Service.java",
 *   "maxLines": 100
 * }
 * </pre>
 *
 * @author yunxi-mcp-servers
 * @since 1.0.0
 */
@Component
public class ReadFileContentTool implements ToolHandler {

    /** 代码搜索服务 */
    private final CodeSearchService codeSearchService;

    /**
     * 构造方法
     *
     * @param codeSearchService 代码搜索服务实例
     */
    public ReadFileContentTool(CodeSearchService codeSearchService) {
        this.codeSearchService = codeSearchService;
    }

    /**
     * 获取工具定义
     *
     * @return 工具定义信息
     */
    @Override
    public String getName() {
        return "read_file_content";
    }

    @Override
    public String getDescription() {
        return "读取指定文件的内容。\n根据相对路径读取代码文件内容，用于查看具体代码实现。\n路径相对于代码搜索根目录。";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getInputSchema() {
        return (Map<String, Object>) (Map) Map.of(
                "type", "object",
                "properties", Map.of(
                        "path", Map.of(
                                "type", "string",
                                "description", "文件相对路径，如 src/main/java/Service.java"),
                        "maxLines", Map.of(
                                "type", "number",
                                "description", "最大读取行数，默认200")));
    }

    /**
     * 执行文件读取
     *
     * @param arguments 工具参数
     * @return 工具执行结果
     */
    @Override
    public String execute(Map<String, Object> arguments) {
        String path = (String) arguments.get("path");

        if (path == null || path.isBlank()) {
            throw new RuntimeException("path is required");
        }

        try {
            String content = codeSearchService.readFile(path);

            // 限制行数
            String[] lines = content.split("\n");
            if (lines.length > 200) {
                content = String.join("\n", java.util.Arrays.copyOfRange(lines, 0, 200));
                content += "\n\n... (省略 " + (lines.length - 200) + " 行)";
            }

            return "文件: " + path + "\n\n" + content;
        } catch (Exception e) {
            throw new RuntimeException("读取文件失败: " + e.getMessage());
        }
    }
}