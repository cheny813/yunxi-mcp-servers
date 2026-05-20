package io.yunxi.mcp.filesystem.tool;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * 搜索文件工具
 */
public class SearchFilesTool implements ToolHandler {

    private final Path baseDir;

    public SearchFilesTool(Path baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public ToolDefinition getDefinition() {
        return ToolDefinition.builder()
                .name("search_files")
                .description(
                        "Search for files by name pattern (supports * and ? wildcards). " +
                                "按名称模式搜索文件（支持 * 和 ? 通配符）。 " +
                                "Use this when you need to find specific files by name, locate source files, or discover resources. "
                                +
                                "适用于按名称查找特定文件、定位源文件或发现资源等场景。 " +
                                "Common use cases: finding source files, locating config files, discovering resources. "
                                +
                                "典型用例：查找源文件、定位配置文件、发现资源文件。")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "pattern", Map.of(
                                        "type", "string",
                                        "description",
                                        "File name pattern. Examples: '*.txt', '*.java', '*Test.java' | " +
                                                "文件名模式。示例: '*.txt', '*.java', '*Test.java'"),
                                "path", Map.of(
                                        "type", "string",
                                        "description",
                                        "Directory to search in (default: root). Example: 'src/main' | " +
                                                "搜索目录（默认：根目录）。示例: 'src/main'"),
                                "maxResults", Map.of(
                                        "type", "integer",
                                        "description", "Maximum number of results (default: 50). Example: 100 | " +
                                                "最大结果数量（默认：50）。示例: 100",
                                        "default", 50)),
                        "required", List.of("pattern")))
                .build();
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        String pattern = (String) arguments.get("pattern");
        String path = (String) arguments.get("path");
        int maxResults = arguments.containsKey("maxResults") ? ((Number) arguments.get("maxResults")).intValue() : 50;

        if (pattern == null || pattern.isBlank()) {
            return ToolResult.error("Search pattern is required");
        }

        try {
            Path searchPath = resolvePath(path != null ? path : "");

            if (!Files.exists(searchPath)) {
                return ToolResult.error("Search directory not found: " + path);
            }

            if (!Files.isDirectory(searchPath)) {
                return ToolResult.error("Search path is not a directory: " + path);
            }

            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
            List<String> results = new ArrayList<>();
            int[] count = { 0 };

            Files.walkFileTree(searchPath, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
                    new SimpleFileVisitor<>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            if (matcher.matches(file.getFileName())) {
                                String relativePath = baseDir.relativize(file).toString();
                                results.add(relativePath);
                                count[0]++;
                                if (count[0] >= maxResults) {
                                    return FileVisitResult.TERMINATE;
                                }
                            }
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                            // 跳过隐藏目录和常见的不需要搜索的目录
                            String name = dir.getFileName() != null ? dir.getFileName().toString() : "";
                            if (name.startsWith(".") || name.equals("node_modules") || name.equals("target")
                                    || name.equals("build")) {
                                return FileVisitResult.SKIP_SUBTREE;
                            }
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) {
                            return FileVisitResult.CONTINUE;
                        }
                    });

            if (results.isEmpty()) {
                return ToolResult.text("No files found matching pattern: " + pattern);
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Search results for '").append(pattern).append("':\n");
            sb.append("Found ").append(results.size()).append(" file(s)\n\n");

            for (String result : results) {
                sb.append("- ").append(result).append("\n");
            }

            if (count[0] >= maxResults) {
                sb.append("\n(maximum results reached)");
            }

            return ToolResult.text(sb.toString());
        } catch (IOException e) {
            return ToolResult.error("Error searching files: " + e.getMessage());
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
