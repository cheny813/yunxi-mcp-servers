package io.yunxi.mcp.filesystem;

import io.yunxi.mcp.common.controller.AbstractMcpController;
import io.yunxi.mcp.filesystem.tool.ReadFileTool;
import io.yunxi.mcp.filesystem.tool.WriteFileTool;
import io.yunxi.mcp.filesystem.tool.ListDirectoryTool;
import io.yunxi.mcp.filesystem.tool.SearchFilesTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * MCP 文件系统控制器
 * <p>
 * 继承 {@link AbstractMcpController}，提供文件系统操作的 MCP 端点。
 * 统一由基类提供 HTTP/SSE 端点实现，本类仅负责工具注册。
 * </p>
 *
 * <h3>提供的工具</h3>
 * <ul>
 * <li>read_file - 读取文件内容</li>
 * <li>write_file - 写入文件内容</li>
 * <li>list_directory - 列出目录内容</li>
 * <li>search_files - 搜索文件</li>
 * </ul>
 */
@Slf4j
@RestController
public class McpController extends AbstractMcpController {

    @Value("${mcp.allowed-directory:/}")
    private String allowedDirectory;

    @Override
    protected String getServerName() {
        return "yunxi-mcp-filesystem";
    }

    @Override
    protected void registerTools() {
        Path baseDir = Paths.get(allowedDirectory).toAbsolutePath().normalize();
        log.info("允许访问的目录: {}", baseDir);

        // 注册文件系统操作工具
        registerTool(new ReadFileTool(baseDir));
        registerTool(new WriteFileTool(baseDir));
        registerTool(new ListDirectoryTool(baseDir));
        registerTool(new SearchFilesTool(baseDir));

        log.info("MCP Filesystem 工具注册完成，可用工具: {}",
                httpEndpoint.getTools().stream().map(tool -> tool.getName()).toList());
    }
}
