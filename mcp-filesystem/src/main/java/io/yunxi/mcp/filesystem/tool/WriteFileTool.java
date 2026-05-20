package io.yunxi.mcp.filesystem.tool;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * 写入文件工具
 */
public class WriteFileTool implements ToolHandler {

    private final Path baseDir;

    public WriteFileTool(Path baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public ToolDefinition getDefinition() {
        return ToolDefinition.builder()
                .name("write_file")
                .description(
                        "Write content to a file (creates new file or overwrites existing). " +
                                "写入文件内容（创建新文件或覆盖现有文件）。 " +
                                "Use this when you need to create files, update configuration, or save generated content. "
                                +
                                "适用于创建文件、更新配置或保存生成内容等场景。 " +
                                "Common use cases: creating config files, saving generated code, writing logs. " +
                                "典型用例：创建配置文件、保存生成的代码、写入日志。")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "path", Map.of(
                                        "type", "string",
                                        "description",
                                        "Relative path to the file. Example: 'src/App.java', 'config/settings.json' | "
                                                +
                                                "文件相对路径。示例: 'src/App.java', 'config/settings.json'"),
                                "content", Map.of(
                                        "type", "string",
                                        "description",
                                        "Content to write to the file. Can be text, JSON, code, etc. | " +
                                                "要写入文件的内容。可以是文本、JSON、代码等。")),
                        "required", List.of("path", "content")))
                .build();
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        String path = (String) arguments.get("path");
        String content = (String) arguments.get("content");

        if (path == null || path.isBlank()) {
            return ToolResult.error("File path is required");
        }

        if (content == null) {
            return ToolResult.error("Content is required");
        }

        try {
            Path filePath = resolvePath(path);

            // 确保父目录存在
            if (Files.exists(filePath.getParent())) {
                Files.createDirectories(filePath.getParent());
            }

            Files.writeString(filePath, content);

            return ToolResult.text("File written successfully: " + path + "\nSize: " + content.length() + " bytes");
        } catch (IOException e) {
            return ToolResult.error("Error writing file: " + e.getMessage());
        }
    }

    protected Path resolvePath(String path) {
        Path requested = baseDir.resolve(path).normalize();
        if (!requested.startsWith(baseDir)) {
            throw new SecurityException("Access denied: path outside allowed directory");
        }
        return requested;
    }
}
