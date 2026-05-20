package io.yunxi.mcp.s3;

import io.yunxi.mcp.common.server.McpHttpEndpoint;
import io.yunxi.mcp.common.server.McpSseEndpoint;
import io.yunxi.mcp.s3.tool.S3Tools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MCP S3 控制器
 * <p>
 * 提供 S3 操作的 MCP 端点，支持 HTTP 和 SSE 两种调用模式。
 * </p>
 *
 * <h3>支持的调用模式</h3>
 * <ul>
 * <li><b>HTTP 模式</b>: 直接 POST JSON-RPC 请求到 /mcp 端点</li>
 * <li><b>SSE 模式</b>: 通过 /mcp/sse 建立 SSE 连接，通过 /mcp/message 发送消息</li>
 * </ul>
 *
 * <h3>提供的工具</h3>
 * <ul>
 * <li>{@code s3_list_buckets} - 列出所有存储桶</li>
 * <li>{@code s3_list_objects} - 列出存储桶中的对象</li>
 * <li>{@code s3_put_object} - 上传对象到存储桶</li>
 * <li>{@code s3_get_object} - 下载存储桶中的对象</li>
 * <li>{@code s3_delete_object} - 删除存储桶中的对象</li>
 * </ul>
 *
 * <h3>测试端点</h3>
 * <ul>
 * <li>{@code GET /mcp/tools} - 获取可用工具列表（含参数Schema）</li>
 * <li>{@code GET /mcp/health} - 健康检查</li>
 * </ul>
 *
 * @see McpHttpEndpoint
 * @see McpSseEndpoint
 */
@Slf4j
@RestController
@RequestMapping("/mcp")
public class McpController {

    /**
     * HTTP 模式端点
     * <p>
     * 处理同步的 MCP 请求-响应模式。
     * </p>
     */
    private final McpHttpEndpoint httpEndpoint;

    /**
     * SSE 模式端点
     * <p>
     * 处理基于 Server-Sent Events 的流式通信模式。
     * </p>
     */
    private final McpSseEndpoint sseEndpoint;

    /**
     * SSE 连接处理器线程池
     * <p>
     * 用于执行 SSE 连接建立后的异步任务。
     * </p>
     */
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * 构造函数
     * <p>
     * 初始化 S3 客户端、HTTP 和 SSE 端点，并注册 S3 操作工具。
     * </p>
     *
     * @param accessKeyId     AWS Access Key ID，可通过配置项 aws.access-key-id 指定
     * @param secretAccessKey AWS Secret Access Key，可通过配置项 aws.secret-access-key 指定
     * @param region          AWS 区域，可通过配置项 aws.region 指定
     * @param endpoint        S3 兼容 endpoint（用于 MinIO、阿里云 OSS 等），可通过配置项 aws.endpoint
     *                        指定
     */
    public McpController(
            @Value("${aws.access-key-id:}") String accessKeyId,
            @Value("${aws.secret-access-key:}") String secretAccessKey,
            @Value("${aws.region:}") String region,
            @Value("${aws.endpoint:}") String endpoint) {

        log.info("初始化 MCP S3 控制器...");
        log.debug("AWS 区域: {}, Endpoint: {}", region, endpoint);

        // 构建 S3 客户端
        S3Client s3Client = buildS3Client(accessKeyId, secretAccessKey, region, endpoint);

        // 初始化 HTTP 和 SSE 端点
        this.httpEndpoint = new McpHttpEndpoint("yunxi-mcp-s3", "1.0.0");
        this.sseEndpoint = new McpSseEndpoint("yunxi-mcp-s3", "1.0.0");

        // 注册 S3 操作工具到 HTTP 端点
        httpEndpoint.registerTool(new S3Tools.ListBucketsTool(s3Client));
        httpEndpoint.registerTool(new S3Tools.ListObjectsTool(s3Client));
        httpEndpoint.registerTool(new S3Tools.PutObjectTool(s3Client));
        httpEndpoint.registerTool(new S3Tools.GetObjectTool(s3Client));
        httpEndpoint.registerTool(new S3Tools.DeleteObjectTool(s3Client));

        // 注册 S3 操作工具到 SSE 端点
        sseEndpoint.registerTool(new S3Tools.ListBucketsTool(s3Client));
        sseEndpoint.registerTool(new S3Tools.ListObjectsTool(s3Client));
        sseEndpoint.registerTool(new S3Tools.PutObjectTool(s3Client));
        sseEndpoint.registerTool(new S3Tools.GetObjectTool(s3Client));
        sseEndpoint.registerTool(new S3Tools.DeleteObjectTool(s3Client));

        log.info("MCP S3 控制器初始化完成，已注册 {} 个工具", 5);
    }

