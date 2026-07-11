package io.yunxi.mcp.filesystem.tool;

import io.yunxi.mcp.common.handler.ToolHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Component
public class ReadFileTool implements ToolHandler {
    private final Path baseDir;

    public ReadFileTool(Path baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public String getName() { return "read_file"; }

    @Override
    public String getDescription() {
        return "Read file contents. 读取文件内容。";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "path", Map.of("type", "string", "description", "Relative file path"),
                        "lines", Map.of("type", "integer", "description", "Max lines to read (optional)")),
                "required", List.of("path"));
    }

    @Override
    public String execute(Map<String, Object> args) {
        String path = (String) args.get("path");
        Integer lines = args.containsKey("lines") ? ((Number) args.get("lines")).intValue() : null;
        if (path == null || path.isBlank()) throw new RuntimeException("path is required");
        try {
            Path fp = resolvePath(path);
            if (!Files.exists(fp)) throw new RuntimeException("File not found: " + path);
            if (Files.isDirectory(fp)) throw new RuntimeException("Path is a directory: " + path);

            String content;
            if (lines != null && lines > 0) {
                content = String.join("\n", Files.readAllLines(fp).subList(0, Math.min(lines, (int) Files.lines(fp).count())));
            } else {
                content = Files.readString(fp);
            }
            return "File: " + path + "\n\n" + content;
        } catch (IOException e) {
            throw new RuntimeException("Error reading file: " + e.getMessage());
        }
    }

    protected Path resolvePath(String path) {
        Path r = baseDir.resolve(path).normalize();
        if (!r.startsWith(baseDir)) throw new SecurityException("Access denied: path outside allowed directory");
        return r;
    }
}
