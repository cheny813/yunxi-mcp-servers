package io.yunxi.mcp.pptx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * PowerPoint 处理 MCP 服务器应用程序入口
 * <p>
 * 提供 PowerPoint 文件处理能力的 MCP 服务器支持。
 * 支持演示文稿读取、获取信息、创建、添加幻灯片等操作。
 * </p>
 *
 * <h3>使用方式</h3>
 *
 * <pre>
 * java -jar mcp-pptx.jar --server.port=40104
 * </pre>
 *
 * <h3>提供工具</h3>
 * <ul>
 * <li>pptx_operation - PowerPoint 操作（读取、获取信息、创建、添加幻灯片）</li>
 * </ul>
 *
 * @author yunxi-mcp-servers
 * @version 1.0.0
 */
@SpringBootApplication
public class PptxMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(PptxMcpApplication.class, args);
    }
}