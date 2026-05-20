package io.yunxi.mcp.memory.config;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Milvus 向量数据库配置
 * <p>
 * 自动配置 Milvus 客户端 Bean，建立与 Milvus 服务的连接。
 * 仅在配置 {@code milvus.enabled=true} 时激活。
 * </p>
 * <p>
 * 配置示例（application.yml）：
 * </p>
 * 
 * <pre>
 * milvus:
 *   enabled: true
 *   uri: http://localhost:19530
 *   token: your-token-if-needed
 *   collection-name: agent_memories
 * </pre>
 *
 * @author yunxi-mcp-servers
 * @see MilvusProperties
 * @see io.milvus.v2.client.MilvusClientV2
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "milvus.enabled", havingValue = "true")
public class MilvusConfig {

    /**
     * 创建 Milvus 配置属性 Bean
     *
     * @return Milvus 配置属性对象
     */
    @Bean
    @ConfigurationProperties(prefix = "milvus")
    public MilvusProperties milvusProperties() {
        return new MilvusProperties();
    }

    /**
     * 创建 Milvus 客户端 Bean
     * <p>
     * 根据配置创建 MilvusClientV2 实例，用于后续的向量操作。
     * 使用 DisposableBean 接口处理资源释放。
     * </p>
     *
     * @param properties Milvus 配置属性
     * @return Milvus 客户端实例
     */
    @Bean
    public MilvusClientV2 milvusClient(MilvusProperties properties) {
        log.info("初始化 Milvus 客户端: uri={}", properties.uri);

        // 添加连接超时配置
        ConnectConfig connectConfig = ConnectConfig.builder()
                .uri(properties.uri)
                .token(properties.token)
                .connectTimeoutMs(10_000)    // 10秒连接超时
                .keepAliveTimeMs(60_000)     // 60秒保活
                .keepAliveTimeoutMs(20_000)  // 20秒保活超时
                .idleTimeoutMs(300_000)      // 5分钟空闲超时
                .build();

        MilvusClientV2 client = new MilvusClientV2(connectConfig);

        // 验证连接
        try {
            client.listCollections();
            log.info("Milvus 客户端初始化成功，连接验证通过");
        } catch (Exception e) {
            log.warn("Milvus 连接验证失败: {}", e.getMessage());
        }

        return client;
    }
}
