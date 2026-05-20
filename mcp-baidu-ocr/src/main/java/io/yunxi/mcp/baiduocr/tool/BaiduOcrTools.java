package io.yunxi.mcp.baiduocr.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.yunxi.mcp.baiduocr.BaiduOcrConfig;
import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 百度 OCR 工具
 * <p>
 * 提供百度文字识别 API 的 MCP 工具实现。
 * 支持通用文字识别、身份证识别、银行卡识别等。
 * </p>
 *
 * <h3>提供的工具</h3>
 * <ul>
 * <li>{@link GeneralOcrTool} - 通用文字识别</li>
 * <li>{@link AccurateOcrTool} - 高精度文字识别</li>
 * </ul>
 *
 * <h3>配置</h3>
 * <p>
 * 需要配置 BAIDU_OCR_API_KEY 和 BAIDU_OCR_SECRET_KEY 环境变量
 * </p>
 */
@Slf4j
@Component
public class BaiduOcrTools {

    private final BaiduOcrConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private String accessToken;
    private long tokenExpireTime;

    @Autowired
    public BaiduOcrTools(BaiduOcrConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.getTimeout()))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 获取 access token
     */
    private String getAccessToken() {
        // 检查缓存的 token 是否有效
        if (accessToken != null && System.currentTimeMillis() < tokenExpireTime) {
            return accessToken;
        }

        try {
            String apiKey = getApiKey();
            String secretKey = getSecretKey();

            String url = config.getTokenEndpoint() + "?grant_type=client_credentials" +
                    "&client_id=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8) +
                    "&client_secret=" + URLEncoder.encode(secretKey, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                accessToken = root.get("access_token").asText();
                int expiresIn = root.get("expires_in").asInt();
                // 提前 5 分钟过期
                tokenExpireTime = System.currentTimeMillis() + (expiresIn - 300) * 1000L;
                return accessToken;
            } else {
                log.error("Failed to get access token: {}", response.body());
                return null;
            }
        } catch (Exception e) {
            log.error("Error getting access token: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取 API Key
     */
    private String getApiKey() {
        String apiKey = config.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = System.getenv("BAIDU_OCR_API_KEY");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("BAIDU_OCR_API_KEY not configured");
        }
        return apiKey;
    }

    /**
     * 获取 Secret Key
     */
    private String getSecretKey() {
        String secretKey = config.getSecretKey();
        if (secretKey == null || secretKey.isBlank()) {
            secretKey = System.getenv("BAIDU_OCR_SECRET_KEY");
        }
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("BAIDU_OCR_SECRET_KEY not configured");
        }
        return secretKey;
    }

    /**
     * 通用文字识别工具
     */
    public static class GeneralOcrTool implements ToolHandler {
        private final BaiduOcrTools parent;

        @Autowired
        public GeneralOcrTool(BaiduOcrTools parent) {
            this.parent = parent;
        }

        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("baidu_ocr_general")
                    .description(
                            "通用文字识别 (General OCR) - 识别图片中的文字内容。" +
                                    "Supports Chinese, English, numbers, and symbols. " +
                                    "典型用例：截图识别、文档扫描、名片识别、票据识别。")
                    .inputSchema(BaiduOcrTools.schema(
                            "image", "string", "Base64 encoded image data or image URL. 图片的 Base64 编码或图片URL。支持 JPEG PNG BMP 格式。",
                            "language_type", "auto_detect", "Language: CHN_ENG(中英文), ENG(英文), JPN(日语), KOR(韩语). 语言类型，默认为CHN_ENG。")
                    )
                    .build();
        }

        @Override
        public ToolResult execute(Map<String, Object> args) {
            String image = (String) args.get("image");
            String languageType = (String) args.getOrDefault("language_type", "CHN_ENG");

            if (image == null || image.isBlank()) {
                return ToolResult.error("Error: image is required (Base64 or URL)");
            }

            // 判断是 URL 还是 Base64
            boolean isUrl = image.startsWith("http://") || image.startsWith("https://");
            String imageParam = isUrl ? image : Base64.getEncoder().encodeToString(image.getBytes(StandardCharsets.UTF_8));

            return parent.doOcr("general_basic", imageParam, isUrl, languageType);
        }
    }

    /**
     * 高精度文字识别工具
     */
    public static class AccurateOcrTool implements ToolHandler {
        private final BaiduOcrTools parent;

        @Autowired
        public AccurateOcrTool(BaiduOcrTools parent) {
            this.parent = parent;
        }

        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("baidu_ocr_accurate")
                    .description(
                            "高精度文字识别 (Accurate OCR) - 更准确的文字识别，但速度较慢。" +
                                    "Higher accuracy but slower. 适用于对识别准确率要求较高的场景。 " +
                                    "典型用例：合同扫描、证照识别、重要文档数字化。")
                    .inputSchema(BaiduOcrTools.schema(
                            "image", "string", "Base64 encoded image data or image URL. 图片的 Base64 编码或图片URL。",
                            "language_type", "auto_detect", "Language: CHN_ENG(中英文), ENG(英文). 语言类型。")
                    )
                    .build();
        }

        @Override
        public ToolResult execute(Map<String, Object> args) {
            String image = (String) args.get("image");
            String languageType = (String) args.getOrDefault("language_type", "CHN_ENG");

            if (image == null || image.isBlank()) {
                return ToolResult.error("Error: image is required (Base64 or URL)");
            }

            boolean isUrl = image.startsWith("http://") || image.startsWith("https://");
            String imageParam = isUrl ? image : Base64.getEncoder().encodeToString(image.getBytes(StandardCharsets.UTF_8));

            return parent.doOcr("accurate_basic", imageParam, isUrl, languageType);
        }
    }

    /**
     * 执行 OCR 识别
     */
    private ToolResult doOcr(String apiType, String imageParam, boolean isUrl, String languageType) {
        try {
            String token = getAccessToken();
            if (token == null) {
                return ToolResult.error("Error: Failed to get access token");
            }

            String url = config.getEndpoint() + "?access_token=" + token;

            Map<String, String> requestBody = new HashMap<>();
            if (isUrl) {
                requestBody.put("url", imageParam);
            } else {
                requestBody.put("image", imageParam);
            }
            requestBody.put("language_type", languageType);
            requestBody.put("detect_direction", "true");
            requestBody.put("paragraph", "true");
            requestBody.put("paragraph", "true");

            String requestJson = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .timeout(Duration.ofSeconds(config.getTimeout()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("OCR API error: status={}, body={}", response.statusCode(), response.body());
                return ToolResult.error("Error: API request failed with status " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());

            // 检查错误
            if (root.has("error_code")) {
                String errorMsg = root.has("error_msg") ? root.get("error_msg").asText() : "Unknown error";
                return ToolResult.error("Error: " + errorMsg);
            }

            // 解析结果
            JsonNode wordsResult = root.get("words_result");
            if (wordsResult == null || wordsResult.isEmpty()) {
                return ToolResult.text("No text found in the image.");
            }

            StringBuilder sb = new StringBuilder();
            sb.append("📝 OCR 识别结果 (共 ").append(wordsResult.size()).append(" 行):\n\n");

            for (JsonNode wordNode : wordsResult) {
                String text = wordNode.get("words").asText();
                sb.append("• ").append(text).append("\n");
            }

            // 如果有位置信息，也一并输出
            if (root.has("paragraphs")) {
                sb.append("\n📑 段落识别结果:\n");
                JsonNode paragraphs = root.get("paragraphs");
                for (JsonNode para : paragraphs) {
                    String paraText = para.get("words").asText();
                    sb.append("  ").append(paraText).append("\n");
                }
            }

            return ToolResult.text(sb.toString());

        } catch (Exception e) {
            log.error("OCR error: {}", e.getMessage(), e);
            return ToolResult.error("Error: " + e.getMessage());
        }
    }

    /**
     * 构建输入参数 Schema
     */
    private static Map<String, Object> schema(String... props) {
        Map<String, Object> s = new HashMap<>();
        s.put("type", "object");
        Map<String, Object> properties = new HashMap<>();
        for (int i = 0; i < props.length; i += 3) {
            Map<String, Object> p = new HashMap<>();
            p.put("type", props[i + 1]);
            p.put("description", props[i + 2]);
            properties.put(props[i], p);
        }
        s.put("properties", properties);
        return s;
    }
}