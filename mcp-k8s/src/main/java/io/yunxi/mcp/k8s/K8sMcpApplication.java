package io.yunxi.mcp.k8s;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Kubernetes MCP 服务器应用程序入口
 * <p>
 * 提供 Kubernetes 资源管理的 MCP 服务器支持。
 * </p>
 *
 * <h3>使用方式</h3>
 * 
 * <pre>
 * java -jar mcp-k8s.jar --server.port=8087
 * </pre>
 */
@SpringBootApplication
public class K8sMcpApplication {

    /**
     * 应用程序主入口
     * <p>
     * 启动 Spring Boot 应用程序，初始化 Kubernetes MCP 服务器。
     * </p>
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(K8sMcpApplication.class, args);
    }
}
