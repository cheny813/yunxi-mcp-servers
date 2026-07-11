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
public class SearchFilesTool implements ToolHandler {
    private final Path baseDir;

    public SearchFilesTool(Path baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public String getName() { return "search_files"; }

    @Override
    public String getDescription() { return "Search files by pattern. 按模式搜索文件。"; }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "pattern", Map.of("type", "string", "description", "File pattern like '*.java', '*.yml'"),
                        "path", Map.of("type", "string", "description", "Search root path (default: root)")),
                "required", List.of("pattern"));
    }

    @Override
    public String execute(Map<String, Object> args) {
        String pattern = (String) args.get("pattern");
        String path = (String) args.getOrDefault("path", ".");
        if (pattern == null) throw new RuntimeException("pattern is required");
        try {
            Path dir = baseDir.resolve(path).normalize();
            if (!dir.startsWith(baseDir)) throw new SecurityException("Access denied");

            StringBuilder sb = new StringBuilder("Search: " + pattern + "\n\n");
            try (Stream<Path> stream = Files.walk(dir)) {
                List<Path> matches = stream
                        .filter(Files::isRegularFile)
                        .filter(p -> {
                            String name = p.getFileName().toString();
                            // Simple glob: * -> .*, ? -> .
                            String regex = pattern
                                    .replace(".", "\\.")
                                    .replace("*", ".*")
                                    .replace("?", ".");
                            return name.matches(regex);
                        })
                        .limit(100)
                        .collect(Collectors.toList());
                for (Path m : matches) {
                    sb.append("  ").append(dir.relativize(m)).append("\n");
                }
                sb.append("\nFound ").append(matches.size()).append(" file(s)");
            }
            return sb.toString();
        } catch (IOException e) {
            throw new RuntimeException("Error searching files: " + e.getMessage());
        }
    }
}
