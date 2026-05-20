package io.yunxi.mcp.code;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 代码搜索 MCP 服务器应用程序入口
 * <p>
 * 提供代码搜索和文件内容查看的 MCP 服务器支持。
 * 支持在指定代码目录中搜索代码片段、查看文件内容，
 * 用于代码审查、问题定位、代码分析等场景。
 * </p>
 *
 * <h3>功能特性</h3>
 * <ul>
 * <li>代码搜索 - 支持关键词搜索、正则匹配、文件类型过滤</li>
 * <li>文件内容查看 - 读取指定文件的内容</li>
 * <li>目录浏览 - 列出代码目录结构</li>
 * <li>多语言支持 - 支持 Java、JavaScript、Python、Go 等常见语言</li>
 * </ul>
 *
 * <h3>使用方式</h3>
 *
 * <pre>
 * java -jar mcp-code-search-1.0.0.jar --server.port=40106 \
 *   --code.search.path=/path/to/code
 * </pre>
 *
 * <h3>提供工具</h3>
 * <ul>
 * <li>search_code - 搜索代码</li>
 * <li>read_file_content - 读取文件内容</li>
 * <li>list_directories - 列出目录</li>
 * </ul>
 *
 * @author yunxi-mcp-servers
 * @version 1.0.0
 * @since 2026-04-13
 */
@SpringBootApplication
public class CodeSearchMcpApplication {

    /**
     * 应用程序主入口
     * <p>
     * 启动 Spring Boot 应用程序，初始化代码搜索 MCP 服务器。
     * </p>
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(CodeSearchMcpApplication.class, args);
    }
}
