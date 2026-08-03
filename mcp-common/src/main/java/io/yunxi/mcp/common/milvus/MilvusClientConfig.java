package io.yunxi.mcp.common.milvus;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Milvus 客户端自动配置（全平台唯一实现）。
 * <p>
 * 公共模块默认 <b>不</b> 创建 {@link MilvusClientV2} Bean（{@code milvus.enabled} 缺省视为 false）。
 * 只有显式声明 {@code milvus.enabled=true} 的模块才会装配本客户端，避免每个引入
 * mcp-common 的服务在启动期无差别地去连 Milvus。
 * </p>
 * <p>
 * 客户端构建策略：
 * <ul>
 *   <li>采用懒校验：Bean 创建本身不强制连通，连接失败仅告警不阻断应用启动；</li>
 *   <li>携带连接/查询超时，统一来自 {@link MilvusConfig}；</li>
 *   <li>Token 优先于 username/password 的认证方式；</li>
 *   <li>下游模块一律通过 {@code ObjectProvider<MilvusClientV2>} 或
 *       {@code @Autowired(required = false)} 注入，对 {@code null} 做降级处理。</li>
 * </ul>
 * </p>
 *
 * @author yunxi-mcp-servers
 * @since 2.0.0
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(MilvusConfig.class)
@ConditionalOnProperty(name = "milvus.enabled", havingValue = "true", matchIfMissing = false)
public class MilvusClientConfig {

    /**
     * 创建全平台唯一的 Milvus 客户端 Bean。
     *
     * @param config Milvus 连接配置（来自 application.yml 的 milvus 段）
     * @return 已构建的 MilvusClientV2 实例（可能为未连通状态，调用方需容错）
     */
    @Bean
    public MilvusClientV2 milvusClient(MilvusConfig config) {
        ConnectConfig.ConnectConfigBuilder builder = ConnectConfig.builder()
                .uri(config.resolveUri())
                .connectTimeoutMs((long) config.getConnectTimeout() * 1000L)
                .rpcDeadlineMs((long) config.getQueryTimeout() * 1000L)
                .enablePrecheck(false);

        // 认证：token 优先，其次 username/password
        if (config.getToken() != null && !config.getToken().isBlank()) {
            builder.token(config.getToken());
        } else if (config.getUsername() != null && !config.getUsername().isBlank()) {
            builder.username(config.getUsername()).password(config.getPassword());
        }

        if (config.getDatabase() != null && !config.getDatabase().isBlank()
                && !"default".equals(config.getDatabase())) {
            builder.dbName(config.getDatabase());
        }

        MilvusClientV2 client = new MilvusClientV2(builder.build());
        log.info("Milvus 客户端已创建 (uri={}, database={}, connectTimeout={}s)",
                config.resolveUri(), config.getDatabase(), config.getConnectTimeout());
        return client;
    }
}
