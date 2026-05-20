package io.yunxi.mcp.filesystem.tool;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 读取文件工具
 */
public class ReadFileTool implements ToolHandler {

    private final Path baseDir;

    public ReadFileTool(Path baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public ToolDefinition getDefinition() {
        return ToolDefinition.builder()
                .name("read_file")
                .description(
                        "Read the contents of a file. " +
                                "读取文件内容。 " +
                                "Use this when you need to read configuration files, source code, logs, or any text-based files. "
                                +
                                "适用于读取配置文件、源代码、日志或任何文本文件等场景。 " +
                                "Common use cases: reading config files, viewing source code, checking logs. " +
                                "典型用例：读取配置文件、查看源代码、检查日志。")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "path", Map.of(
                                        "type", "string",
                                        "description", "Relative path to the file (relative to allowed directory). " +
                                                "Example: 'src/main.java', 'config/app.yml' | " +
                                                "文件相对路径（相对于允许目录）。示例: 'src/main.java', 'config/app.yml'"),
                                "lines", Map.of(
                                        "type", "integer",
                                        "description",
                                        "Number of lines to read (optional, default: all). Example: 50 | " +
                                                "要读取的行数（可选，默认：全部）。示例: 50")),
                        "required", List.of("path")))
                .build();
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        String path = (String) arguments.get("path");
        Integer lines = arguments.containsKey("lines") ? ((Number) arguments.get("lines")).intValue() : null;

        if (path == null || path.isBlank()) {
            return ToolResult.error("File path is required");
        }

        try {
            Path filePath = resolvePath(path);

            if (!Files.exists(filePath)) {
                return ToolResult.error("File not found: " + path);
            }

            if (Files.isDirectory(filePath)) {
                return ToolResult.error("Path is a directory, not a file: " + path);
            }

            String content;
            if (lines != null && lines > 0) {
                try (Stream<String> stream = Files.lines(filePath)) {
                    content = stream.limit(lines).reduce("", (a, b) -> a + b + "\n");
                }
            } else {
                content = Files.readString(filePath);
            }

            return ToolResult.text("File: " + path + "\n\n" + content);
        } catch (IOException e) {
            return ToolResult.error("Error reading file: " + e.getMessage());
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
