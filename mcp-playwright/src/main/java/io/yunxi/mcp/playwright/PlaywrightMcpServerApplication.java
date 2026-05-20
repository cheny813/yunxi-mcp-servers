package io.yunxi.mcp.playwright;

import com.microsoft.playwright.Playwright;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MCP Playwright Server 主启动类
 * 
 * 提供 Playwright 浏览器自动化能力，支持页面读取、表单填写、自动提交。
 *
 * <h3>核心功能</h3>
 * <ul>
 *   <li>页面导航和跳转</li>
 *   <li>表单读取和填写</li>
 *   <li>下拉框、复选框、单选框操作</li>
 *   <li>文件上传</li>
 *   <li>自动提交</li>
 *   <li>页面截图</li>
 * </ul>
 *
 * <h3>应用场景</h3>
 * <ul>
 *   <li>传统系统 RPA 自动化</li>
 *   <li>数据抓取</li>
 *   <li>表单自动填写</li>
 *   <li>业务流程自动化</li>
 * </ul>
 *
 * @author yunxi
 * @version 1.0.0
 */
@Slf4j
@SpringBootApplication
public class PlaywrightMcpServerApplication {

    public static void main(String[] args) {
        log.info("========================================");
        log.info("  MCP Playwright Server Starting...");
        log.info("  Port: 8096");
        log.info("  Browser: Chromium");
        log.info("========================================");
        SpringApplication.run(PlaywrightMcpServerApplication.class, args);
        log.info("MCP Playwright Server started successfully!");
    }
}
