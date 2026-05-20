package io.yunxi.mcp.formfill;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 表单填写 MCP 服务器
 * 
 * 提供智能表单填写能力，支持多种业务场景：
 * - 营养食谱管理（单日/周计划）
 * - 食品安全检测
 * - 经费管理
 * - 通用表单
 * 
 * 使用方式：
 * AI Agent 可以直接调用这些工具来填写表单，
 * 而不需要依赖前端的 Markdown 解析逻辑。
 */
@SpringBootApplication
public class FormFillMcpServerApplication {

    private static final Logger log = LoggerFactory.getLogger(FormFillMcpServerApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(FormFillMcpServerApplication.class, args);
        log.info("========================================");
        log.info("  表单填写 MCP 服务器已启动");
        log.info("  端口: 3003");
        log.info("========================================");
    }
}
