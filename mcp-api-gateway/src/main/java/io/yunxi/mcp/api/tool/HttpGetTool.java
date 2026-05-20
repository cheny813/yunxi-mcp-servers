package io.yunxi.mcp.api.tool;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * HTTP GET 请求工具
 * <p>
 * 通过 MCP 协议发起 HTTP GET 请求，返回完整的响应信息。
 * 此工具仅在 Stdio 模式下可用，作为 AI Agent 的 HTTP 客户端工具。
 * </p>
 *
 * <h3>使用示例</h3>
 * <pre>
 * {
 *   "name": "http_get",
 *   "arguments": {
 *     "url": "https://api.example.com/data",
 *     "headers": {
 *       "Authorization": "Bearer token123"
 *     }
 *   }
 * }
 * </pre>
 *
 * @see HttpPostTool
 */
public class HttpGetTool implements ToolHandler {

    /**
     * HTTP 客户端
     * <p>
     * 用于发起 HTTP 请求的 OkHttpClient 实例。
     * 配置了连接超时 30 秒、读取超时 60 秒。
     * </p>
     */
    private final OkHttpClient client;

    /**
     * 构造函数
     * <p>
     * 创建 HttpGetTool 实例，初始化 HTTP 客户端。
     * </p>
     */
    public HttpGetTool() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 获取工具定义
     * <p>
     * 返回工具的元数据定义，包括名称、描述和输入参数 schema。
     * </p>
     *
     * @return 工具定义对象
     */
    @Override
    public ToolDefinition getDefinition() {
        return ToolDefinition.builder()
                .name("http_get")
                .description("Make an HTTP GET request and return the response")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "url", Map.of(
                                        "type", "string",
                                        "description", "URL to request"
                                ),
                                "headers", Map.of(
                                        "type", "object",
                                        "description", "Request headers (optional)",
                                        "additionalProperties", Map.of("type", "string")
                                )
                        ),
                        "required", List.of("url")
                ))
                .build();
    }

    /**
     * 执行 HTTP GET 请求
     * <p>
     * 根据传入的参数发起 HTTP GET 请求，并返回完整的响应信息。
     * </p>
     * <p>
     * 处理流程：
     * <ol>
     *   <li>验证 URL 参数是否提供</li>
     *   <li>构建 HTTP 请求（添加可选的请求头）</li>
     *   <li>发送请求并获取响应</li>
     *   <li>格式化响应信息并返回</li>
     * </ol>
     * </p>
     *
     * @param arguments 包含以下字段的 Map：
     *                  <ul>
     *                    <li>url (必填) - 请求的 URL 地址</li>
     *                    <li>headers (可选) - 请求头 Map</li>
     *                  </ul>
     * @return 工具执行结果，包含 HTTP 响应状态、响应头和响应体
     */
    @Override
    @SuppressWarnings("unchecked")
    public ToolResult execute(Map<String, Object> arguments) {
        // 从参数中提取 URL
        String url = (String) arguments.get("url");

        // 验证 URL 是否存在
        if (url == null || url.isBlank()) {
            return ToolResult.error("URL is required");
        }

        try {
            // 创建请求构建器，指定 URL
            Request.Builder requestBuilder = new Request.Builder().url(url);

            // 添加可选的请求头
            if (arguments.containsKey("headers")) {
                Map<String, String> headers = (Map<String, String>) arguments.get("headers");
                headers.forEach(requestBuilder::addHeader);
            }

            // 构建完整的请求对象
            Request request = requestBuilder.build();

            // 发起同步请求并获取响应
            try (Response response = client.newCall(request).execute()) {
                // 提取响应体内容
                String body = response.body() != null ? response.body().string() : "";

                // 构建格式化的响应输出
                StringBuilder sb = new StringBuilder();
                sb.append("HTTP GET: ").append(url).append("\n");
                sb.append("Status: ").append(response.code()).append(" ").append(response.message()).append("\n");
                sb.append("Headers:\n");
                // 遍历所有响应头并格式化输出
                response.headers().forEach(header ->
                        sb.append("  ").append(header.getFirst()).append(": ").append(header.getSecond()).append("\n"));
                sb.append("\nBody:\n").append(body);

                return ToolResult.text(sb.toString());
            }
        } catch (Exception e) {
            // 捕获所有异常并返回错误信息
            return ToolResult.error("HTTP Error: " + e.getMessage());
        }
    }
}
