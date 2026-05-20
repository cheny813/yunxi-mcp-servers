package io.yunxi.mcp.milvus.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Milvus 向量数据库配置属性
 *
 * <p>
 * 配置示例：
 *
     * <pre>
     * milvus:
     *   host: ${MILVUS_HOST:localhost}
     *   port: 19530
     *   connect-timeout: 5
     *   query-timeout: 30
     * </pre>
 * </p>
 *
 * @author yunxi
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "milvus")
public class MilvusConfig {

    /**
     * Milvus 服务地址
     * <p>
     * Milvus 服务器的主机名或 IP 地址。
     * </p>
     */
    private String host; // 服务器地址，优先使用环境变量 MILVUS_HOST 配置

    /**
     * Milvus 服务端口
     * <p>
     * Milvus 服务的端口号，默认值为 19530（Milvus 默认端口）。
     * </p>
     */
    private int port = 19530;

    /**
     * 数据库名称
     * <p>
     * 连接的 Milvus 数据库名称，默认为 "default"。
     * </p>
     */
    private String database = "default";

    /**
     * 用户名（可选，Milvus 认证启用时需要）
     * <p>
     * 用于用户名密码认证方式。
     * </p>
     */
    private String username;

    /**
     * 密码（可选，Milvus 认证启用时需要）
     * <p>
     * 用于用户名密码认证方式。
     * </p>
     */
    private String password;

    /**
     * Token（可选，替代用户名密码的认证方式，优先级更高）
     * <p>
     * 用于 Token 认证方式，优先级高于用户名密码认证。
     * </p>
     */
    private String token;

    /**
     * 连接超时（秒）
     * <p>
     * 建立连接的超时时间，默认 5 秒。
     * </p>
     */
    private int connectTimeout = 5;

    /**
     * 查询超时（秒）
     * <p>
     * 查询操作的超时时间，默认 30 秒。
     * </p>
     */
    private int queryTimeout = 30;
}
