package io.yunxi.mcp.baiduocr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 百度 OCR MCP 服务器应用程序入口
 * <p>
 * 提供百度文字识别 API 的 MCP 服务器支持。
 * 支持通用文字识别、高精度文字识别等。
 * </p>
 *
 * <h3>使用方式</h3>
 *
 * <pre>
 * java -jar mcp-baidu-ocr.jar --server.port=40110
 * </pre>
 *
 * <h3>环境变量</h3>
 * <ul>
 * <li>BAIDU_OCR_API_KEY - 百度 OCR API Key（必需）</li>
 * <li>BAIDU_OCR_SECRET_KEY - 百度 OCR Secret Key（必需）</li>
 * </ul>
 *
 * <h3>提供工具</h3>
 * <ul>
 * <li>baidu_ocr_general - 通用文字识别</li>
 * <li>baidu_ocr_accurate - 高精度文字识别</li>
 * </ul>
 *
 * @author yunxi-mcp-servers
 * @version 1.0.0
 */
@SpringBootApplication
public class BaiduOcrMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(BaiduOcrMcpApplication.class, args);
    }
}