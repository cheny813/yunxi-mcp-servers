package io.yunxi.mcp.common.embedding.provider;

import io.yunxi.mcp.common.embedding.EmbeddingProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mock Embedding Provider（用于开发测试，无需外部 API）。
 * <p>
 * 使用 SHA-256 哈希生成确定性伪随机向量，同一文本始终返回相同向量。
 * <p>
 * 配置前缀：{@code embedding.mock.*}
 *
 * @author yunxi
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "embedding.provider", havingValue = "mock", matchIfMissing = false)
public class MockEmbeddingProvider implements EmbeddingProvider {

    @Value("${embedding.mock.dimension:384}")
    private int dimension;

    @Value("${embedding.mock.batch-size:50}")
    private int batchSize;

    @Override
    public String getName() {
        return "Mock";
    }

    @Override
    public String getProviderName() {
        return getName();
    }

    @Override
    public String getModelName() {
        return "mock-hash-" + dimension;
    }

    @Override
    public int getDimension() {
        return dimension;
    }

    @Override
    public int getBatchSize() {
        return batchSize;
    }

    @PostConstruct
    public void init() {
        log.info("[Mock] initialized: dimension={}, batchSize={}", dimension, batchSize);
    }

    @Override
    public List<Float> embed(String text) {
        return generateMockEmbedding(text);
    }

    @Override
    public List<List<Float>> embedBatch(List<String> texts) {
        List<List<Float>> results = new ArrayList<>();
        for (String text : texts) {
            results.add(generateMockEmbedding(text));
        }
        return results;
    }

    private List<Float> generateMockEmbedding(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes("UTF-8"));
            List<Float> embedding = new ArrayList<>(dimension);
            for (int i = 0; i < dimension; i++) {
                // 使用哈希字节作为随机种子，映射到 [-1, 1]
                int byteIndex = i % hash.length;
                float value = (float) ((hash[byteIndex] & 0xFF) / 128.0 - 1.0);
                embedding.add(value);
            }
            return embedding;
        } catch (Exception e) {
            log.error("[Mock] failed to generate embedding", e);
            return Collections.emptyList();
        }
    }
}
