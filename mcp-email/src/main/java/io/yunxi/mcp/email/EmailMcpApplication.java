package io.yunxi.mcp.email;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Email MCP 服务器应用程序入口
 * <p>
 * 提供邮件发送功能的 MCP 服务器支持。
 * </p>
 *
 * <h3>使用方式</h3>
 * 
 * <pre>
 * java -jar mcp-email.jar --server.port=8088 \
 *   --spring.mail.host=smtp.gmail.com \
 *   --spring.mail.username=your@email.com \
 *   --spring.mail.password=your-password
 * </pre>
 */
@SpringBootApplication
public class EmailMcpApplication {

    /**
     * 应用程序主入口
     * <p>
     * 启动 Spring Boot 应用程序，初始化 Email MCP 服务器。
     * </p>
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(EmailMcpApplication.class, args);
    }
}
