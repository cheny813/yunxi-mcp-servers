package io.yunxi.mcp.common.embedding.provider;

import io.yunxi.mcp.common.embedding.EmbeddingProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Ollama 本地 Embedding Provider（连接池版，防止 Windows 临时端口耗尽）。
 * <p>
 * 无需 API Key，使用本地部署的 Ollama 服务生成嵌入向量。
 * 支持 bge-m3、bge-large-zh 等模型。
 * </p>
 * <p>
 * 使用 Apache HttpClient 5 连接池复用 TCP 连接，避免每次 HTTP 请求
 * 新建连接导致 Windows 临时端口耗尽（bind error / insufficient buffer space）。
 * 连接池默认：max 50 总连接，max 20 每路由。
 * </p>
 * <p>
 * 配置前缀：{@code embedding.ollama.*}
 *
 * @author yunxi
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "embedding.provider", havingValue = "ollama", matchIfMissing = false)
public class OllamaEmbeddingProvider implements EmbeddingProvider {

    private RestTemplate restTemplate;
    private PoolingHttpClientConnectionManager connectionManager;

    @Value("${embedding.ollama.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${embedding.ollama.model:bge-m3}")
    private String model;

    @Value("${embedding.ollama.dimension:1024}")
    private int dimension;

    @Value("${embedding.ollama.batch-size:50}")
    private int batchSize;

    @Value("${embedding.ollama.connect-timeout-sec:10}")
    private int connectTimeoutSec;

    @Value("${embedding.ollama.read-timeout-sec:120}")
    private int readTimeoutSec;

    @Value("${embedding.ollama.pool-max-total:50}")
    private int poolMaxTotal;

    @Value("${embedding.ollama.pool-max-per-route:20}")
    private int poolMaxPerRoute;

    @Override
    public String getName() {
        return "Ollama";
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

    /**
     * 初始化连接池 RestTemplate，防止大规模 embedding 时耗尽 Windows 临时端口。
     */
    @PostConstruct
    public void init() {
        connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(poolMaxTotal);
        connectionManager.setDefaultMaxPerRoute(poolMaxPerRoute);

        var httpClient = HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(org.apache.hc.client5.http.config.RequestConfig.custom()
                        .setConnectTimeout(Timeout.of(connectTimeoutSec, TimeUnit.SECONDS))
                        .setResponseTimeout(Timeout.of(readTimeoutSec, TimeUnit.SECONDS))
                        .build())
                .build();

        var factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        this.restTemplate = new RestTemplate(factory);

        log.info("[Ollama] initialized (pooled http): baseUrl={}, model={}, dimension={}, "
                        + "poolMaxTotal={}, poolMaxPerRoute={}, connectTimeout={}s, readTimeout={}s",
                baseUrl, model, dimension, poolMaxTotal, poolMaxPerRoute,
                connectTimeoutSec, readTimeoutSec);
    }

    @PreDestroy
    public void destroy() {
        if (connectionManager != null) {
            connectionManager.close();
        }
    }

    @Override
    public List<Float> embed(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }
        List<List<Float>> batchResult = embedBatch(Collections.singletonList(text));
        if (batchResult.isEmpty()) {
            return Collections.emptyList();
        }
        return batchResult.get(0);
    }

    @Override
    public List<List<Float>> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("input", texts);

            var response = restTemplate.postForEntity(
                    baseUrl + "/api/embed", requestBody, Map.class);

            if (response.getBody() != null) {
                @SuppressWarnings("unchecked")
                List<List<Double>> embeddings = (List<List<Double>>) response.getBody().get("embeddings");
                if (embeddings != null && !embeddings.isEmpty()) {
                    return embeddings.stream()
                            .map(inner -> inner.stream()
                                    .map(Double::floatValue)
                                    .collect(Collectors.toList()))
                            .collect(Collectors.toList());
                }
            }
        } catch (Exception e) {
            log.error("[Ollama] embedBatch failed for {} texts", texts.size(), e);
        }
        return Collections.emptyList();
    }
}
