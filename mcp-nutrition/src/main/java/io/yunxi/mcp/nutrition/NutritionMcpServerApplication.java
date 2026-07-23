package io.yunxi.mcp.nutrition;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 营养数据 MCP 服务端 (v2.0)
 *
 * <p>独立部署的 MCP 服务，通过官方 MCP SDK SSE 传输与 Agent 通信。</p>
 * <p>ToolHandler 由 McpServerFactory 自动发现并注册，无需手动注册。</p>
 * <p>@EnableScheduling 启用同步定时任务（SchoolDishSyncRunner 的菜品增量同步）。</p>
 *
 * @author yunxi-agent-platform
 */
@SpringBootApplication
@EnableScheduling
public class NutritionMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(NutritionMcpServerApplication.class, args);
    }
}
