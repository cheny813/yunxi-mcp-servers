package io.yunxi.mcp.nutrition.sync.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 同步管道配置加载器
 * <p>
 * 通过 {@code @ConfigurationProperties(prefix = "sync")} 从 Spring 环境中绑定
 * {@code sync.pipelines} 列表（来自导入的 sync-pipelines.yml）。
 * </p>
 *
 * @author yunxi-agent-platform
 * @version 2.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "sync")
public class SyncPipelineConfigLoader {

    @Value("${sync.pipelines-file:classpath:sync-pipelines.yml}")
    private String pipelinesFile;

    private List<SyncPipelineConfig> pipelines = new ArrayList<>();

    public List<SyncPipelineConfig> getPipelines() {
        return pipelines;
    }

    public void setPipelines(List<SyncPipelineConfig> pipelines) {
        this.pipelines = pipelines;
    }

    public String getPipelinesFile() {
        return pipelinesFile;
    }

    public void setPipelinesFile(String pipelinesFile) {
        this.pipelinesFile = pipelinesFile;
    }
}
