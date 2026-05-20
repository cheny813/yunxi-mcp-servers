package io.yunxi.mcp.mongodb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MCP MongoDB 应用入口
 * <p>
 * 基于 Spring Boot 的 MCP MongoDB 服务器，提供 HTTP 和 SSE 两种调用模式，
 * 用于操作 MongoDB 数据库。
 * </p>
 *
 * <h3>使用说明</h3>
 * <pre>
 * # 启动服务（使用默认配置）
 * java -jar mcp-mongodb-1.0.0.jar
 *
 * # 或指定 MongoDB 连接字符串
 * java -jar mcp-mongodb-1.0.0.jar \
 *   --mongodb.connection-string=mongodb://localhost:27017
 * </pre>
 *
 * <h3>提供的工具</h3>
 * <ul>
 *   <li>mongodb_list_databases - 列出所有数据库</li>
 *   <li>mongodb_list_collections - 列出数据库中的集合</li>
 *   <li>mongodb_find - 查询文档</li>
 *   <li>mongodb_insert - 插入文档</li>
 *   <li>mongodb_delete - 删除文档</li>
 * </ul>
 */
@SpringBootApplication
public class MongoMcpApplication {
    public static void main(String[] args) {
        SpringApplication.run(MongoMcpApplication.class, args);
    }
}
