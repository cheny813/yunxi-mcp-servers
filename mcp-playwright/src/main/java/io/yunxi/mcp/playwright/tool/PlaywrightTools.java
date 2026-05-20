package io.yunxi.mcp.playwright.tool;

import io.yunxi.mcp.playwright.service.PlaywrightService;
import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Playwright MCP 工具集
 * 
 * 提供浏览器自动化能力，包括：
 * - 启动/关闭浏览器
 * - 页面导航
 * - 获取页面内容
 * - 点击元素
 * - 输入文本
 * 
 * 适用场景：
 * - 网页自动化测试
 * - 数据采集
 * - 表单自动填写
 * - 网页截图
 */
@Slf4j
@Component
public class PlaywrightTools {

    private final PlaywrightService playwrightService;

    public PlaywrightTools(PlaywrightService playwrightService) {
        this.playwrightService = playwrightService;
    }

    public ToolHandler startBrowser() {
        return new ToolHandler() {
            @Override
            public ToolDefinition getDefinition() {
                return ToolDefinition.builder()
                        .name("playwright_start_browser")
                        .description("启动 Playwright 浏览器。用于开始一个自动化浏览器会话，支持 Chromium、Firefox、WebKit 等浏览器引擎。")
                        .inputSchema(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "headless", Map.of(
                                                "type", "boolean",
                                                "description", "是否使用无头模式（默认 true）"
                                        ),
                                        "browser", Map.of(
                                                "type", "string",
                                                "description", "浏览器类型：chromium、firefox、webkit（默认 chromium）",
                                                "enum", List.of("chromium", "firefox", "webkit")
                                        )
                                ),
                                "required", List.of()
                        ))
                        .build();
            }

            @Override
            public ToolResult execute(Map<String, Object> arguments) {
                try {
                    boolean success = playwrightService.startBrowser();
                    return success ? ToolResult.text("浏览器启动成功") : ToolResult.error("浏览器启动失败");
                } catch (Exception e) {
                    return ToolResult.error("启动浏览器时发生错误: " + e.getMessage());
                }
            }
        };
    }

    public ToolHandler navigate() {
        return new ToolHandler() {
            @Override
            public ToolDefinition getDefinition() {
                return ToolDefinition.builder()
                        .name("playwright_navigate")
                        .description("导航到指定 URL 地址。在浏览器中打开指定的网页链接。")
                        .inputSchema(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "url", Map.of(
                                                "type", "string",
                                                "description", "要访问的完整 URL 地址，例如：https://example.com"
                                        ),
                                        "waitUntil", Map.of(
                                                "type", "string",
                                                "description", "等待条件：load（页面加载完成）、domcontentloaded（DOM 加载完成）、networkidle（网络空闲）",
                                                "enum", List.of("load", "domcontentloaded", "networkidle")
                                        )
                                ),
                                "required", List.of("url")
                        ))
                        .build();
            }

            @Override
            public ToolResult execute(Map<String, Object> arguments) {
                try {
                    String url = (String) arguments.get("url");
                    boolean success = playwrightService.navigate(url);
                    return success ? ToolResult.text("成功导航到: " + url) : ToolResult.error("导航失败");
                } catch (Exception e) {
                    return ToolResult.error("导航时发生错误: " + e.getMessage());
                }
            }
        };
    }

    public ToolHandler getPageContent() {
        return new ToolHandler() {
            @Override
            public ToolDefinition getDefinition() {
                return ToolDefinition.builder()
                        .name("playwright_get_content")
                        .description("获取当前页面的 HTML 内容。用于读取网页源码或提取页面数据。")
                        .inputSchema(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "selector", Map.of(
                                                "type", "string",
                                                "description", "可选，CSS 选择器。如果指定，只返回该元素的 HTML"
                                        )
                                ),
                                "required", List.of()
                        ))
                        .build();
            }

            @Override
            public ToolResult execute(Map<String, Object> arguments) {
                try {
                    String content = playwrightService.getPageContent();
                    return ToolResult.text("页面内容长度: " + content.length() + " 字符\n\n" + 
                            (content.length() > 2000 ? content.substring(0, 2000) + "..." : content));
                } catch (Exception e) {
                    return ToolResult.error("获取页面内容时发生错误: " + e.getMessage());
                }
            }
        };
    }

    public ToolHandler click() {
        return new ToolHandler() {
            @Override
            public ToolDefinition getDefinition() {
                return ToolDefinition.builder()
                        .name("playwright_click")
                        .description("点击页面上的元素。通过 CSS 选择器定位元素并执行点击操作。")
                        .inputSchema(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "selector", Map.of(
                                                "type", "string",
                                                "description", "CSS 选择器，例如：#submit-btn、.login-button、button[type='submit']"
                                        ),
                                        "timeout", Map.of(
                                                "type", "number",
                                                "description", "等待元素出现的超时时间（毫秒），默认 30000"
                                        )
                                ),
                                "required", List.of("selector")
                        ))
                        .build();
            }

            @Override
            public ToolResult execute(Map<String, Object> arguments) {
                try {
                    String selector = (String) arguments.get("selector");
                    boolean success = playwrightService.click(selector);
                    return success ? ToolResult.text("成功点击元素: " + selector) : ToolResult.error("点击失败");
                } catch (Exception e) {
                    return ToolResult.error("点击元素时发生错误: " + e.getMessage());
                }
            }
        };
    }

    public ToolHandler type() {
        return new ToolHandler() {
            @Override
            public ToolDefinition getDefinition() {
                return ToolDefinition.builder()
                        .name("playwright_type")
                        .description("在输入框中输入文本。通过 CSS 选择器定位输入框并输入指定内容。")
                        .inputSchema(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "selector", Map.of(
                                                "type", "string",
                                                "description", "CSS 选择器，例如：#username、input[name='email']、.search-input"
                                        ),
                                        "text", Map.of(
                                                "type", "string",
                                                "description", "要输入的文本内容"
                                        ),
                                        "clear", Map.of(
                                                "type", "boolean",
                                                "description", "是否先清空输入框（默认 true）"
                                        )
                                ),
                                "required", List.of("selector", "text")
                        ))
                        .build();
            }

            @Override
            public ToolResult execute(Map<String, Object> arguments) {
                try {
                    String selector = (String) arguments.get("selector");
                    String text = (String) arguments.get("text");
                    boolean success = playwrightService.type(selector, text);
                    return success ? ToolResult.text("成功输入文本: " + text) : ToolResult.error("输入失败");
                } catch (Exception e) {
                    return ToolResult.error("输入文本时发生错误: " + e.getMessage());
                }
            }
        };
    }

    public ToolHandler closeBrowser() {
        return new ToolHandler() {
            @Override
            public ToolDefinition getDefinition() {
                return ToolDefinition.builder()
                        .name("playwright_close_browser")
                        .description("关闭浏览器。结束浏览器会话并释放相关资源。")
                        .inputSchema(Map.of("type", "object", "properties", Map.of()))
                        .build();
            }

            @Override
            public ToolResult execute(Map<String, Object> arguments) {
                try {
                    playwrightService.closeBrowser();
                    return ToolResult.text("浏览器已关闭");
                } catch (Exception e) {
                    return ToolResult.error("关闭浏览器时发生错误: " + e.getMessage());
                }
            }
        };
    }
}
