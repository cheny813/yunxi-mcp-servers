package io.yunxi.mcp.qdrant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Qdrant 配置
 *
 * @author yunxi-mcp-servers
 * @version 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "qdrant")
public class QdrantConfig {

    /**
     * 是否启用 Qdrant
     */
    private boolean enabled = true;

    /**
     * Qdrant Host
     */
    private String host = "localhost";

    /**
     * Qdrant Port
     */
    private int port = 6333;

    /**
     * 是否使用 HTTPS
     */
    private boolean useTls = false;

    /**
     * API Key (可选)
     */
    private String apiKey;

    /**
     * 超时时间（秒）
     */
    private int timeout = 30;

    /**
     * 默认 Top K
     */
    private int defaultTopK = 10;

    /**
     * 默认相似度阈值
     */
    private double defaultScoreThreshold = 0.7;
}