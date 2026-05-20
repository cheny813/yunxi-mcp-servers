package io.yunxi.mcp.codeedit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 代码编辑 MCP 服务器应用程序入口
 * <p>
 * 提供代码文件编辑功能的 MCP 服务器支持。
 * 支持通过字符串替换方式修改代码文件，
 * 用于自动修复代码问题、批量重构等场景。
 * </p>
 *
 * <h3>功能特性</h3>
 * <ul>
 * <li>代码编辑 - 通过字符串替换修改代码</li>
 * <li>安全保护 - 自动备份原文件、验证原内容匹配</li>
 * <li>自动注释 - 添加修改原因注释</li>
 * <li>编码处理 - 自动处理 UTF-8 编码</li>
 * </ul>
 *
 * <h3>安全机制</h3>
 * <ul>
 * <li>自动备份原文件</li>
 * <li>验证原内容匹配后再修改</li>
 * <li>自动添加修改注释</li>
 * </ul>
 *
 * <h3>使用方式</h3>
 *
 * <pre>
 * java -jar mcp-code-edit-1.0.0.jar --server.port=40107 \
 *   --code.edit.path=/path/to/code
 * </pre>
 *
 * <h3>提供工具</h3>
 * <ul>
 * <li>edit_code - 编辑代码文件</li>
 * </ul>
 *
 * <h3>⚠️ 警告</h3>
 * <p>
 * 此服务会修改代码文件，请谨慎使用！建议先备份代码。
 * </p>
 *
 * @author yunxi-mcp-servers
 * @version 1.0.0
 * @since 2026-04-13
 */
@SpringBootApplication
public class CodeEditMcpApplication {

    /**
     * 应用程序主入口
     * <p>
     * 启动 Spring Boot 应用程序，初始化代码编辑 MCP 服务器。
     * </p>
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(CodeEditMcpApplication.class, args);
    }
}