    /**
     * 构建 S3 客户端
     * <p>
     * 根据配置参数创建 S3Client 实例。支持以下认证方式：
     * <ul>
     * <li>静态凭证：使用 accessKeyId 和 secretAccessKey</li>
     * <li>默认凭证：从环境变量、AWS 配置文件或 ECS 任务角色获取</li>
     * </ul>
     * 同时支持指定自定义 endpoint（用于 S3 兼容存储服务）。
     * </p>
     *
     * @param accessKeyId     AWS Access Key ID
     * @param secretAccessKey AWS Secret Access Key
     * @param region          AWS 区域
     * @param endpoint        S3 兼容 endpoint
     * @return S3Client 实例
     */
    private S3Client buildS3Client(String accessKeyId, String secretAccessKey, String region, String endpoint) {
        var builder = S3Client.builder();

        // 配置凭证：优先使用静态凭证，否则使用默认凭证提供者
        if (accessKeyId != null && !accessKeyId.isBlank() && secretAccessKey != null && !secretAccessKey.isBlank()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId, secretAccessKey)));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        // 配置区域
        if (region != null && !region.isBlank()) {
            builder.region(Region.of(region));
        }

        // 配置自定义 endpoint（用于 S3 兼容存储）
        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        return builder.build();
    }

    // ============ HTTP 模式 ============

    /**
     * 处理 HTTP 模式的 MCP 请求
     *
     * @param requestJson JSON-RPC 请求 JSON 字符串
     * @return JSON-RPC 响应 JSON 字符串
     */
    @PostMapping
    public String handleHttpRequest(@RequestBody String requestJson) {
        log.debug("收到 HTTP MCP 请求: {}",
                requestJson.length() > 200 ? requestJson.substring(0, 200) + "..." : requestJson);
        String response = httpEndpoint.handleRequest(requestJson);
        log.debug("返回 HTTP MCP 响应: {}", response.length() > 200 ? response.substring(0, 200) + "..." : response);
        return response;
    }

    // ============ SSE 模式 ============

    /**
     * 建立 SSE 连接
     *
     * @return SSE 发射器
     */
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter handleSseConnection() {
        log.info("建立新的 SSE 连接");

        SseEmitter emitter = new SseEmitter(0L);

        executor.execute(() -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("endpoint")
                        .data("/mcp/message"));
                log.debug("SSE 连接已建立，消息端点: /mcp/message");

                emitter.onCompletion(() -> log.info("SSE 连接已关闭"));
                emitter.onTimeout(() -> log.warn("SSE 连接超时"));
                emitter.onError(e -> log.error("SSE 连接错误: {}", e.getMessage()));
            } catch (Exception e) {
                log.error("建立 SSE 连接失败: {}", e.getMessage(), e);
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * 处理 SSE 模式的消息
     *
     * @param requestJson JSON-RPC 请求 JSON 字符串
     * @return JSON-RPC 响应 JSON 字符串
     */
    @PostMapping("/message")
    public String handleSseMessage(@RequestBody String requestJson) {
        log.debug("收到 SSE 消息: {}", requestJson.length() > 200 ? requestJson.substring(0, 200) + "..." : requestJson);
        return sseEndpoint.handleMessage(requestJson);
    }

    // ============ 测试端点 ============

    /**
     * 获取可用工具列表
     * <p>
     * 返回所有已注册工具的名称、描述和参数 Schema。
     * 用于测试和调试 MCP 工具。
     * </p>
     *
     * @return 工具列表 Map
     */
    @GetMapping("/tools")
    public Map<String, Object> getTools() {
        log.debug("收到获取工具列表请求");
        List<String> toolNames = httpEndpoint.getTools().stream()
                .map(tool -> tool.getName())
                .toList();

        Map<String, Object> tools = Map.of(
                "tools", httpEndpoint.getTools().stream()
                        .map(tool -> Map.of(
                                "name", tool.getName(),
                                "description", tool.getDescription(),
                                "inputSchema", tool.getInputSchema()))
                        .toList());

        log.info("返回工具列表，共 {} 个工具: {}", toolNames.size(), toolNames);
        return tools;
    }

    // ============ 通用端点 ============

    /**
     * 获取服务器信息
     *
     * @return 服务器信息 Map
     */
    @GetMapping("/info")
    public Map<String, Object> getInfo() {
        return Map.of(
                "name", httpEndpoint.getServerInfo().get("name"),
                "version", httpEndpoint.getServerInfo().get("version"),
                "tools", httpEndpoint.getTools(),
                "modes", new String[] { "http", "sse" });
    }

    /**
     * 健康检查端点
     *
     * @return 健康状态
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP");
    }
}
