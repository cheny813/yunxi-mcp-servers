package io.yunxi.mcp.nutrition.sync;

import io.yunxi.mcp.nutrition.sync.config.SyncPipelineConfig;
import io.yunxi.mcp.nutrition.sync.config.SyncPipelineConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.util.List;

/**
 * 同步集合初始化器
 * <p>
 * 应用启动时，根据 sync-pipelines.yml 中定义的管道，确保对应的 Milvus 集合已存在。
 * 仅处理同步管道集合，不触碰框架自身的记忆集合（如 conversation_memory）。
 * 直接依赖 MilvusCollectionService（不再依赖 SyncEngine，防止 SyncEngine @PostConstruct
 * 异常导致初始化器连锁失败）。
 * </p>
 * <p>
 * HIGHEST_PRECEDENCE 确保在 SyncEngine.syncAll() 之前完成所有集合的 Schema 创建。
 * </p>
 *
 * @author yunxi-agent-platform
 * @version 2.2.0
 */
@Component
@ConditionalOnProperty(name = "milvus.enabled", havingValue = "true", matchIfMissing = true)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SyncCollectionInitializer {

    private static final Logger log = LoggerFactory.getLogger(SyncCollectionInitializer.class);

    @Autowired(required = false)
    private SyncPipelineConfigLoader pipelineConfigLoader;

    @Autowired
    private MilvusCollectionService collectionService;

    @PostConstruct
    public void init() {
        if (pipelineConfigLoader == null) {
            log.warn("SyncCollectionInitializer: SyncPipelineConfigLoader 不可用，跳过集合初始化");
            return;
        }
        List<SyncPipelineConfig> pipelines = pipelineConfigLoader.getPipelines();
        if (pipelines == null || pipelines.isEmpty()) {
            log.info("SyncCollectionInitializer: 无同步管道，跳过集合初始化");
            return;
        }
        for (SyncPipelineConfig cfg : pipelines) {
            if (!cfg.isEnabled()) {
                continue;
            }
            try {
                collectionService.ensureSyncCollection(cfg);
                log.info("同步集合已确保存在: {}", cfg.getTarget().getCollection());
            } catch (Exception e) {
                log.warn("确保同步集合失败: {}", cfg.getTarget().getCollection(), e);
            }
        }
    }
}
