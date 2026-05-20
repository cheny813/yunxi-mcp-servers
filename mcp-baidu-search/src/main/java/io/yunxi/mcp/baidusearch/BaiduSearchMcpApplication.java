package io.yunxi.mcp.baidusearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 百度搜索 MCP 服务器应用程序入口
 * <p>
 * 提供百度 AI 搜索 API 的 MCP 服务器支持。
 * 支持实时信息搜索、新闻查询等。
 * </p>
 *
 * <h3>使用方式</h3>
 *
 * <pre>
 * java -jar mcp-baidu-search.jar --server.port=40111
 * </pre>
 *
 * <h3>环境变量</h3>
 * <ul>
 * <li>BAIDU_API_KEY - 百度千帆 API Key（必需）</li>
 * </ul>
 *
 * <h3>提供工具</h3>
 * <ul>
 * <li>baidu_search - 百度搜索</li>
 * </ul>
 *
 * @author yunxi-mcp-servers
 * @version 1.0.0
 */
@SpringBootApplication
public class BaiduSearchMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(BaiduSearchMcpApplication.class, args);
    }
}