package io.yunxi.mcp.knowledge.embedding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文本嵌入向量服务
 * 负责将文本转换为高维向量
 *
 * @author yunxi-mcp-servers
 */
@Slf4j
@Service
public class EmbeddingService {

    @Autowired
    private DashScopeEmbeddingProvider dashScopeProvider;

    private EmbeddingProvider currentProvider;

    /** 嵌入向量缓存 */
    private final Map<String, List<Float>> embeddingCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 10000;

    @PostConstruct
    public void init() {
        currentProvider = dashScopeProvider;
        log.info("EmbeddingService 初始化完成, Provider: {}", currentProvider.getName());
    }

    /**
     * 获取向量维度
     *
     * @return 向量维度
     */
    public int getDimension() {
        return currentProvider != null ? currentProvider.getDimension() : 1024;
    }

    /**
     * 将文本转换为向量
     *
     * @param text 输入文本
     * @return 浮点向量列表
     */
    public List<Float> embed(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        text = text.trim();
        if (text.length() > 8000) {
            text = text.substring(0, 8000);
        }

        // 检查缓存
        String cacheKey = text.hashCode() + "_" + text.length();
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
     * 清除缓存
     */
    public void clearCache() {
        embeddingCache.clear();
        log.info("Embedding 缓存已清除");
    }
}