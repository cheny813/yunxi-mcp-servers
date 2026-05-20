package io.yunxi.mcp.xlsx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Excel 处理 MCP 服务器应用程序入口
 * <p>
 * 提供 Excel 文件处理能力的 MCP 服务器支持。
 * 支持表格读取、写入、创建、CSV 转换等操作。
 * </p>
 *
 * <h3>使用方式</h3>
 *
 * <pre>
 * java -jar mcp-xlsx.jar --server.port=40102
 * </pre>
 *
 * <h3>提供工具</h3>
 * <ul>
 * <li>xlsx_operation - Excel 操作（读取、写入、创建、CSV转换）</li>
 * </ul>
 *
 * @author yunxi-mcp-servers
 * @version 1.0.0
 */
@SpringBootApplication
public class XlsxMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(XlsxMcpApplication.class, args);
    }
}