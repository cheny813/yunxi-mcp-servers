package io.yunxi.mcp.redis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MCP Redis 应用入口
 * <p>
 * 基于 Spring Boot 的 MCP Redis 服务器，提供 HTTP 和 SSE 两种调用模式，
 * 用于操作 Redis 数据库。
 * </p>
 *
 * <h3>使用说明</h3>
 * 
 * <pre>
 * # 启动服务（使用默认配置）
 * java -Dfile.encoding=UTF-8 -jar mcp-redis-1.0.0.jar --server.port=8083
 *
 * # 或指定 Redis 连接配置
 * java -Dfile.encoding=UTF-8 -jar mcp-redis-1.0.0.jar --server.port=8083 \
 *   --spring.data.redis.host=localhost \
 *   --spring.data.redis.port=6379
 * </pre>
 *
 * <h3>提供的工具</h3>
 * <ul>
 * <li>redis_get - 获取键值</li>
 * <li>redis_set - 设置键值（支持 TTL）</li>
 * <li>redis_delete - 删除键</li>
 * <li>redis_keys - 查找键</li>
 * <li>redis_list_ops - List 操作（lpush, rpush, lrange, lpop, rpop）</li>
 * <li>redis_hash_ops - Hash 操作（hset, hget, hgetall, hdel）</li>
 * </ul>
 */
@SpringBootApplication
public class RedisMcpApplication {

    /**
     * 主入口
     * <p>
     * 启动 Spring Boot 应用程序，初始化 Redis MCP 服务器。
     * </p>
     *
     * @param args 命令行参数（用于配置 Spring Boot 应用）
     */
    public static void main(String[] args) {
        SpringApplication.run(RedisMcpApplication.class, args);
    }
}
