package io.yunxi.mcp.github;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * GitHub MCP 服务器应用程序入口
 * <p>
 * 提供 GitHub API 访问的 MCP 服务器支持，包括仓库管理、Issue 和 PR 操作等。
 * </p>
 *
 * <h3>使用方式</h3>
 * 
 * <pre>
 * java -jar mcp-github.jar --server.port=8084 --github.token=your_token
 * </pre>
 */
@SpringBootApplication
public class GithubMcpApplication {

    /**
     * 应用程序主入口
     * <p>
     * 启动 Spring Boot 应用程序，初始化 GitHub MCP 服务器。
     * </p>
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(GithubMcpApplication.class, args);
    }
}
