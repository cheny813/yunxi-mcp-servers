package io.yunxi.mcp.wechat.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import io.yunxi.mcp.wechat.WeChatConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 企业微信通知工具
 *
 * <p>
 * 提供企业微信消息发送的 MCP 工具实现。
 * 支持文本、Markdown 消息类型，同时支持群机器人 Webhook 和应用消息两种模式。
 * </p>
 *
 * <h3>提供的工具</h3>
 * <ul>
 *   <li>wechat_send_text - 发送文本消息</li>
 *   <li>wechat_send_markdown - 发送 Markdown 消息</li>
 * </ul>
 *
 * @author yunxi-mcp-servers
 * @version 1.0.0
 */
@Slf4j
@Component
public class WeChatTools {

    private final WeChatConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public WeChatTools(WeChatConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.getTimeout()))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    // ==================== MCP 工具定义 ====================

    /**
     * 发送文本消息工具
     */
    @Component
    public static class SendTextTool implements ToolHandler {
        private final WeChatTools parent;

        @Autowired
        public SendTextTool(WeChatTools parent) {
            this.parent = parent;
        }

        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("wechat_send_text")
                    .description(
                            "发送企业微信文本消息 - 向企业微信群或成员发送文本消息。" +
                                    "典型用例：告警通知、任务提醒、部署状态通知。")
                    .inputSchema(WeChatTools.schema(
                            "content", "string", "消息内容",
                            "mentionedList", "array", "被提醒人的userid列表（可选）",
                            "mentionedMobileList", "array", "被提醒人手机号列表（可选）")
                    )
                    .build();
        }

        @Override
        public ToolResult execute(Map<String, Object> args) {
            String content = (String) args.get("content");
            if (content == null || content.isBlank()) {
                return ToolResult.error("Error: content is required");
            }
            return parent.sendText(content);
        }
    }

    /**
     * 发送 Markdown 消息工具
     */
    @Component
    public static class SendMarkdownTool implements ToolHandler {
        private final WeChatTools parent;

        @Autowired
        public SendMarkdownTool(WeChatTools parent) {
            this.parent = parent;
        }

        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("wechat_send_markdown")
                    .description(
                            "发送企业微信 Markdown 消息 - 发送富格式 Markdown 消息。" +
                                    "支持标题、列表、链接等格式。" +
                                    "典型用例：详细告警信息、部署审批、状态面板。")
                    .inputSchema(WeChatTools.schema(
                            "title", "string", "消息标题",
                            "text", "string", "Markdown 格式的消息内容")
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

            return parent.sendMarkdown(title, text);
        }
    }

    // ==================== 消息发送实现 ====================

    /**
     * 发送文本消息
     */
    public ToolResult sendText(String content) {
        try {
            // 优先使用 Webhook 模式
            if (isWebhookMode()) {
                return sendViaWebhook("text", Map.of("content", content));
            }

            // 应用消息模式
            String token = getAccessToken();
            if (token == null) {
                return ToolResult.error("Error: 无法获取 access_token，请检查企业微信配置");
            }

            Map<String, Object> message = buildAppMessage("text", Map.of("content", content));
            return sendAppMessage(token, message);

        } catch (Exception e) {
            log.error("Send text error: {}", e.getMessage(), e);
            return ToolResult.error("Error: " + e.getMessage());
        }
    }

    /**
     * 发送 Markdown 消息
     */
    public ToolResult sendMarkdown(String title, String text) {
        try {
            if (isWebhookMode()) {
                return sendViaWebhook("markdown", Map.of("content", text));
            }

            String token = getAccessToken();
            if (token == null) {
                return ToolResult.error("Error: 无法获取 access_token，请检查企业微信配置");
            }

            Map<String, Object> message = buildAppMessage("markdown", Map.of("content", text));
            return sendAppMessage(token, message);

        } catch (Exception e) {
            log.error("Send markdown error: {}", e.getMessage(), e);
            return ToolResult.error("Error: " + e.getMessage());
        }
    }

    // ==================== Webhook 模式 ====================

    /**
     * 通过群机器人 Webhook 发送消息
     */
    private ToolResult sendViaWebhook(String msgType, Map<String, Object> content) {
        try {
            String webhook = config.getWebhook();
            if (webhook == null || webhook.isBlank()) {
                webhook = System.getenv("WECHAT_WEBHOOK");
            }
            if (webhook == null || webhook.isBlank()) {
                return ToolResult.error("Error: WECHAT_WEBHOOK not configured");
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("msgtype", msgType);
            requestBody.put(msgType, content);

            String requestJson = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhook))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .timeout(Duration.ofSeconds(config.getTimeout()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());

            int errcode = root.has("errcode") ? root.get("errcode").asInt() : -1;
            if (errcode != 0) {
                String errMsg = root.has("errmsg") ? root.get("errmsg").asText() : "Unknown error";
                return ToolResult.error("Error: " + errMsg);
            }

            return ToolResult.text("✅ 企业微信消息发送成功");

        } catch (Exception e) {
            log.error("Webhook send error: {}", e.getMessage(), e);
            return ToolResult.error("Error: " + e.getMessage());
        }
    }

    // ==================== 应用消息模式 ====================

    /**
     * 获取 access_token（带缓存）
     */
    private String getAccessToken() {
        // 检查缓存
        if (config.getCachedToken() != null && !config.getCachedToken().isBlank()) {
            long elapsed = System.currentTimeMillis() - config.getTokenFetchedAt();
            if (elapsed < config.getTokenCacheSeconds() * 1000) {
                return config.getCachedToken();
            }
        }

        try {
            String corpId = config.getCorpId() != null ? config.getCorpId() : System.getenv("WECHAT_CORP_ID");
            String secret = config.getSecret() != null ? config.getSecret() : System.getenv("WECHAT_SECRET");

            if (corpId == null || corpId.isBlank() || secret == null || secret.isBlank()) {
                log.error("WECHAT_CORP_ID or WECHAT_SECRET not configured");
                return null;
            }

            String url = String.format(
                    "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=%s&corpsecret=%s",
                    corpId, secret);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(config.getTimeout()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());

            int errcode = root.has("errcode") ? root.get("errcode").asInt() : -1;
            if (errcode != 0) {
                String errMsg = root.has("errmsg") ? root.get("errmsg").asText() : "Unknown error";
                log.error("Get access_token failed: errcode={}, errmsg={}", errcode, errMsg);
                return null;
            }

            String token = root.get("access_token").asText();
            config.setCachedToken(token);
            config.setTokenFetchedAt(System.currentTimeMillis());

            log.info("获取 access_token 成功");
            return token;

        } catch (Exception e) {
            log.error("Get access_token error: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 构建应用消息体
     */
    private Map<String, Object> buildAppMessage(String msgType, Map<String, Object> content) {
        Map<String, Object> message = new HashMap<>();
        message.put("touser", "@all");
        message.put("msgtype", msgType);
        message.put("agentid", config.getAgentId() != null ? Integer.parseInt(config.getAgentId()) : 0);
        message.put(msgType, content);
        return message;
    }

    /**
     * 通过应用消息接口发送
     */
    private ToolResult sendAppMessage(String token, Map<String, Object> message) {
        try {
            String url = "https://qyapi.weixin.qq.com/cgi-bin/message/send?access_token=" + token;
            String requestJson = objectMapper.writeValueAsString(message);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .timeout(Duration.ofSeconds(config.getTimeout()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());

            int errcode = root.has("errcode") ? root.get("errcode").asInt() : -1;
            if (errcode != 0) {
                String errMsg = root.has("errmsg") ? root.get("errmsg").asText() : "Unknown error";
                return ToolResult.error("Error: " + errMsg);
            }

            return ToolResult.text("✅ 企业微信消息发送成功");

        } catch (Exception e) {
            log.error("App message send error: {}", e.getMessage(), e);
            return ToolResult.error("Error: " + e.getMessage());
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 是否为 Webhook 模式
     */
    private boolean isWebhookMode() {
        String webhook = config.getWebhook();
        if (webhook == null || webhook.isBlank()) {
            webhook = System.getenv("WECHAT_WEBHOOK");
        }
        return webhook != null && !webhook.isBlank();
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
