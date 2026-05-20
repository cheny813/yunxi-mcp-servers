package io.yunxi.mcp.milvus.embedding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

/**
 * 嵌入向量服务
 * 
 * <p>
 * 支持多种嵌入模型提供商：
 * <ul>
 * <li>阿里云 DashScope API (text-embedding-v3)</li>
 * <li>本地 Ollama 服务 (bge-m3)</li>
 * </ul>
 * </p>
 * <p>
 * 包含向量缓存机制，支持降级处理（当 API 调用失败时使用模拟向量）。
 * </p>
 *
 * @author yunxi
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
public class EmbeddingService {

    /**
     * 嵌入模型提供商
     * <p>
     * 可选值：dashscope（默认）、ollama
     * </p>
     */
    @Value("${embedding.provider:dashscope}")
    private String provider;

    /**
     * DashScope API 密钥
     */
    @Value("${embedding.dashscope.api-key:}")
    private String dashscopeApiKey;

    /**
     * DashScope 模型名称
     */
    @Value("${embedding.dashscope.model:text-embedding-v3}")
    private String dashscopeModel;

    /**
     * Ollama 服务基础 URL
     */
    @Value("${embedding.ollama.base-url:}")
    private String ollamaBaseUrl;

    /**
     * Ollama 模型名称
     */
    @Value("${embedding.ollama.model:bge-m3}")
    private String ollamaModel;

    /**
     * 向量维度
     */
    @Value("${embedding.dashscope.dimension:1024}")
    private int dimension;

    /**
     * 是否启用嵌入服务
     * <p>
     * 设为 false 时，所有请求将返回模拟向量（用于测试或降级）。
     * </p>
     */
    @Value("${embedding.enabled:true}")
    private boolean enabled;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 向量缓存
    /**
     * 向量缓存
     * <p>
     * 使用文本的 hashCode + 长度作为缓存键，减少重复计算。
     * </p>
     */
    private final Map<String, List<Float>> cache = new HashMap<>();
    private static final int MAX_CACHE_SIZE = 1000;

    /**
     * 获取向量维度
     */
    public int getDimension() {
        return dimension;
    }

    /**
     * 将文本转换为向量
     */
    public List<Float> embed(String text) {
        if (text == null || text.trim().isEmpty()) {
            return generateZeroVector();
        }

        text = text.trim();
        if (text.length() > 8000) {
            text = text.substring(0, 8000);
        }

        // 检查缓存
        String cacheKey = text.hashCode() + "_" + text.length();
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }

        List<Float> embedding;

        if (!enabled) {
            log.warn("Embedding 服务未启用，使用模拟向量");
            embedding = generateMockEmbedding(text);
        } else if ("ollama".equalsIgnoreCase(provider)) {
            embedding = callOllamaApi(text);
        } else {
            embedding = callDashScopeApi(text);
        }

        // 缓存结果
        if (embedding != null && !embedding.isEmpty() && cache.size() < MAX_CACHE_SIZE) {
            cache.put(cacheKey, embedding);
        }

        return embedding != null ? embedding : generateZeroVector();
    }

    /**
     * 调用 Ollama Embedding API
     */
    private List<Float> callOllamaApi(String text) {
        try {
            String url = ollamaBaseUrl + "/api/embeddings";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", ollamaModel);
            requestBody.put("prompt", text);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);

            log.debug("调用 Ollama Embedding API: model={}", ollamaModel);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());

                if (root.has("error")) {
                    log.error("Ollama API 错误: {}", root.path("error").asText());
                    return generateMockEmbedding(text);
                }

                JsonNode embeddingNode = root.path("embedding");
                if (embeddingNode.isArray()) {
                    List<Float> vector = new ArrayList<>();
                    for (JsonNode node : embeddingNode) {
                        vector.add((float) node.asDouble());
                    }
                    log.debug("成功获取向量，维度: {}", vector.size());
                    return vector;
                }
            }

            log.warn("Ollama Embedding API 响应异常: {}", response.getBody());
            return generateMockEmbedding(text);

        } catch (Exception e) {
            log.error("调用 Ollama Embedding API 失败: {}", e.getMessage());
            return generateMockEmbedding(text);
        }
    }

    /**
     * 调用 DashScope Embedding API
     */
    private List<Float> callDashScopeApi(String text) {
        try {
            if (dashscopeApiKey == null || dashscopeApiKey.isEmpty()) {
                log.warn("DashScope API Key 未配置，使用模拟向量");
                return generateMockEmbedding(text);
            }

            String url = "https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", dashscopeModel);

            Map<String, Object> input = new HashMap<>();
            input.put("texts", Collections.singletonList(text));
            requestBody.put("input", input);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("text_type", "query");
            parameters.put("dimension", dimension);
            requestBody.put("parameters", parameters);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + dashscopeApiKey);

            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);

            log.debug("调用 DashScope Embedding API: model={}, dimension={}", dashscopeModel, dimension);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());

                if (root.has("code") && !"success".equals(root.path("code").asText())) {
                    log.error("DashScope API 错误: {} - {}", root.path("code").asText(), root.path("message").asText());
                    return generateMockEmbedding(text);
                }

                JsonNode embeddings = root.path("output").path("embeddings");
                if (embeddings.isArray() && embeddings.size() > 0) {
                    JsonNode vectorNode = embeddings.get(0).path("embedding");
                    List<Float> vector = new ArrayList<>();
                    for (JsonNode node : vectorNode) {
                        vector.add((float) node.asDouble());
                    }
                    log.debug("成功获取向量，维度: {}", vector.size());
                    return vector;
                }
            }

            log.warn("DashScope Embedding API 响应异常: {}", response.getBody());
            return generateMockEmbedding(text);

        } catch (Exception e) {
            log.error("调用 DashScope Embedding API 失败: {}", e.getMessage());
            return generateMockEmbedding(text);
        }
    }

    /**
     * 生成模拟向量（基于文本hash，仅用于降级）
     */
    private List<Float> generateMockEmbedding(String text) {
        List<Float> vector = new ArrayList<>();
        Random random = new Random(text.hashCode());

        for (int i = 0; i < dimension; i++) {
            vector.add(random.nextFloat() * 2 - 1);
        }

        // 归一化
        float norm = 0;
        for (float v : vector) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);

        List<Float> normalized = new ArrayList<>();
        for (float v : vector) {
            normalized.add(v / norm);
        }

        return normalized;
    }

    /**
     * 生成零向量
     */
    private List<Float> generateZeroVector() {
        List<Float> vector = new ArrayList<>();
        for (int i = 0; i < dimension; i++) {
            vector.add(0.0f);
        }
        return vector;
    }

    /**
     * 清除缓存
     */
    public void clearCache() {
        cache.clear();
        log.info("Embedding 缓存已清除");
    }
}
