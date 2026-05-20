package io.yunxi.mcp.email.tool;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.HashMap;
import java.util.Map;

/**
 * Email 工具集合
 * <p>
 * 提供邮件发送的 MCP 工具实现，使用 Spring JavaMailSender 发送邮件。
 * </p>
 *
 * <h3>提供的工具</h3>
 * <ul>
 * <li>{@link SendEmailTool} - 发送邮件</li>
 * </ul>
 */
public class EmailTools {

    /**
     * 邮件发送器
     */
    private final JavaMailSender mailSender;

    /**
     * 发件人地址
     */
    private final String fromAddress;

    /**
     * 构造函数
     *
     * @param mailSender  邮件发送器
     * @param fromAddress 发件人地址
     */
    public EmailTools(JavaMailSender mailSender, String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    /**
     * 发送邮件
     * <p>
     * 工具名称: {@code send_email}
     * </p>
     * <p>
     * 发送文本格式的邮件到指定的收件人。
     * </p>
     */
    public static class SendEmailTool implements ToolHandler {
        /**
         * 邮件发送器
         */
        private final JavaMailSender mailSender;

        /**
         * 发件人地址
         */
        private final String fromAddress;

        /**
         * 构造函数
         *
         * @param mailSender  邮件发送器
         * @param fromAddress 发件人地址
         */
        public SendEmailTool(JavaMailSender mailSender, String fromAddress) {
            this.mailSender = mailSender;
            this.fromAddress = fromAddress;
        }

        /**
         * 获取工具定义
         *
         * @return 工具定义对象
         */
        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("send_email")
                    .description(
                            "Send an email. " +
                                    "发送电子邮件。 " +
                                    "Use this when you need to send notifications, alerts, reports, or any email communication. "
                                    +
                                    "适用于发送通知、警报、报告或任何邮件通信等场景。 " +
                                    "Common use cases: notifications, password reset, reports, alerts. " +
                                    "典型用例：通知、密码重置、报告、警报。")
                    .inputSchema(schema(
                            "to", "string",
                            "Recipient email address. Example: 'user@example.com' | 收件人邮箱地址。示例: 'user@example.com'",
                            "subject", "string", "Email subject. Example: 'Welcome!' | 邮件主题。示例: '欢迎！'",
                            "body", "string", "Email body content. Example: 'Hello, ...' | 邮件正文内容。示例: '您好，...'"))
                    .build();
        }

        /**
         * 执行发送邮件操作
         * <p>
         * 发送邮件到指定的收件人，包含主题和正文内容。
         * </p>
         *
         * @param args 参数 Map，支持以下字段：
         *             <ul>
         *             <li>to (必需) - 收件人邮箱地址</li>
         *             <li>subject (必需) - 邮件主题</li>
         *             <li>body (必需) - 邮件正文</li>
         *             </ul>
         * @return 工具执行结果
         */
        @Override
        public ToolResult execute(Map<String, Object> args) {
            String to = (String) args.get("to");
            String subject = (String) args.get("subject");
            String body = (String) args.get("body");

            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromAddress);
                message.setTo(to);
                message.setSubject(subject);
                message.setText(body);

                mailSender.send(message);
                return ToolResult.text("Email sent successfully to: " + to);
            } catch (Exception e) {
                return ToolResult.error("Error: " + e.getMessage());
            }
        }
    }

    /**
     * 构建输入参数 Schema（支持多个参数）
     *
     * @param props 可变参数，每3个为一组：名称、类型、描述
     * @return 参数定义 Map
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
