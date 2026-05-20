package io.yunxi.mcp.baiduasr.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.yunxi.mcp.baiduasr.BaiduAsrConfig;
import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
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

/**
 * 百度语音识别工具
 * <p>
 * 提供百度语音识别 API 的 MCP 工具实现。
 * 支持短语音识别、输入法模式等。
 * </p>
 *
 * <h3>提供的工具</h3>
 * <ul>
 * <li>{@link SpeechRecognitionTool} - 语音转文字</li>
 * </ul>
 *
 * <h3>配置</h3>
 * <p>
 * 需要配置 BAIDU_ASR_API_KEY 和 BAIDU_ASR_SECRET_KEY 环境变量
 * </p>
 */
@Slf4j
@Component
public class BaiduAsrTools {

    private final BaiduAsrConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private String accessToken;
    private long tokenExpireTime;

    // 支持的识别模型
    public static final Map<String, String> DEV_PID_MAP = Map.of(
            "1537", "普通话 (Search)",
            "1737", "英语",
            "1637", "粤语",
            "1739", "四川话",
            "1638", "河南话",
            "1662", "情感合成",
            "1900", "输入法模型"
    );

    @Autowired
    public BaiduAsrTools(BaiduAsrConfig config) {
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

    private String getApiKey() {
        String apiKey = config.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = System.getenv("BAIDU_ASR_API_KEY");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("BAIDU_ASR_API_KEY not configured");
        }
        return apiKey;
    }

    private String getSecretKey() {
        String secretKey = config.getSecretKey();
        if (secretKey == null || secretKey.isBlank()) {
            secretKey = System.getenv("BAIDU_ASR_SECRET_KEY");
        }
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("BAIDU_ASR_SECRET_KEY not configured");
        }
        return secretKey;
    }

    /**
     * 语音识别工具
     */
    @Component
    public static class SpeechRecognitionTool implements ToolHandler {
        private final BaiduAsrTools parent;

        @Autowired
        public SpeechRecognitionTool(BaiduAsrTools parent) {
            this.parent = parent;
        }

        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("baidu_asr")
                    .description(
                            "百度语音识别 (ASR) - 将音频转换为文字。" +
                                    "Convert audio to text. 支持 PCM WAV AMR 格式。 " +
                                    "典型用例：语音转文字、音频文件转写、会议录音转写。")
                    .inputSchema(BaiduAsrTools.schema(
                            "audio", "string", "Base64 encoded audio data. 音频数据 (PCM/WAV/AMR)，Base64 编码。",
                            "format", "string", "Audio format: pcm, wav, amr. 音频格式，默认 pcm。",
                            "rate", "integer", "Sample rate: 8000 or 16000. 采样率，默认 16000。",
                            "dev_pid", "string", "Model: 1537(普通话), 1737(英语), 1637(粤语), 1739(四川话). 识别模型，默认 1537。")
                    )
                    .build();
        }

        @Override
        public ToolResult execute(Map<String, Object> args) {
            String audio = (String) args.get("audio");
            String format = (String) args.getOrDefault("format", "pcm");
            Integer rate = args.containsKey("rate") ? ((Number) args.get("rate")).intValue() : 16000;
            String devPid = (String) args.getOrDefault("dev_pid", "1537");

            if (audio == null || audio.isBlank()) {
                return ToolResult.error("Error: audio is required (Base64 encoded)");
            }

            return parent.recognize(audio, format, rate, devPid);
        }
    }

    /**
     * 执行语音识别
     */
    private ToolResult recognize(String audio, String format, int rate, String devPid) {
        try {
            String token = getAccessToken();
            if (token == null) {
                return ToolResult.error("Error: Failed to get access token");
            }

            // 解析 Base64 音频数据
            byte[] audioBytes;
            try {
                audioBytes = Base64.getDecoder().decode(audio);
            } catch (IllegalArgumentException e) {
                return ToolResult.error("Error: Invalid Base64 audio data");
            }

            String devPidFinal = config.getDevPid();
            if (devPid != null && !devPid.isBlank()) {
                devPidFinal = devPid;
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("format", format);
            requestBody.put("rate", rate);
            requestBody.put("dev_pid", devPidFinal);
            requestBody.put("channel", 1);
            requestBody.put("speech", Base64.getEncoder().encodeToString(audioBytes));
            requestBody.put("len", audioBytes.length);

            String requestJson = objectMapper.writeValueAsString(requestBody);

            // 使用鉴权 URL
            String url = config.getEndpoint() + "?dev_pid=" + devPidFinal + "&cuid=yunxi_mcp&token=" + token;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .timeout(Duration.ofSeconds(config.getTimeout()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("ASR API error: status={}, body={}", response.statusCode(), response.body());
                return ToolResult.error("Error: API request failed with status " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());

            // 检查错误
            if (root.has("err_no")) {
                int errNo = root.get("err_no").asInt();
                if (errNo != 0) {
                    String errMsg = root.has("err_msg") ? root.get("err_msg").asText() : "Unknown error";
                    return ToolResult.error("Error [" + errNo + "]: " + errMsg);
                }
            }

            // 解析结果
            JsonNode result = root.get("result");
            if (result == null || result.isEmpty()) {
                return ToolResult.text("No speech recognized.");
            }

            StringBuilder sb = new StringBuilder();
            sb.append("🎤 语音识别结果:\n\n");

            // 合并所有识别结果
            for (JsonNode item : result) {
                sb.append(item.asText());
            }

            // 添加置信度信息（如果有）
            if (root.has("confidence")) {
                sb.append("\n\n📊 置信度: ").append(root.get("confidence").asText());
            }

            return ToolResult.text(sb.toString());

        } catch (Exception e) {
            log.error("ASR error: {}", e.getMessage(), e);
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