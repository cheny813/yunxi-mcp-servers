package io.yunxi.mcp.elasticsearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Elasticsearch MCP 服务器应用程序入口
 * <p>
 * 提供 Elasticsearch 数据库操作的 MCP 服务器支持。
 * </p>
 *
 * <h3>使用方式</h3>
 * <pre>
 * java -jar mcp-elasticsearch.jar --server.port=8085 \
 *   --elasticsearch.host=localhost \
 *   --elasticsearch.port=9200
 * </pre>
 */
@SpringBootApplication
public class ElasticsearchMcpApplication {

    /**
     * 应用程序主入口
     * <p>
     * 启动 Spring Boot 应用程序，初始化 Elasticsearch MCP 服务器。
     * </p>
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ElasticsearchMcpApplication.class, args);
    }
}
