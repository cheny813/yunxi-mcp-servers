package io.yunxi.mcp.common.embedding.provider;

import com.alibaba.dashscope.embeddings.TextEmbedding;
import com.alibaba.dashscope.embeddings.TextEmbeddingParam;
import com.alibaba.dashscope.embeddings.TextEmbeddingResult;
import com.alibaba.dashscope.embeddings.TextEmbeddingResultItem;
import io.yunxi.mcp.common.embedding.EmbeddingProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 阿里云 DashScope Embedding Provider（统一实现）。
 * <p>
 * 由 mcp-common 统一管理，所有依赖模块（knowledge / memory / milvus / nutrition）
 * 通过嵌入 Provider SPI 自动获得该能力。
 * <p>
 * 配置前缀：{@code embedding.dashscope.*}
 *
 * @author yunxi
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "embedding.provider", havingValue = "dashscope", matchIfMissing = false)
public class DashScopeEmbeddingProvider implements EmbeddingProvider {

    @Value("${embedding.dashscope.api-key:}")
    private String apiKey;

    @Value("${embedding.dashscope.model:text-embedding-v3}")
    private String model;

    @Value("${embedding.dashscope.dimension:1024}")
    private int dimension;

    @Value("${embedding.dashscope.batch-size:50}")
    private int batchSize;

    @Override
    public String getName() {
        return "DashScope";
    }

    @Override
    public String getProviderName() {
        return getName();
    }

    @Override
    public String getModelName() {
        return model;
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
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("[DashScope] API Key is empty, embedding may not work. Set embedding.dashscope.api-key");
        } else {
            log.info("[DashScope] initialized: model={}, dimension={}, batchSize={}", model, dimension, batchSize);
        }
    }

    @Override
    public List<Float> embed(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            TextEmbeddingParam param = TextEmbeddingParam.builder()
                    .apiKey(apiKey)
                    .model(model)
                    .texts(Collections.singletonList(text))
                    .build();
            TextEmbeddingResult result = new TextEmbedding().call(param);
            if (result != null && result.getOutput() != null
                    && result.getOutput().getEmbeddings() != null
                    && !result.getOutput().getEmbeddings().isEmpty()) {
                TextEmbeddingResultItem item = result.getOutput().getEmbeddings().get(0);
                return item.getEmbedding() != null
                        ? item.getEmbedding().stream().map(Double::floatValue).collect(Collectors.toList())
                        : Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("[DashScope] embed failed for text len={}", text.length(), e);
        }
        return Collections.emptyList();
    }

    @Override
    public List<List<Float>> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }
        List<List<Float>> results = new ArrayList<>();
        try {
            // DashScope 支持批量文本
            for (int i = 0; i < texts.size(); i += batchSize) {
                int end = Math.min(i + batchSize, texts.size());
                List<String> batch = texts.subList(i, end);
                TextEmbeddingParam param = TextEmbeddingParam.builder()
                        .apiKey(apiKey)
                        .model(model)
                        .texts(batch)
                        .build();
                TextEmbeddingResult result = new TextEmbedding().call(param);
                if (result != null && result.getOutput() != null
                        && result.getOutput().getEmbeddings() != null) {
                    for (TextEmbeddingResultItem item : result.getOutput().getEmbeddings()) {
                        results.add(item.getEmbedding() != null
                                ? item.getEmbedding().stream().map(Double::floatValue).collect(Collectors.toList())
                                : Collections.emptyList());
                    }
                }
            }
        } catch (Exception e) {
            log.error("[DashScope] embedBatch failed: size={}", texts.size(), e);
            // 部分失败时返回已成功的结果
        }
        return results;
    }
}
