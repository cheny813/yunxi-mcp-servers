package io.yunxi.mcp.milvus.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Milvus 客户端配置
 * <p>
 * 实际的 Milvus 客户端生命周期由 {@link MilvusClientHolder} 管理，
 * 支持启动时连接失败不阻塞应用、后台自动重连。
 * </p>
 */
@Slf4j
@Configuration
@EnableScheduling
public class MilvusClientConfig {
}
