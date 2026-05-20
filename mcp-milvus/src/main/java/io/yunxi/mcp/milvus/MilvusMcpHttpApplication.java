package io.yunxi.mcp.milvus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Milvus 向量搜索 MCP 服务器应用程序入口（HTTP 模式）
 * <p>
 * 提供 Milvus 向量数据库搜索操作的 MCP 服务器支持。
 * </p>
 *
 * <h3>使用方式</h3>
 *
 * <pre>
 * java -jar mcp-milvus-http.jar --server.port=8098 \
 *   --milvus.host=localhost \
 *   --milvus.port=19530
 * </pre>
 */
@SpringBootApplication
public class MilvusMcpHttpApplication {

    /**
     * 应用程序主入口
     * <p>
     * 启动 Spring Boot 应用程序，初始化 Milvus 向量搜索 MCP 服务器。
     * </p>
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(MilvusMcpHttpApplication.class, args);
    }
}
