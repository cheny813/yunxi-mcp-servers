package io.yunxi.mcp.database;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 数据库 MCP 服务器应用程序入口（HTTP 模式）
 * <p>
 * 提供数据库查询和管理操作的 MCP 服务器支持。
 * </p>
 *
 * <h3>使用方式</h3>
 * 
 * <pre>
 * java -jar mcp-database-http.jar --server.port=8081 \
 *   --spring.datasource.url=\${DATABASE_URL:jdbc:mysql://localhost:3306/test} \
 *   --spring.datasource.username=\${DATABASE_USERNAME:root} \
 *   --spring.datasource.password={DATABASE_PASSWORD:root}
 * </pre>
 */
@SpringBootApplication
public class DatabaseMcpHttpApplication {

    /**
     * 应用程序主入口
     * <p>
     * 启动 Spring Boot 应用程序，初始化数据库 MCP 服务器。
     * </p>
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(DatabaseMcpHttpApplication.class, args);
    }
}
