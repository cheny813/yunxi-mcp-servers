package io.yunxi.mcp.pdf;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * PDF 处理 MCP 服务器应用程序入口
 * <p>
 * 提供 PDF 文件处理能力的 MCP 服务器支持。
 * 支持 PDF 读取、提取文本、合并、拆分、加密、解密等操作。
 * </p>
 *
 * <h3>使用方式</h3>
 *
 * <pre>
 * java -jar mcp-pdf.jar --server.port=40103
 * </pre>
 *
 * <h3>提供工具</h3>
 * <ul>
 * <li>pdf_operation - PDF 操作（提取文本、获取信息、合并、拆分、旋转、加密、解密）</li>
 * </ul>
 *
 * @author yunxi-mcp-servers
 * @version 1.0.0
 */
@SpringBootApplication
public class PdfMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(PdfMcpApplication.class, args);
    }
}