package io.yunxi.mcp.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import io.yunxi.mcp.common.server.McpHttpEndpoint;
import io.yunxi.mcp.common.server.McpSseEndpoint;
import io.yunxi.mcp.elasticsearch.tool.ElasticsearchTools;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MCP Elasticsearch 控制器
 * <p>
 * 支持 HTTP 和 SSE 两种模式的 Elasticsearch 操作控制器。
 * </p>
 *
 * <h3>提供的功能</h3>
 * <ul>
 * <li>搜索文档</li>
 * <li>索引文档</li>
 * <li>列出索引</li>
 * <li>删除文档</li>
 * </ul>
 *
 * <h3>测试端点</h3>
 * <ul>
 * <li>{@code GET /mcp/tools} - 获取可用工具列表（含参数Schema）</li>
 * <li>{@code GET /mcp/health} - 健康检查</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/mcp")
public class McpController {

    /**
     * HTTP 模式的 MCP 端点
     * <p>
     * 处理基于 HTTP 协议的 MCP 请求和响应。
     * </p>
     */
    private final McpHttpEndpoint httpEndpoint;

    /**
     * SSE 模式的 MCP 端点
     * <p>
     * 处理基于服务器推送事件（SSE）的 MCP 请求和响应。
     * </p>
     */
    private final McpSseEndpoint sseEndpoint;

    /**
     * 线程池
     * <p>
     * 用于异步处理 SSE 连接和消息发送。
     * </p>
     */
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * 构造函数
     * <p>
     * 初始化 Elasticsearch 客户端和 MCP 端点，注册所有工具。
     * </p>
     *
     * @param host Elasticsearch 主机地址，默认为 localhost
     * @param port Elasticsearch 端口，默认为 9200
     */
    public McpController(
            @Value("${elasticsearch.host:localhost}") String host,
            @Value("${elasticsearch.port:9200}") int port) {

        log.info("初始化 MCP Elasticsearch 控制器...");
        log.debug("Elasticsearch 连接: {}:{}", host, port);

        RestClient restClient = RestClient.builder(new HttpHost(host, port, "http")).build();
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        ElasticsearchClient client = new ElasticsearchClient(transport);

        this.httpEndpoint = new McpHttpEndpoint("yunxi-mcp-elasticsearch", "1.0.0");
        this.sseEndpoint = new McpSseEndpoint("yunxi-mcp-elasticsearch", "1.0.0");

        // 注册工具
        httpEndpoint.registerTool(new ElasticsearchTools.SearchTool(client));
        httpEndpoint.registerTool(new ElasticsearchTools.IndexDocumentTool(client));
        httpEndpoint.registerTool(new ElasticsearchTools.ListIndicesTool(client));
        httpEndpoint.registerTool(new ElasticsearchTools.DeleteDocumentTool(client));

        sseEndpoint.registerTool(new ElasticsearchTools.SearchTool(client));
        sseEndpoint.registerTool(new ElasticsearchTools.IndexDocumentTool(client));
        sseEndpoint.registerTool(new ElasticsearchTools.ListIndicesTool(client));
        sseEndpoint.registerTool(new ElasticsearchTools.DeleteDocumentTool(client));

        log.info("MCP Elasticsearch 控制器初始化完成，已注册 {} 个工具", 4);
    }

    // ============ HTTP 模式 ============

    /**
     * 处理 HTTP 请求
     * <p>
     * 接收 MCP JSON 格式的请求，通过 HTTP 端点处理并返回响应。
     * </p>
     *
     * @param requestJson JSON 格式的 MCP 请求字符串
     * @return JSON 格式的 MCP 响应字符串
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
     * 处理 SSE 连接
     * <p>
     * 建立 SSE 连接并发送消息端点信息。
     * </p>
     *
     * @return SSE 发送器对象，用于推送消息
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
     * 处理 SSE 消息
     * <p>
     * 接收通过 SSE 连接发送的 MCP 消息，通过 SSE 端点处理并返回响应。
     * </p>
     *
     * @param requestJson JSON 格式的 MCP 请求字符串
     * @return JSON 格式的 MCP 响应字符串
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
     * <p>
     * 返回 MCP 服务器的名称、版本、工具列表和支持的模式。
     * </p>
     *
     * @return 服务器信息 Map，包含 name、version、tools 和 modes
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
     * <p>
     * 返回服务器的健康状态。
     * </p>
     *
     * @return 健康状态 Map，包含 status 字段
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP");
    }
}
