package io.yunxi.mcp.qdrant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Qdrant 向量数据库 MCP 服务器应用程序入口
 * <p>
 * 提供 Qdrant 向量数据库的 MCP 服务器支持。
 * 支持向量搜索、集合管理、点数据操作等。
 * </p>
 *
 * <h3>使用方式</h3>
 *
 * <pre>
 * java -jar mcp-qdrant.jar --server.port=6334 \
 *   --qdrant.host=localhost --qdrant.port=6333
 * </pre>
 *
 * @author yunxi-mcp-servers
 * @version 1.0.0
 */
@SpringBootApplication
public class QdrantMcpApplication {
    public static void main(String[] args) {
        SpringApplication.run(QdrantMcpApplication.class, args);
    }
}