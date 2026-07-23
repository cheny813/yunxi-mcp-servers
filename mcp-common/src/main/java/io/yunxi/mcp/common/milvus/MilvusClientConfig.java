package io.yunxi.mcp.common.milvus;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Milvus 客户端自动配置（v2 SDK）
 * <p>
 * 仅在 {@code milvus.enabled=true}（默认为 true）时创建 {@link MilvusClientV2} Bean。
 * 关闭时向量功能优雅降级，下游模块通过 {@code @Autowired(required = false)} 或
 * {@code ObjectProvider} 处理 null 客户端。
 * </p>
 *
 * @author yunxi-mcp-servers
 * @since 2.0.0
 */
@Configuration
@EnableConfigurationProperties(MilvusConfig.class)
@ConditionalOnProperty(name = "milvus.enabled", havingValue = "true", matchIfMissing = true)
public class MilvusClientConfig {

    private static final Logger log = LoggerFactory.getLogger(MilvusClientConfig.class);

    /**
     * 创建 Milvus v2 客户端连接
     *
     * @param milvusConfig Milvus 配置
     * @return Milvus 客户端
     */
    @Bean
    public MilvusClientV2 milvusClient(MilvusConfig milvusConfig) {
        ConnectConfig.ConnectConfigBuilder builder = ConnectConfig.builder()
                .uri("http://" + milvusConfig.getHost() + ":" + milvusConfig.getPort());
        String token = milvusConfig.getToken();
        if (token != null && !token.isBlank()) {
            builder.token(token);
        }
        MilvusClientV2 client = new MilvusClientV2(builder.build());
        log.info("Milvus 客户端已创建: {}:{}", milvusConfig.getHost(), milvusConfig.getPort());
        return client;
    }
}
