package io.yunxi.mcp.memory.embedding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * OpenAI 向量嵌入提供者（占位实现）
 * <p>
 * OpenAI 文本嵌入服务的实现类，目前仅为占位实现。
 * 预留接口以便未来接入 OpenAI 的嵌入服务。
 * </p>
 * <p>
 * 配置示例（application.yml）：
 * </p>
 * <pre>
 * embedding:
 *   provider: openai
 *   openai:
 *     api-key: your-api-key
 *     model: text-embedding-ada-002
 * </pre>
 * <p>
 * 注意事项：
 * </p>
 * <ul>
 *   <li>当前版本尚未实现实际功能，调用会返回空向量</li>
 *   <li>推荐使用 DashScope 作为替代方案</li>
 *   <li>向量维度为 1536（text-embedding-ada-002）</li>
 * </ul>
 * <p>
 * 激活条件：通过配置 {@code embedding.provider=openai} 启用。
 * </p>
 *
 * @author yunxi-mcp-servers
 * @see <a href="https://platform.openai.com/docs/guides/embeddings">OpenAI Embeddings</a>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "embedding.provider", havingValue = "openai")
public class OpenAIEmbeddingProvider implements EmbeddingProvider {

    /**
     * OpenAI API Key
     * 可通过环境变量 OPENAI_API_KEY 或配置项设置
     */
    @Value("${embedding.openai.api-key:}")
    private String apiKey;

    /**
     * 使用的嵌入模型
     * 默认为 text-embedding-ada-002，支持 1536 维向量输出
     */
    @Value("${embedding.openai.model:text-embedding-ada-002}")
    private String model;

    /** 向量维度：OpenAI text-embedding-ada-002 输出 1536 维 */
    private static final int DIMENSION = 1536;

    @Override
    public String getName() {
        return "OpenAI";
    }

    @Override
    public int getDimension() {
        return DIMENSION;
    }

    @Override
    public int getBatchSize() {
        return 2048;
    }

    /**
     * 将文本转换为向量
     * <p>
     * 占位实现，当前直接返回空列表并记录警告日志。
     * 未来可接入 OpenAI API 实现实际功能。
     * </p>
     *
     * @param text 输入文本
     * @return 空列表（占位实现）
     */
    @Override
    public List<Float> embed(String text) {
        log.warn("OpenAI Embedding Provider 尚未实现，请使用 DashScope");
        return Collections.emptyList();
    }

    /**
     * 批量将多个文本转换为向量
     * <p>
     * 占位实现，当前直接返回空列表。
     * </p>
     *
     * @param texts 文本列表
     * @return 空列表（占位实现）
     */
    @Override
    public List<List<Float>> embedBatch(List<String> texts) {
        log.warn("OpenAI Embedding Provider 尚未实现，请使用 DashScope");
        List<List<Float>> results = new ArrayList<>();
        for (String text : texts) {
            results.add(Collections.emptyList());
        }
        return results;
    }
}
