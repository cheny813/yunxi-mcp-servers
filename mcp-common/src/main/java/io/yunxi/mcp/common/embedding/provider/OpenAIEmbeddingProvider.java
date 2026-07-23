package io.yunxi.mcp.common.embedding.provider;

import io.yunxi.mcp.common.embedding.EmbeddingProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.*;

/**
 * OpenAI 兼容 Embedding Provider。
 * <p>
 * 兼容任何 OpenAI API 格式的嵌入服务（含硅基流动、DeepSeek 等国产模型）。
 * <p>
 * 配置前缀：{@code embedding.openai.*}
 *
 * @author yunxi
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "embedding.provider", havingValue = "openai", matchIfMissing = false)
public class OpenAIEmbeddingProvider implements EmbeddingProvider {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${embedding.openai.api-key:}")
    private String apiKey;

    @Value("${embedding.openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${embedding.openai.model:text-embedding-3-small}")
    private String model;

    @Value("${embedding.openai.dimension:1536}")
    private int dimension;

    @Value("${embedding.openai.batch-size:50}")
    private int batchSize;

    @Override
    public String getName() {
        return "OpenAI";
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
        log.info("[OpenAI] initialized: baseUrl={}, model={}, dimension={}", baseUrl, model, dimension);
    }

    @Override
    public List<Float> embed(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("input", text);

            var headers = new org.springframework.http.HttpHeaders();
            headers.setBearerAuth(apiKey);
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

            var request = new org.springframework.http.HttpEntity<>(requestBody, headers);
            var response = restTemplate.postForEntity(
                    baseUrl + "/embeddings", request, Map.class);

            if (response.getBody() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
                if (data != null && !data.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    List<Double> embedding = (List<Double>) data.get(0).get("embedding");
                    if (embedding != null) {
                        List<Float> result = new ArrayList<>();
                        for (Double d : embedding) {
                            result.add(d.floatValue());
                        }
                        return result;
                    }
                }
            }
        } catch (Exception e) {
            log.error("[OpenAI] embed failed for text len={}", text.length(), e);
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
            for (int i = 0; i < texts.size(); i += batchSize) {
                int end = Math.min(i + batchSize, texts.size());
                List<String> batch = texts.subList(i, end);
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", model);
                requestBody.put("input", batch);

                var headers = new org.springframework.http.HttpHeaders();
                headers.setBearerAuth(apiKey);
                headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

                var request = new org.springframework.http.HttpEntity<>(requestBody, headers);
                var response = restTemplate.postForEntity(
                        baseUrl + "/embeddings", request, Map.class);

                if (response.getBody() != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
                    if (data != null) {
                        for (Map<String, Object> item : data) {
                            @SuppressWarnings("unchecked")
                            List<Double> embedding = (List<Double>) item.get("embedding");
                            if (embedding != null) {
                                List<Float> vec = new ArrayList<>();
                                for (Double d : embedding) {
                                    vec.add(d.floatValue());
                                }
                                results.add(vec);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("[OpenAI] embedBatch failed: size={}", texts.size(), e);
        }
        return results;
    }
}
