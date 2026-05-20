package io.yunxi.mcp.memory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MCP Memory Tool 应用启动类
 * <p>
 * 独立的 MCP Memory Tool 服务器应用
 * 提供记忆管理相关的 MCP 工具
 * </p>
 */
@SpringBootApplication(scanBasePackages = "io.yunxi.mcp.memory")
public class MemoryToolApplication {

    private static final Logger log = LoggerFactory.getLogger(MemoryToolApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(MemoryToolApplication.class, args);
        
        log.info("========================================");
        log.info("MCP Memory Tool Server Started");
        log.info("Available endpoints:");
        log.info("  - /mcp/memory/health      : Health check");
        log.info("  - /mcp/memory/tools       : List all tools");
        log.info("  - /mcp/memory/invoke       : Invoke a tool");
        log.info("  - /mcp/memory/tools/{name}  : Get tool definition");
        log.info("  - /mcp/memory/info        : Server information");
        log.info("========================================");
    }
}
