package io.yunxi.mcp.dingtalk;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 钉钉通知配置
 *
 * @author yunxi-mcp-servers
 * @version 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "dingtalk")
public class DingTalkConfig {

    /**
     * 是否启用钉钉通知
     */
    private boolean enabled = true;

    /**
     * 钉钉 Webhook 地址
     * 从环境变量 DINGTALK_WEBHOOK 获取
     */
    private String webhook;

    /**
     * 钉钉加签密钥 (可选)
     * 从环境变量 DINGTALK_SECRET 获取
     */
    private String secret;

    /**
     * 默认消息类型: text, markdown, link, actionCard
     */
    private String defaultMsgType = "text";

    /**
     * 超时时间（秒）
     */
    private int timeout = 10;
}