package io.yunxi.mcp.docker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Docker MCP 服务器应用程序入口
 * <p>
 * 提供 Docker 容器和镜像管理的 MCP 服务器支持。
 * </p>
 *
 * <h3>使用方式</h3>
 * <pre>
 * java -jar mcp-docker.jar --server.port=8086
 * </pre>
 */
@SpringBootApplication
public class DockerMcpApplication {

    /**
     * 应用程序主入口
     * <p>
     * 启动 Spring Boot 应用程序，初始化 Docker MCP 服务器。
     * </p>
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(DockerMcpApplication.class, args);
    }
}
