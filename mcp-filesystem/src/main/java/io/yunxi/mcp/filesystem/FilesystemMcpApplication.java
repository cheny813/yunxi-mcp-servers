package io.yunxi.mcp.filesystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 文件系统 MCP 服务器应用程序入口
 * <p>
 * 提供文件系统操作的 MCP 服务器支持，包括读取、写入、列出目录和搜索文件等。
 * </p>
 *
 * <h3>使用方式</h3>
 * 
 * <pre>
 * java -jar mcp-filesystem.jar --server.port=8082 \
 *   --mcp.allowed-directory=/path/to/allowed/directory
 * </pre>
 */
@SpringBootApplication
public class FilesystemMcpApplication {

    /**
     * 应用程序主入口
     * <p>
     * 启动 Spring Boot 应用程序，初始化文件系统 MCP 服务器。
     * </p>
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(FilesystemMcpApplication.class, args);
    }
}
