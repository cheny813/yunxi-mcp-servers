package io.yunxi.mcp.baiduasr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 百度语音识别 MCP 服务器应用程序入口
 * <p>
 * 提供百度语音识别 API 的 MCP 服务器支持。
 * 支持将音频转换为文字。
 * </p>
 *
 * <h3>使用方式</h3>
 *
 * <pre>
 * java -jar mcp-baidu-asr.jar --server.port=40112
 * </pre>
 *
 * <h3>环境变量</h3>
 * <ul>
 * <li>BAIDU_ASR_API_KEY - 百度 ASR API Key（必需）</li>
 * <li>BAIDU_ASR_SECRET_KEY - 百度 ASR Secret Key（必需）</li>
 * </ul>
 *
 * <h3>提供工具</h3>
 * <ul>
 * <li>baidu_asr - 语音转文字</li>
 * </ul>
 *
 * @author yunxi-mcp-servers
 * @version 1.0.0
 */
@SpringBootApplication
public class BaiduAsrMcpApplication {
    public static void main(String[] args) {
        SpringApplication.run(BaiduAsrMcpApplication.class, args);
    }
}