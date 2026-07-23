package io.yunxi.mcp.common.embedding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 向量嵌入路由服务
 * <p>
 * 自动发现所有 {@link EmbeddingProvider} Bean，按 providerName 路由请求。
 * 内置 LRU 缓存（MAX_CACHE_SIZE=10000）减少重复调用。
 * 同时提供 {@link #cosineSimilarity(List, List)} 工具方法。
 * </p>
 *
 * @author yunxi-mcp-servers
 * @since 2.0.0
 */
@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);
    private static final int MAX_CACHE_SIZE = 10000;

    @Value("${embedding.provider:dashscope}")
    private String defaultProviderName;

    @Value("${embedding.default-dimension:1024}")
    private int defaultDimension;

    private final Map<String, EmbeddingProvider> providers = new ConcurrentHashMap<>();
    private final Map<String, List<Float>> textEmbeddingCache = Collections.synchronizedMap(
            new LinkedHashMap<>() {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, List<Float>> eldest) {
                    return size() > MAX_CACHE_SIZE;
                }
            });

    /**
     * 通过 setter 注入所有 EmbeddingProvider Bean（{@code required = false}，允许无 Provider 的场景）。
     */
    @Autowired(required = false)
    public void setProviders(List<EmbeddingProvider> providerList) {
        log.info("EmbeddingService 初始化: 发现 {} 个 Provider", providerList.size());
        for (EmbeddingProvider provider : providerList) {
            String name = provider.getProviderName();
            if (name != null && !name.isBlank()) {
                providers.put(name.toLowerCase(Locale.ROOT), provider);
                log.info("  - {} (model={}, dim={})", name, provider.getModelName(), provider.getDimension());
            } else {
                log.warn("  - 跳过未命名的 Provider: {}", provider.getClass().getName());
            }
        }
        log.info("EmbeddingService 就绪，可用 providers: {}", providers.keySet());
    }

    /**
     * 按名称查找 Provider，未指定或未找到时回退到默认 Provider。
     *
     * @param providerName 提供者名称（可为空）
     * @return 匹配的 Provider
     */
    public EmbeddingProvider getProvider(String providerName) {
        if (providerName == null || providerName.isBlank()) {
            return resolveDefaultProvider();
        }
        EmbeddingProvider p = providers.get(providerName.toLowerCase(Locale.ROOT));
        return p != null ? p : resolveDefaultProvider();
    }

    private EmbeddingProvider resolveDefaultProvider() {
        EmbeddingProvider p = providers.get(defaultProviderName.toLowerCase(Locale.ROOT));
        if (p == null && !providers.isEmpty()) {
            p = providers.values().iterator().next();
        }
        if (p == null) {
            throw new IllegalStateException("没有可用的 EmbeddingProvider，请检查 embedding.provider 和 Provider Bean 配置");
        }
        return p;
    }

    /** 使用默认 Provider 嵌入单条文本（含缓存） */
    public List<Float> embed(String text) {
        return embed(text, defaultProviderName);
    }

    /** 使用指定 Provider 嵌入单条文本（含缓存） */
    public List<Float> embed(String text, String providerName) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String cacheKey = (providerName != null ? providerName : defaultProviderName) + ":" + text;
        List<Float> cached = textEmbeddingCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        try {
            EmbeddingProvider provider = getProvider(providerName);
            List<Float> result = provider.embed(text);
            textEmbeddingCache.put(cacheKey, result);
            return result;
        } catch (Exception e) {
            log.error("嵌入失败 (provider={}, text={}...)", providerName,
                    text.substring(0, Math.min(50, text.length())), e);
            return Collections.emptyList();
        }
    }

    /** 使用默认 Provider 批量嵌入 */
    public List<List<Float>> embedBatch(List<String> texts) {
        return embedBatch(texts, defaultProviderName);
    }

    /** 使用指定 Provider 批量嵌入 */
    public List<List<Float>> embedBatch(List<String> texts, String providerName) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            EmbeddingProvider provider = getProvider(providerName);
            return provider.embedBatch(texts);
        } catch (Exception e) {
            log.error("批量嵌入失败 (provider={}, count={})", providerName, texts.size(), e);
            return Collections.emptyList();
        }
    }

    /** 返回默认 Provider 的向量维度 */
    public int getDimension() {
        try {
            return resolveDefaultProvider().getDimension();
        } catch (Exception e) {
            log.warn("无法获取默认 Provider 的维度，回退到配置值 {}", defaultDimension);
            return defaultDimension;
        }
    }

    /** 返回指定 Provider 的向量维度 */
    public int getDimension(String providerName) {
        try {
            return getProvider(providerName).getDimension();
        } catch (Exception e) {
            log.warn("无法获取 Provider '{}' 的维度，回退到配置值 {}", providerName, defaultDimension);
            return defaultDimension;
        }
    }

    /** 返回默认 Provider 的名称 */
    public String getProviderName() {
        return resolveDefaultProvider().getProviderName();
    }

    /** 返回所有已注册 Provider 的名称列表 */
    public List<String> getAvailableProviders() {
        return new ArrayList<>(providers.keySet());
    }

    /**
     * 计算两个向量的余弦相似度
     *
     * @param vectorA 向量 A
     * @param vectorB 向量 B
     * @return 余弦相似度 [-1, 1]，输入非法时返回 0.0
     */
    public static double cosineSimilarity(List<Float> vectorA, List<Float> vectorB) {
        if (vectorA == null || vectorB == null || vectorA.size() != vectorB.size()) {
            return 0.0;
        }
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vectorA.size(); i++) {
            dotProduct += vectorA.get(i) * vectorB.get(i);
            normA += vectorA.get(i) * vectorA.get(i);
            normB += vectorB.get(i) * vectorB.get(i);
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /** 清空嵌入缓存 */
    public void clearCache() {
        textEmbeddingCache.clear();
        log.info("嵌入缓存已清空");
    }
}
