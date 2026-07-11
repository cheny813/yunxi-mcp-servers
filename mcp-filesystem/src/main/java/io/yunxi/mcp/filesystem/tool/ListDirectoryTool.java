package io.yunxi.mcp.filesystem.tool;

import io.yunxi.mcp.common.handler.ToolHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class ListDirectoryTool implements ToolHandler {
    private final Path baseDir;

    public ListDirectoryTool(Path baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public String getName() { return "list_directory"; }

    @Override
    public String getDescription() { return "List directory contents. 列出目录内容。"; }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "path", Map.of("type", "string", "description", "Relative directory path (default: root)")),
                "required", List.of());
    }

    @Override
    public String execute(Map<String, Object> args) {
        String path = (String) args.getOrDefault("path", ".");
        try {
            Path dir = baseDir.resolve(path).normalize();
            if (!dir.startsWith(baseDir)) throw new SecurityException("Access denied");
            if (!Files.exists(dir)) throw new RuntimeException("Directory not found: " + path);
            if (!Files.isDirectory(dir)) throw new RuntimeException("Not a directory: " + path);

            StringBuilder sb = new StringBuilder("Directory: " + path + "\n\n");
            try (Stream<Path> stream = Files.list(dir)) {
                List<Path> entries = stream.sorted().collect(Collectors.toList());
                for (Path e : entries) {
                    String type = Files.isDirectory(e) ? "[DIR]" : "[FILE]";
                    sb.append("  ").append(type).append(" ").append(e.getFileName()).append("\n");
                }
            }
            return sb.toString();
        } catch (IOException e) {
            throw new RuntimeException("Error listing directory: " + e.getMessage());
        }
    }
}
