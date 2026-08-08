package io.yunxi.mcp.formfill;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 表单填写 MCP 服务器
 *
 * 通用表单填写能力：根据"场景配置"将外部数据映射到目标表单元素，
 * 并通过 WebSocket 下发给前端执行填写。场景由 classpath 下的
 * scenarios/*.json 驱动，部署时按需放入即可，无需改动代码。
 *
 * AI Agent 直接调用工具（formfill_fill / formfill_get_structure / formfill_list_scenes）即可填写表单。
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
