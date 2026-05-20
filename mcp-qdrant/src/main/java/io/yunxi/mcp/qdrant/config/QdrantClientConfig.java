package io.yunxi.mcp.qdrant.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.yunxi.mcp.qdrant.QdrantConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Qdrant 客户端配置
 */
@Configuration
public class QdrantClientConfig {

    private final QdrantConfig config;

    public QdrantClientConfig(QdrantConfig config) {
        this.config = config;
    }

    @Bean
    public QdrantClient qdrantClient() {
        // 创建 QdrantGrpcClient
        QdrantGrpcClient grpcClient = QdrantGrpcClient.newBuilder(config.getHost(), config.getPort())
                .build();

        // 创建 QdrantClient
        return new QdrantClient(grpcClient);
    }
}
