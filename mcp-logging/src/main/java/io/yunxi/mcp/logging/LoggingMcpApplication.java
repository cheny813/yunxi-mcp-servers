package io.yunxi.mcp.logging;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 日志查询 MCP 服务器应用程序入口
 * <p>
 * 提供日志文件查询和分析的 MCP 服务器支持。
 * 支持按关键词、日志级别、时间范围搜索日志内容，
 * 用于诊断系统问题、查看错误信息、追踪业务流程。
 * </p>
 *
 * <h3>功能特性</h3>
 * <ul>
 * <li>日志文件列表查询 - 列出可查询的日志文件</li>
 * <li>日志内容搜索 - 支持关键词、级别、时间范围过滤</li>
 * <li>多格式支持 - 支持常见的日志文件格式</li>
 * </ul>
 *
 * <h3>使用方式</h3>
 *
 * <pre>
 * java -jar mcp-logging-1.0.0.jar --server.port=40105 \
 *   --logging.file.path=/var/log/app
 * </pre>
 *
 * <h3>提供工具</h3>
 * <ul>
 * <li>list_log_files - 列出日志文件</li>
 * <li>query_logs - 查询日志内容</li>
 * </ul>
 *
 * @author yunxi-mcp-servers
 * @version 1.0.0
 * @since 2026-04-13
 */
@SpringBootApplication
public class LoggingMcpApplication {

    /**
     * 应用程序主入口
     * <p>
     * 启动 Spring Boot 应用程序，初始化日志查询 MCP 服务器。
     * </p>
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(LoggingMcpApplication.class, args);
    }
}
