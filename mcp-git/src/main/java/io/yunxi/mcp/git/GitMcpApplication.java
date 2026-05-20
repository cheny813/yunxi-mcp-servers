package io.yunxi.mcp.git;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MCP Git 应用入口
 * <p>
 * 基于 Spring Boot 的 MCP Git 服务器，提供 HTTP 和 SSE 两种调用模式。
 * </p>
 *
 * <h3>使用说明</h3>
 * <pre>
 * # 启动服务
 * java -jar mcp-git-1.0.0.jar
 *
 * # 或指定 Git 仓库路径
 * java -jar mcp-git-1.0.0.jar --git.repo-path=/path/to/repo
 * </pre>
 *
 * <h3>提供的工具</h3>
 * <ul>
 *   <li>git_status - 获取 Git 仓库状态</li>
 *   <li>git_log - 获取 Git 提交日志</li>
 *   <li>git_branches - 获取分支列表</li>
 * </ul>
 */
@SpringBootApplication
public class GitMcpApplication {
    public static void main(String[] args) {
        SpringApplication.run(GitMcpApplication.class, args);
    }
}
