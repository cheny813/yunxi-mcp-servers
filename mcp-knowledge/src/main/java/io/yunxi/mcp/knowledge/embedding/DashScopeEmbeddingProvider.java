package io.yunxi.mcp.knowledge.embedding;

import com.alibaba.dashscope.embeddings.TextEmbedding;
import com.alibaba.dashscope.embeddings.TextEmbeddingParam;
import com.alibaba.dashscope.embeddings.TextEmbeddingResult;
import com.alibaba.dashscope.embeddings.TextEmbeddingResultItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * DashScope 文本嵌入提供者
 * 使用阿里云 DashScope 的 text-embedding-v3 模型将文本转换为 1024 维向量
 *
 * @author yunxi-mcp-servers
 */
@Slf4j
@Component
public class DashScopeEmbeddingProvider implements EmbeddingProvider {

    @Value("${dashscope.api-key:}")
    private String apiKey;

    @Value("${dashscope.model:text-embedding-v3}")
    private String model;

    /** 向量维度: DashScope text-embedding-v3 默认输出 1024 维 */
    private static final int DIMENSION = 1024;

    private TextEmbedding textEmbedding;

    @PostConstruct
    public void init() {
        textEmbedding = new TextEmbedding();
        log.info("DashScope Embedding Provider 初始化完成, model={}, dimension={}", model, DIMENSION);
    }

    /**
     * 获取提供者名称
     *
     * @return 提供者名称 "DashScope"
     */
    @Override
    public String getName() {
        return "DashScope";
    }

    /**
     * 获取向量维度
     *
     * @return 向量维度 1024
     */
    @Override
    public int getDimension() {
        return DIMENSION;
    }

    /**
     * 将文本转换为向量
     *
     * @param text 输入文本
     * @return 1024 维浮点向量列表，失败时返回空列表
     */
    @Override
    public List<Float> embed(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            TextEmbeddingParam param = TextEmbeddingParam.builder()
                    .apiKey(apiKey)
                    .model(model)
                    .texts(Collections.singletonList(text))
                    .build();

            TextEmbeddingResult result = textEmbedding.call(param);

            if (result != null && result.getOutput() != null && !result.getOutput().getEmbeddings().isEmpty()) {
                TextEmbeddingResultItem item = result.getOutput().getEmbeddings().get(0);
                List<Double> doubleEmbedding = item.getEmbedding();
                // 转换为 Float (Milvus 需要 Float 类型)
                List<Float> floatEmbedding = new ArrayList<>(doubleEmbedding.size());
                for (Double d : doubleEmbedding) {
                    floatEmbedding.add(d.floatValue());
                }
                return floatEmbedding;
            }
        } catch (Exception e) {
            log.error("DashScope 向量化失败: {}", e.getMessage(), e);
        }

        return Collections.emptyList();
    }

    /**
     * 获取批处理大小
     *
     * @return 批处理大小 25
     */
    @Override
    public int getBatchSize() {
        return 25;
    }

    /**
     * 批量将多个文本转换为向量
     *
     * @param texts 文本列表
     * @return 向量列表（与输入文本一一对应）
     */
    @Override
    public List<List<Float>> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        List<List<Float>> results = new ArrayList<>();
        for (String text : texts) {
            results.add(embed(text));
        }
        return results;
    }
}