package io.yunxi.mcp.text2sql.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.yunxi.mcp.text2sql.config.LlmProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容的 LLM 客户端（/v1/chat/completions）。
 * <p>与 Focus_MCP_SQL 不同，本服务不依赖 DataFocus 云，可指向任意 OpenAI 兼容端点
 * （默认 DashScope 兼容模式）。</p>
 */
@Service
public class LlmClient {

    private static final Logger log = LoggerFactory.getLogger(LlmClient.class);

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final Duration timeout;

    public LlmClient(LlmProperties props) {
        this.baseUrl = props.getBaseUrl();
        this.apiKey = props.getApiKey() == null ? "" : props.getApiKey();
        this.model = props.getModel();
        this.timeout = Duration.ofSeconds(props.getTimeoutSeconds());
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public String chat(String systemPrompt, String userPrompt) {
        try {
            Map<String, Object> req = Map.of(
                    "model", model,
                    "temperature", 0.0,
                    "response_format", Map.of("type", "json_object"),
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    )
            );
            String body = mapper.writeValueAsString(req);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(stripTrailingSlash(baseUrl) + "/chat/completions"))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> resp =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new RuntimeException("LLM API 返回 " + resp.statusCode() + ": " + resp.body());
            }
            JsonNode root = mapper.readTree(resp.body());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode()) {
                throw new RuntimeException("LLM 响应缺少 choices[0].message.content: " + resp.body());
            }
            return content.asText();
        } catch (Exception e) {
            throw new RuntimeException("LLM 调用失败: " + e.getMessage(), e);
        }
    }

    private static String stripTrailingSlash(String s) {
        if (s != null && s.endsWith("/")) {
            return s.substring(0, s.length() - 1);
        }
        return s;
    }
}
