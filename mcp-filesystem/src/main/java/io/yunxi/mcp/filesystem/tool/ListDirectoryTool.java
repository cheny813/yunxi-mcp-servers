package io.yunxi.mcp.filesystem.tool;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 列出目录内容工具
 */
public class ListDirectoryTool implements ToolHandler {

    private final Path baseDir;

    public ListDirectoryTool(Path baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public ToolDefinition getDefinition() {
        return ToolDefinition.builder()
                .name("list_directory")
                .description(
                        "List contents of a directory. " +
                                "列出目录内容。 " +
                                "Use this when you need to explore file structure, find available files, or navigate directories. "
                                +
                                "适用于探索文件结构、查找可用文件或导航目录等场景。 " +
                                "Common use cases: exploring project structure, finding files, checking directory contents. "
                                +
                                "典型用例：探索项目结构、查找文件、检查目录内容。")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "path", Map.of(
                                        "type", "string",
                                        "description",
                                        "Relative path to directory (empty = root). Example: 'src', 'config' | " +
                                                "目录相对路径（空=根目录）。示例: 'src', 'config'"),
                                "recursive", Map.of(
                                        "type", "boolean",
                                        "description", "List subdirectories recursively (default: false). | " +
                                                "递归列出子目录（默认: false）。"))))
                .build();
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        String path = (String) arguments.get("path");
        Boolean recursive = arguments.containsKey("recursive") ? (Boolean) arguments.get("recursive") : false;

        try {
            Path dirPath = resolvePath(path != null ? path : "");

            if (!Files.exists(dirPath)) {
                return ToolResult.error("Directory not found: " + path);
            }

            if (!Files.isDirectory(dirPath)) {
                return ToolResult.error("Path is not a directory: " + path);
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Directory: ").append(path != null ? path : "/").append("\n\n");

            if (recursive) {
                listRecursive(dirPath, sb, 0);
            } else {
                listSimple(dirPath, sb);
            }

            return ToolResult.text(sb.toString());
        } catch (IOException e) {
            return ToolResult.error("Error listing directory: " + e.getMessage());
        }
    }

    private void listSimple(Path dirPath, StringBuilder sb) throws IOException {
        List<Map<String, Object>> items = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
            for (Path entry : stream) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name", entry.getFileName().toString());
                item.put("type", Files.isDirectory(entry) ? "dir" : "file");
                if (Files.isRegularFile(entry)) {
                    try {
                        item.put("size", Files.size(entry));
                    } catch (IOException ignored) {
                    }
                }
                items.add(item);
            }
        }

        // 排序：目录在前，文件在后
        items.sort((a, b) -> {
            if (!a.get("type").equals(b.get("type"))) {
                return a.get("type").equals("dir") ? -1 : 1;
            }
            return ((String) a.get("name")).compareTo((String) b.get("name"));
        });

        for (Map<String, Object> item : items) {
            String type = (String) item.get("type");
            if ("dir".equals(type)) {
                sb.append("[DIR]  ").append(item.get("name")).append("\n");
            } else {
                sb.append("[FILE] ").append(item.get("name"));
                if (item.containsKey("size")) {
                    sb.append(" (").append(item.get("size")).append(" bytes)");
                }
                sb.append("\n");
            }
        }
    }

    private void listRecursive(Path dirPath, StringBuilder sb, int depth) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
            List<Path> entries = new ArrayList<>();
            for (Path entry : stream) {
                entries.add(entry);
            }

            entries.sort(Comparator.comparing((Path p) -> !Files.isDirectory(p))
                    .thenComparing(Path::getFileName));

            for (Path entry : entries) {
                String indent = "  ".repeat(depth);
                String name = entry.getFileName().toString();

                if (Files.isDirectory(entry)) {
                    sb.append(indent).append("[DIR]  ").append(name).append("/\n");
                    listRecursive(entry, sb, depth + 1);
                } else {
                    long size = Files.size(entry);
                    sb.append(indent).append("[FILE] ").append(name).append(" (").append(size).append(" bytes)\n");
                }
            }
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
