package io.yunxi.mcp.memory.embedding;

import com.alibaba.dashscope.embeddings.TextEmbedding;
import com.alibaba.dashscope.embeddings.TextEmbeddingParam;
import com.alibaba.dashscope.embeddings.TextEmbeddingResult;
import com.alibaba.dashscope.embeddings.TextEmbeddingResultItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * DashScope 向量嵌入提供者
 * <p>
 * 阿里云文本嵌入服务（DashScope）的实现类。
 * 使用 DashScope 的 text-embedding-v3 模型将文本转换为 1024 维向量。
 * </p>
 * <p>
 * 配置示例（application.yml）：
 * </p>
 * <pre>
 * embedding:
 *   provider: dashscope
 *   dashscope:
 *     api-key: your-api-key
 *     model: text-embedding-v3
 * </pre>
 * <p>
 * 激活条件：通过配置 {@code embedding.provider=dashscope} 启用。
 * </p>
 *
 * @author yunxi-mcp-servers
 * @see <a href="https://dashscope.aliyuncs.com/">阿里云 DashScope</a>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "embedding.provider", havingValue = "dashscope")
public class DashScopeEmbeddingProvider implements EmbeddingProvider {

    /**
     * DashScope API Key
     * 可通过环境变量 DASHSCOPE_API_KEY 或配置项设置
     */
    @Value("${embedding.dashscope.api-key:}")
    private String apiKey;

    /**
     * 使用的嵌入模型
     * 默认为 text-embedding-v3，支持 1024 维向量输出
     */
    @Value("${embedding.dashscope.model:text-embedding-v3}")
    private String model;

    /** 向量维度：DashScope text-embedding-v3 默认输出 1024 维 */
    private static final int DIMENSION = 1024;

    /** DashScope TextEmbedding 客户端 */
    private TextEmbedding textEmbedding;

    /**
     * 初始化方法
     * 构造 TextEmbedding 客户端实例
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        textEmbedding = new TextEmbedding();
        log.info("DashScope Embedding Provider 初始化完成: model={}, dimension={}", model, DIMENSION);
    }

    @Override
    public String getName() {
        return "DashScope";
    }

    @Override
    public int getDimension() {
        return DIMENSION;
    }

    @Override
    public int getBatchSize() {
        return 25;
    }

    /**
     * 将文本转换为向量
     * <p>
     * 调用 DashScope API 进行文本向量化。
     * 处理流程：
     * </p>
     * <ol>
     *   <li>空值检查 - 空文本直接返回空列表</li>
     *   <li>构建请求参数 - 设置 API Key、模型和文本</li>
     *   <li>调用 API - 通过 TextEmbedding 获取向量结果</li>
     *   <li>类型转换 - 将 Double 类型的向量转换为 Float</li>
     * </ol>
     *
     * @param text 输入文本
     * @return 1024 维浮点向量，失败时返回空列表
     */
    @Override
    public List<Float> embed(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            // 构建请求参数
            TextEmbeddingParam param = TextEmbeddingParam.builder()
                    .apiKey(apiKey)
                    .model(model)
                    .texts(Collections.singletonList(text))
                    .build();

            // 调用 DashScope API
            TextEmbeddingResult result = textEmbedding.call(param);

            // 解析响应，提取向量
            if (result != null && result.getOutput() != null && !result.getOutput().getEmbeddings().isEmpty()) {
                TextEmbeddingResultItem item = result.getOutput().getEmbeddings().get(0);
                List<Double> doubleEmbedding = item.getEmbedding();
                // 转换 Double 为 Float（Milvus 需要 Float 类型）
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
     * 批量将多个文本转换为向量
     * <p>
     * 内部逐条调用 embed 方法实现。
     * DashScope API 支持批量请求，但 SDK 层面暂未封装，
     * 未来可考虑优化为真正的批量请求以提升性能。
     * </p>
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
