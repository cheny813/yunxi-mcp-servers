package io.yunxi.mcp.filesystem.tool;

import io.yunxi.mcp.common.handler.ToolHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

@Component
public class WriteFileTool implements ToolHandler {
    private final Path baseDir;

    public WriteFileTool(Path baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public String getName() { return "write_file"; }

    @Override
    public String getDescription() { return "Write content to a file. 写入文件内容。"; }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "path", Map.of("type", "string", "description", "Relative file path"),
                        "content", Map.of("type", "string", "description", "Content to write"),
                        "append", Map.of("type", "boolean", "description", "Append mode (default: false)")),
                "required", List.of("path", "content"));
    }

    @Override
    public String execute(Map<String, Object> args) {
        String path = (String) args.get("path");
        String content = (String) args.get("content");
        boolean append = Boolean.TRUE.equals(args.get("append"));
        if (path == null || path.isBlank()) throw new RuntimeException("path is required");
        if (content == null) throw new RuntimeException("content is required");
        try {
            Path fp = resolvePath(path);
            Files.createDirectories(fp.getParent());
            if (append) {
                Files.writeString(fp, content, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } else {
                Files.writeString(fp, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
            return "Written to " + path + " (" + content.length() + " chars)";
        } catch (IOException e) {
            throw new RuntimeException("Error writing file: " + e.getMessage());
        }
    }

    protected Path resolvePath(String path) {
        Path r = baseDir.resolve(path).normalize();
        if (!r.startsWith(baseDir)) throw new SecurityException("Access denied: path outside allowed directory");
        return r;
    }
}
