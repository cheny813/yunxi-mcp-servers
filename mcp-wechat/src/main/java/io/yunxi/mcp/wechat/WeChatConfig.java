package io.yunxi.mcp.wechat;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 企业微信配置
 *
 * <p>
 * 支持两种模式：
 * <ul>
 *   <li>应用消息模式: 通过 CorpID + Secret 获取 access_token，调用应用消息接口</li>
 *   <li>群机器人模式: 通过 Webhook URL 直接发送消息</li>
 * </ul>
 * </p>
 *
 * @author yunxi-mcp-servers
 * @version 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "wechat")
public class WeChatConfig {

    /** 是否启用 */
    private boolean enabled = true;

    /** 企业 CorpID */
    private String corpId;

    /** 应用 AgentId */
    private String agentId;

    /** 应用 Secret */
    private String secret;

    /** 群机器人 Webhook（可选，优先使用） */
    private String webhook;

    /** 超时时间（秒） */
    private int timeout = 10;

    /** Token 缓存时间（秒） */
    private long tokenCacheSeconds = 7200;

    /** 缓存的 access_token */
    private String cachedToken;

    /** Token 获取时间 */
    private long tokenFetchedAt;
}
