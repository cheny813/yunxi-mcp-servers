package io.yunxi.mcp.memory.embedding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文本嵌入向量服务
 * <p>
 * 负责将文本转换为高维向量，支持多种嵌入服务提供商（Provider）。
 * 主要功能包括：
 * </p>
 * <ul>
 *   <li>文本向量化 - 将用户输入的文本转换为固定维度的浮点向量</li>
 *   <li>批量向量化 - 支持一次性处理多条文本</li>
 *   <li>向量相似度计算 - 支持余弦相似度计算，用于语义检索</li>
 *   <li>嵌入缓存 - 避免重复向量化相同文本，提升性能</li>
 * </ul>
 * <p>
 * 支持的 Provider：
 * </p>
 * <ul>
 *   <li>DashScope - 阿里云文本嵌入服务（默认）</li>
 *   <li>OpenAI - OpenAI 文本嵌入服务（占位实现）</li>
 *   <li>Mock - 用于测试的模拟 Provider</li>
 * </ul>
 *
 * @author yunxi-mcp-servers
 */
@Slf4j
@Service
public class EmbeddingService {

    /**
     * 嵌入服务提供商标识
     * 可通过配置项 embedding.provider 指定，默认为 dashscope
     */
    @Value("${embedding.provider:dashscope}")
    private String provider;

    /**
     * 默认向量维度
     * 当没有可用的 Provider 时使用
     */
    @Value("${embedding.default-dimension:1024}")
    private int defaultDimension;

    /**
     * 注入所有可用的 EmbeddingProvider 实现
     * Spring 会自动将所有实现该接口的 Bean 注入到这个 Map 中
     * Key 为 Bean 名称（如 "dashScopeEmbeddingProvider"）
     */
    @Autowired(required = false)
    private Map<String, EmbeddingProvider> embeddingProviders;

    /**
     * 当前选定的 Provider
     */
    private EmbeddingProvider currentProvider;

    /**
     * 嵌入向量缓存
     * 使用 ConcurrentHashMap 保证线程安全
     * Key 格式：textHash_length_providerName
     */
    private final Map<String, List<Float>> embeddingCache = new ConcurrentHashMap<>();
    /** 缓存最大条目数，避免内存溢出 */
    private static final int MAX_CACHE_SIZE = 10000;

    /**
     * 初始化方法
     * 在 Bean 构造完成后执行，根据配置选择合适的 Provider
     */
    @PostConstruct
    public void init() {
        if (embeddingProviders != null && embeddingProviders.containsKey(provider)) {
            currentProvider = embeddingProviders.get(provider);
            log.info("使用嵌入向量 Provider: {} (维度: {})",
                    currentProvider.getName(), currentProvider.getDimension());
        } else {
            if (embeddingProviders != null && !embeddingProviders.isEmpty()) {
                currentProvider = embeddingProviders.values().iterator().next();
                log.warn("Provider '{}' 不存在，使用默认 Provider: {}",
                        provider, currentProvider.getName());
            } else {
                log.warn("没有可用的嵌入向量 Provider，使用默认维度: {}", defaultDimension);
            }
        }
    }

    /**
     * 获取当前 Provider 的向量维度
     *
     * @return 向量维度，如果 Provider 未初始化则返回默认值
     */
    public int getDimension() {
        return currentProvider != null ? currentProvider.getDimension() : defaultDimension;
    }

    /**
     * 获取当前 Provider 的名称
     *
     * @return Provider 名称，未初始化则返回 "Unknown"
     */
    public String getProviderName() {
        return currentProvider != null ? currentProvider.getName() : "Unknown";
    }

    /**
     * 将单个文本转换为向量
     * <p>
     * 处理流程：
     * </p>
     * <ol>
     *   <li>空值检查 - 返回空列表</li>
     *   <li>长度截断 - 超过 8000 字符的文本会被截断</li>
     *   <li>缓存检查 - 避免重复计算</li>
     *   <li>向量化调用 - 调用 Provider 的 embed 方法</li>
     *   <li>结果缓存 - 将结果存入缓存</li>
     * </ol>
     *
     * @param text 输入文本
     * @return 浮点向量列表，失败时返回空列表
     */
    public List<Float> embed(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        text = text.trim();
        // 文本过长时进行截断，避免超过 Provider 的限制
        if (text.length() > 8000) {
            text = text.substring(0, 8000);
        }

        // 检查缓存
        String cacheKey = text.hashCode() + "_" + text.length() + "_" + getProviderName();
        if (embeddingCache.containsKey(cacheKey)) {
            return embeddingCache.get(cacheKey);
        }

        if (currentProvider == null) {
            log.error("嵌入向量 Provider 未初始化");
            return Collections.emptyList();
        }

        try {
            List<Float> embedding = currentProvider.embed(text);

            // 缓存有效结果
            if (embedding != null && !embedding.isEmpty()) {
                if (embeddingCache.size() < MAX_CACHE_SIZE) {
                    embeddingCache.put(cacheKey, embedding);
                }
            }

            return embedding;

        } catch (Exception e) {
            log.error("获取文本向量失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 批量将多个文本转换为向量
     * <p>
     * 内部逐条调用 embed 方法，适合少量文本的批量处理。
     * 对于大量文本，建议在 Provider 层面实现批量优化。
     * </p>
     *
     * @param texts 文本列表
     * @return 向量列表（与输入文本一一对应）
     */
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

    /**
     * 计算两个向量的余弦相似度
     * <p>
     * 余弦相似度衡量两个向量方向的相似程度，取值范围 [-1, 1]。
     * 值为 1 表示完全相似，值为 0 表示正交（无相关性），值为 -1 表示完全相反。
     * </p>
     *
     * @param v1 第一个向量
     * @param v2 第二个向量
     * @return 余弦相似度，向量为空或维度不匹配时返回 0
     */
    public double cosineSimilarity(List<Float> v1, List<Float> v2) {
        if (v1 == null || v2 == null || v1.size() != v2.size()) {
            return 0;
        }

        double dotProduct = 0;
        double norm1 = 0;
        double norm2 = 0;

        // 计算点积和各向量的范数
        for (int i = 0; i < v1.size(); i++) {
            dotProduct += v1.get(i) * v2.get(i);
            norm1 += v1.get(i) * v1.get(i);
            norm2 += v2.get(i) * v2.get(i);
        }

        // 避免除零错误
        if (norm1 == 0 || norm2 == 0) {
            return 0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * 清除嵌入缓存
     * <p>
     * 在内存紧张或需要重新加载配置时调用。
     * </p>
     */
    public void clearCache() {
        embeddingCache.clear();
        log.info("Embedding 缓存已清除");
    }
}
