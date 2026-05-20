package io.yunxi.mcp.wikipedia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MCP Wikipedia 应用入口
 * <p>
 * 基于 Spring Boot 的 MCP Wikipedia 服务器，提供 HTTP 和 SSE 两种调用模式，
 * 用于搜索和获取 Wikipedia 文章摘要。
 * </p>
 *
 * <h3>使用说明</h3>
 * <pre>
 * # 启动服务
 * java -jar mcp-wikipedia-1.0.0.jar
 * </pre>
 *
 * <h3>提供的工具</h3>
 * <ul>
 *   <li>wikipedia_search - 搜索 Wikipedia 文章</li>
 *   <li>wikipedia_summary - 获取 Wikipedia 文章摘要</li>
 * </ul>
 */
@SpringBootApplication
public class WikipediaMcpApplication {
    public static void main(String[] args) {
        SpringApplication.run(WikipediaMcpApplication.class, args);
    }
}
