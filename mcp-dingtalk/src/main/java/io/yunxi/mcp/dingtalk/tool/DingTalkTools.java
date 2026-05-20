package io.yunxi.mcp.dingtalk.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.yunxi.mcp.dingtalk.DingTalkConfig;
import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 钉钉通知工具
 * <p>
 * 提供钉钉消息发送的 MCP 工具实现。
 * 支持文本、Markdown、链接、ActionCard 等消息类型。
 * </p>
 *
 * <h3>提供的工具</h3>
 * <ul>
 * <li>{@link SendTextTool} - 发送文本消息</li>
 * <li>{@link SendMarkdownTool} - 发送 Markdown 消息</li>
 * <li>{@link SendLinkTool} - 发送链接消息</li>
 * </ul>
 *
 * <h3>配置</h3>
 * <p>
 * 需要配置 DINGTALK_WEBHOOK 环境变量
 * 可选配置 DINGTALK_SECRET 用于加签
 * </p>
 */
@Slf4j
@Component
public class DingTalkTools {

    private final DingTalkConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public DingTalkTools(DingTalkConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.getTimeout()))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 发送文本消息工具
     */
    public static class SendTextTool implements ToolHandler {
        private final DingTalkTools parent;

        @Autowired
        public SendTextTool(DingTalkTools parent) {
            this.parent = parent;
        }

        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("dingtalk_send_text")
                    .description(
                            "发送钉钉文本消息 (Send Text) - 向钉钉群或用户发送文本消息。" +
                                    "典型用例：告警通知、任务提醒、系统状态通知。")
                    .inputSchema(DingTalkTools.schema(
                            "content", "string", "Message content. 消息内容。",
                            "atMobiles", "array", "Phone numbers to @. @被唤醒的手机号列表 (可选)。",
                            "isAtAll", "boolean", "Whether to @all. 是否 @所有人，默认 false。")
                    )
                    .build();
        }

        @Override
        public ToolResult execute(Map<String, Object> args) {
            String content = (String) args.get("content");
            if (content == null || content.isBlank()) {
                return ToolResult.error("Error: content is required");
            }

            @SuppressWarnings("unchecked")
            java.util.List<String> atMobiles = (java.util.List<String>) args.get("atMobiles");
            Boolean isAtAll = (Boolean) args.getOrDefault("isAtAll", false);

            return parent.sendText(content, atMobiles, isAtAll);
        }
    }

    /**
     * 发送 Markdown 消息工具
     */
    public static class SendMarkdownTool implements ToolHandler {
        private final DingTalkTools parent;

        @Autowired
        public SendMarkdownTool(DingTalkTools parent) {
            this.parent = parent;
        }

        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("dingtalk_send_markdown")
                    .description(
                            "发送钉钉 Markdown 消息 (Send Markdown) - 发送富格式 Markdown 消息。" +
                                    "支持标题、列表、代码、链接等。" +
                                    "典型用例：详细告警信息、报告摘要、状态面板。")
                    .inputSchema(DingTalkTools.schema(
                            "title", "string", "Message title. 消息标题。",
                            "text", "string", "Markdown content. Markdown 格式的消息内容。",
                            "atMobiles", "array", "Phone numbers to @. @被唤醒的手机号列表 (可选)。",
                            "isAtAll", "boolean", "Whether to @all. 是否 @所有人。")
                    )
                    .build();
        }

        @Override
        public ToolResult execute(Map<String, Object> args) {
            String title = (String) args.get("title");
            String text = (String) args.get("text");

            if (title == null || title.isBlank()) {
                return ToolResult.error("Error: title is required");
            }
            if (text == null || text.isBlank()) {
                return ToolResult.error("Error: text is required");
            }

            @SuppressWarnings("unchecked")
            java.util.List<String> atMobiles = (java.util.List<String>) args.get("atMobiles");
            Boolean isAtAll = (Boolean) args.getOrDefault("isAtAll", false);

            return parent.sendMarkdown(title, text, atMobiles, isAtAll);
        }
    }

    /**
     * 发送链接消息工具
     */
    public static class SendLinkTool implements ToolHandler {
        private final DingTalkTools parent;

        @Autowired
        public SendLinkTool(DingTalkTools parent) {
            this.parent = parent;
        }

        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("dingtalk_send_link")
                    .description(
                            "发送钉钉链接消息 (Send Link) - 发送带有链接的消息卡片。" +
                                    "典型用例：点击查看详情、外部系统链接。")
                    .inputSchema(DingTalkTools.schema(
                            "title", "string", "Message title. 消息标题。",
                            "text", "string", "Content summary. 内容摘要。",
                            "url", "string", "Link URL. 跳转链接。",
                            "picUrl", "string", "Image URL (optional). 图片链接，可为空。")
                    )
                    .build();
        }

        @Override
        public ToolResult execute(Map<String, Object> args) {
            String title = (String) args.get("title");
            String text = (String) args.get("text");
            String url = (String) args.get("url");
            String picUrl = (String) args.get("picUrl");

            if (title == null || title.isBlank()) {
                return ToolResult.error("Error: title is required");
            }
            if (text == null || text.isBlank()) {
                return ToolResult.error("Error: text is required");
            }
            if (url == null || url.isBlank()) {
                return ToolResult.error("Error: url is required");
            }

            return parent.sendLink(title, text, url, picUrl);
        }
    }

    /**
     * 发送文本消息
     */
    public ToolResult sendText(String content, java.util.List<String> atMobiles, boolean isAtAll) {
        try {
            String webhook = getWebhook();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("msgtype", "text");

            Map<String, Object> text = new HashMap<>();
            text.put("content", content);
            requestBody.put("text", text);

            Map<String, Object> at = new HashMap<>();
            if (atMobiles != null && !atMobiles.isEmpty()) {
                at.put("atMobiles", atMobiles);
            }
            at.put("isAtAll", isAtAll);
            requestBody.put("at", at);

            return sendMessage(webhook, requestBody);
        } catch (Exception e) {
            log.error("Send text error: {}", e.getMessage(), e);
            return ToolResult.error("Error: " + e.getMessage());
        }
    }

    /**
     * 发送 Markdown 消息
     */
    public ToolResult sendMarkdown(String title, String text, java.util.List<String> atMobiles, boolean isAtAll) {
        try {
            String webhook = getWebhook();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("msgtype", "markdown");

            Map<String, Object> markdown = new HashMap<>();
            markdown.put("title", title);
            markdown.put("text", text);
            requestBody.put("markdown", markdown);

            Map<String, Object> at = new HashMap<>();
            if (atMobiles != null && !atMobiles.isEmpty()) {
                at.put("atMobiles", atMobiles);
            }
            at.put("isAtAll", isAtAll);
            requestBody.put("at", at);

            return sendMessage(webhook, requestBody);
        } catch (Exception e) {
            log.error("Send markdown error: {}", e.getMessage(), e);
            return ToolResult.error("Error: " + e.getMessage());
        }
    }

    /**
     * 发送链接消息
     */
    public ToolResult sendLink(String title, String text, String url, String picUrl) {
        try {
            String webhook = getWebhook();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("msgtype", "link");

            Map<String, Object> link = new HashMap<>();
            link.put("title", title);
            link.put("text", text);
            link.put("url", url);
            if (picUrl != null && !picUrl.isBlank()) {
                link.put("picUrl", picUrl);
            }
            requestBody.put("link", link);

            return sendMessage(webhook, requestBody);
        } catch (Exception e) {
            log.error("Send link error: {}", e.getMessage(), e);
            return ToolResult.error("Error: " + e.getMessage());
        }
    }

    /**
     * 获取Webhook地址
     */
    private String getWebhook() {
        String webhook = config.getWebhook();
        if (webhook == null || webhook.isBlank()) {
            webhook = System.getenv("DINGTALK_WEBHOOK");
        }
        if (webhook == null || webhook.isBlank()) {
            throw new IllegalStateException("DINGTALK_WEBHOOK not configured");
        }
        return webhook;
    }

    /**
     * 发送消息
     */
    private ToolResult sendMessage(String webhook, Map<String, Object> requestBody) {
        try {
            String url = webhook;
            String secret = config.getSecret();

            // 如果有密钥，进行签名
            if (secret != null && !secret.isBlank()) {
                secret = System.getenv("DINGTALK_SECRET") != null
                        ? System.getenv("DINGTALK_SECRET") : secret;
                if (!secret.isBlank()) {
                    url = signUrl(webhook, secret);
                }
            }

            String requestJson = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .timeout(Duration.ofSeconds(config.getTimeout()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            JsonNode root = objectMapper.readTree(response.body());

            if (root.has("errcode") && root.get("errcode").asInt() != 0) {
                String errMsg = root.has("errmsg") ? root.get("errmsg").asText() : "Unknown error";
                return ToolResult.error("Error: " + errMsg);
            }

            return ToolResult.text("✅ 钉钉消息发送成功");

        } catch (Exception e) {
            log.error("Send message error: {}", e.getMessage(), e);
            return ToolResult.error("Error: " + e.getMessage());
        }
    }

    /**
     * 生成签名 URL
     */
    private String signUrl(String webhook, String secret) {
        try {
            long timestamp = System.currentTimeMillis();
            String sign = generateSign(secret, timestamp);

            String separator = webhook.contains("?") ? "&" : "?";
            return webhook + separator + "timestamp=" + timestamp + "&sign=" + sign;
        } catch (Exception e) {
            log.warn("Failed to sign URL: {}", e.getMessage());
            return webhook;
        }
    }

    /**
     * 生成签名
     */
    private String generateSign(String secret, long timestamp) throws Exception {
        String stringToSign = timestamp + "\n" + secret;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256"));
        byte[] signData = mac.doFinal(stringToSign.getBytes("UTF-8"));
        return URLEncoder.encode(Base64.getEncoder().encodeToString(signData), "UTF-8");
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