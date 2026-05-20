package io.yunxi.mcp.milvus.config;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Milvus 客户端持有者，支持自动重连。
 * <p>
 * 当 Milvus 服务不可达时（如本机休眠后网络断开），应用仍可正常启动，
 * 后台定时任务会自动尝试重连，恢复后自动可用。
 * </p>
 */
@Slf4j
@Component
public class MilvusClientHolder {

    private final MilvusConfig config;

    private volatile MilvusClientV2 client;

    @Autowired
    public MilvusClientHolder(MilvusConfig config) {
        this.config = config;
    }

    /**
     * 应用启动后首次尝试连接
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        tryConnect();
    }

/**
     * 每 30 秒检查一次，如果客户端不可用则尝试重连
     */
    @Scheduled(fixedDelay = 30_000)
    public void tryReconnect() {
        if (client == null || !isClientValid(client)) {
            log.info("Milvus 客户端不可用或连接已失效，准备重连...");
            tryConnect();
        }
    }

    /**
     * 检查客户端连接是否有效
     */
    private boolean isClientValid(MilvusClientV2 client) {
        try {
            // 尝试一个轻量级操作来验证连接
            client.listCollections();
            return true;
        } catch (Exception e) {
            log.warn("Milvus 连接有效性检查失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 尝试连接 Milvus 服务
     */
    private synchronized void tryConnect() {
        log.info("正在连接 Milvus: {}:{}", config.getHost(), config.getPort());

        try {
            // 先关闭旧连接（如果有）
            closeQuietly();

            long connectTimeoutMs = config.getConnectTimeout() * 1000L;
            long queryTimeoutMs = config.getQueryTimeout() * 1000L;

            ConnectConfig.ConnectConfigBuilder builder = ConnectConfig.builder()
                    .uri("http://" + config.getHost() + ":" + config.getPort())
                    .dbName(config.getDatabase())
                    .connectTimeoutMs(connectTimeoutMs)
                    .keepAliveTimeMs(60_000L)
                    .keepAliveTimeoutMs(20_000L)
                    .rpcDeadlineMs(queryTimeoutMs)
                    .idleTimeoutMs(24 * 3600_000L);

            if (config.getToken() != null && !config.getToken().isEmpty()) {
                log.info("Milvus 使用 Token 认证");
                builder.token(config.getToken());
            } else if (config.getUsername() != null && !config.getUsername().isEmpty()) {
                log.info("Milvus 使用用户名密码认证: {}", config.getUsername());
                builder.username(config.getUsername())
                        .password(config.getPassword() != null ? config.getPassword() : "");
            } else {
                log.info("Milvus 无认证模式");
            }

            ConnectConfig connectConfig = builder.build();
            this.client = new MilvusClientV2(connectConfig);
            log.info("Milvus 客户端连接成功: {}:{}", config.getHost(), config.getPort());

        } catch (Exception e) {
            log.warn("Milvus 连接失败（将在 30 秒后重试）: {}", e.getMessage());
            this.client = null;
        }
    }

    /**
     * 强制重连（连接断开后手动调用）
     */
    public synchronized void reconnect() {
        tryConnect();
    }

/**
     * 获取当前 Milvus 客户端，如果不可用则立即尝试重连
     */
    public MilvusClientV2 getClient() {
        if (client == null) {
            log.info("Milvus 客户端为空，尝试立即重连...");
            tryReconnect();
        }
        return client;
    }

    /**
     * 获取客户端（带重试机制）
     * @param retryIfFailed 如果获取失败是否触发重连
     */
    public MilvusClientV2 getClient(boolean retryIfFailed) {
        if (client == null && retryIfFailed) {
            log.info("Milvus 客户端不可用，触发重连...");
            tryReconnect();
        }
        return client;
    }

    /**
     * Milvus 是否可用
     */
    public boolean isAvailable() {
        return client != null;
    }

    @PreDestroy
    public void destroy() {
        closeQuietly();
    }

    private void closeQuietly() {
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                log.debug("关闭 Milvus 客户端时出错: {}", e.getMessage());
            }
            client = null;
        }
    }
}
