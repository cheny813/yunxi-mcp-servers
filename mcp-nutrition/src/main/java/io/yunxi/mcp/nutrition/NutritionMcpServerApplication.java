package io.yunxi.mcp.nutrition;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 营养数据 MCP 服务端 (v2.0)
 *
 * <p>独立部署的 MCP 服务，通过官方 MCP SDK SSE 传输与 Agent 通信。</p>
 * <p>ToolHandler 由 McpServerFactory 自动发现并注册，无需手动注册。</p>
 *
 * @author yunxi-agent-platform
 */
@SpringBootApplication
public class NutritionMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(NutritionMcpServerApplication.class, args);
    }
}
