package io.yunxi.mcp.common.milvus;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;


/**
 * Milvus 客户端自动配置（全平台唯一实现）。
 * <p>
 * 公共模块默认 <b>不</b> 创建 {@link MilvusClientV2} Bean（{@code milvus.enabled} 缺省视为 false）。
 * 只有显式声明 {@code milvus.enabled=true} 的模块才会装配本客户端，避免每个引入
 * mcp-common 的服务在启动期无差别地去连 Milvus。
 * </p>
 * <p>
 * 客户端构建策略（容错优先，确保 Milvus 不可达时应用仍可启动）：
 * <ul>
 *   <li>构造前先做 TCP 连通性预检，不可达则直接跳过客户端创建，避免 SDK 阻塞整应用；</li>
 *   <li>使用 try/catch 包裹客户端构造（该 SDK 在 {@code new MilvusClientV2} 时即阻塞连接，
 *       不可达会抛 {@code DEADLINE_EXCEEDED}），失败仅告警并返回 {@code null}；</li>
 *   <li>Bean 创建本身不强制连通，连接失败仅告警不阻断应用启动（与注释语义一致）；</li>
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

    /** 启动期连通性预检超时（毫秒），避免 Milvus 不可达时 SDK 构造阻塞整应用 */
    private static final int PRECHECK_TIMEOUT_MS = 1500;

    /**
     * 创建全平台唯一的 Milvus 客户端 Bean。
     * <p>Milvus 不可达或构造失败时返回 {@code null}，由下游 {@code ObjectProvider.getIfAvailable()}
     * 取到 null 实现向量功能降级，而非让整个 Spring 上下文初始化失败。</p>
     *
     * @param config Milvus 连接配置（来自 application.yml 的 milvus 段）
     * @return 已构建的 MilvusClientV2 实例；不可用时为 {@code null}
     */
    @Bean
    public MilvusClientV2 milvusClient(MilvusConfig config) {
        if (!isReachable(config)) {
            log.warn("Milvus 不可达 ({}:{}), 向量功能降级, 跳过客户端创建",
                    config.getHost(), config.getPort());
            return null;
        }
        try {
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
        } catch (Exception e) {
            // 该 SDK 在 new MilvusClientV2 时即阻塞连接，不可达会抛 DEADLINE_EXCEEDED。
            // 捕获并返回 null，使下游 getIfAvailable() 拿到 null，向量功能降级而非整应用崩溃。
            log.warn("Milvus 客户端创建失败, 向量功能降级: {}", e.getMessage());
            return null;
        }
    }

    /**
     * TCP 连通性预检：在构造重型 gRPC 客户端前，快速判断 Milvus 是否可达。
     * 不可达则直接降级，避免 SDK 构造时阻塞 {@code connectTimeout} 秒并抛异常。
     *
     * @param config Milvus 连接配置
     * @return 端口可连通时为 true
     */
    private boolean isReachable(MilvusConfig config) {
        String host = config.getHost();
        int port = config.getPort();
        String uri = config.getUri();
        if (uri != null && !uri.isBlank()) {
            try {
                URI u = URI.create(uri);
                if (u.getHost() != null) host = u.getHost();
                if (u.getPort() != -1) port = u.getPort();
            } catch (Exception ignore) {
                // URI 解析失败，回退使用 host/port
            }
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), PRECHECK_TIMEOUT_MS);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
