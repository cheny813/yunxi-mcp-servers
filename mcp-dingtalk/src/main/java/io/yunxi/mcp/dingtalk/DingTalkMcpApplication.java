package io.yunxi.mcp.dingtalk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 钉钉通知 MCP 服务器应用程序入口
 * <p>
 * 提供钉钉消息发送能力的 MCP 服务器支持。
 * 支持文本、Markdown、链接等消息类型。
 * </p>
 *
 * <h3>使用方式</h3>
 *
 * <pre>
 * java -jar mcp-dingtalk.jar --server.port=40101
 * </pre>
 *
 * <h3>环境变量</h3>
 * <ul>
 * <li>DINGTALK_WEBHOOK - 钉钉 Webhook 地址（必需）</li>
 * <li>DINGTALK_SECRET - 钉钉加签密钥（可选）</li>
 * </ul>
 *
 * <h3>提供工具</h3>
 * <ul>
 * <li>dingtalk_send_text - 发送文本消息</li>
 * <li>dingtalk_send_markdown - 发送 Markdown 消息</li>
 * <li>dingtalk_send_link - 发送链接消息</li>
 * </ul>
 *
 * @author yunxi-mcp-servers
 * @version 1.0.0
 */
@SpringBootApplication
public class DingTalkMcpApplication {
    public static void main(String[] args) {
        SpringApplication.run(DingTalkMcpApplication.class, args);
    }
}