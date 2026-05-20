package io.yunxi.mcp.common.server;

import io.yunxi.mcp.common.protocol.McpError;
import io.yunxi.mcp.common.protocol.McpRequest;
import io.yunxi.mcp.common.protocol.McpResponse;

/**
 * MCP Server SSE 端点
 * <p>
 * 提供基于 SSE (Server-Sent Events) 的 MCP (Model Context Protocol) 服务端点。
 * SSE 是一种服务器向客户端推送实时数据的技术标准，适用于远程调用场景。
 * </p>
 * <p>
 * SSE 模式的通信流程：
 * <ol>
 * <li>客户端通过 GET 请求建立 SSE 连接（如 {@code GET /mcp/sse}）</li>
 * <li>服务器发送 {@code endpoint} 事件，告知客户端消息发送的端点 URL</li>
 * <li>客户端通过 POST 请求发送 JSON-RPC 消息到消息端点</li>
 * <li>服务器通过 SSE 事件流推送响应给客户端</li>
 * </ol>
 * </p>
 * <p>
 * SSE 事件格式：
 * 
 * <pre>{@code
 * // 连接建立时发送
 * event: endpoint
 * data: /mcp/message
 * 
 * 
 * // 响应消息
 * event: message
 * data: {"jsonrpc":"2.0","id":1,"result":{...}}
 * 
 * }</pre>
 * </p>
 * <p>
 * 使用示例：
 * 
 * <pre>
 * {
 *     &#64;code
 *     &#64;RestController
 *     &#64;RequestMapping("/mcp")
 *     public class McpController {
 *         private final McpSseEndpoint sseEndpoint;
 *         private final ExecutorService executor = Executors.newCachedThreadPool();
 *
 *         public McpController() {
 *             this.sseEndpoint = new McpSseEndpoint("my-server", "1.0.0");
 *             sseEndpoint.registerTool(new MyTool());
 *         }
 *
 *         @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
 *         public SseEmitter handleSse() {
 *             SseEmitter emitter = new SseEmitter(0L);
 *             executor.execute(() -> {
 *                 try {
 *                     emitter.send(SseEmitter.event()
 *                             .name("endpoint")
 *                             .data("/mcp/message"));
 *                 } catch (Exception e) {
 *                     emitter.completeWithError(e);
 *                 }
 *             });
 *             return emitter;
 *         }
 *
 *         &#64;PostMapping("/message")
 *         public String handleMessage(@RequestBody String json) {
 *             return sseEndpoint.handleMessage(json);
 *         }
 *     }
 * }
 * </pre>
 * </p>
 * <p>
 * 客户端实现示例：
 * 
 * <pre>{@code
 * // 1. 建立 SSE 连接
 * const eventSource = new EventSource('/mcp/sse');
 *
 * // 2. 接收 endpoint 事件
 * eventSource.addEventListener('endpoint', (event) => {
 *     this.messageEndpoint = event.data;
 * });
 *
 * // 3. 接收响应消息
 * eventSource.addEventListener('message', (event) => {
 *     const response = JSON.parse(event.data);
 *     // 处理响应
 * });
 *
 * // 4. 发送请求
 * async function sendRequest(request) {
 *     const response = await fetch(this.messageEndpoint, {
 *         method: 'POST',
 *         body: JSON.stringify(request)
 *     });
 *     return response.text();
 * }
 * }</pre>
 * </p>
 *
 * 
 * @since 1.0.0
 */
public class McpSseEndpoint extends AbstractMcpEndpoint {

    /**
     * 消息端点 URL
     * 客户端通过此端点发送 JSON-RPC 请求
     */
    protected final String messageEndpoint;

    /**
     * 构造 SSE 端点
     *
     * @param name    服务器名称
     * @param version 服务器版本
     */
    public McpSseEndpoint(String name, String version) {
        this(name, version, "/message");
    }

    /**
     * 构造 SSE 端点
     *
     * @param name            服务器名称
     * @param version         服务器版本
     * @param messageEndpoint 消息端点 URL 路径（默认为 "/message"）
     */
    public McpSseEndpoint(String name, String version, String messageEndpoint) {
        super(name, version);
        this.messageEndpoint = messageEndpoint;
    }

    /**
     * 获取 SSE endpoint 事件
     * <p>
     * 返回用于告知客户端消息发送端点的事件字符串。
     * 客户端建立 SSE 连接后，首先会收到此事件。
     * </p>
     *
     * @return SSE 格式的 endpoint 事件字符串
     *         格式：{@code event: endpoint\ndata: /mcp/message\n\n}
     */
    public String getSseEndpointEvent() {
        return "event: endpoint\ndata: " + messageEndpoint + "\n\n";
    }

    /**
     * 处理 SSE 模式的 MCP 请求
     * <p>
     * 客户端通过消息端点发送 JSON-RPC 请求，此方法处理后返回 SSE 格式的响应。
     * </p>
     *
     * @param requestJson JSON-RPC 请求字符串
     * @return SSE 格式的响应字符串
     */
    public String handleMessage(String requestJson) {
        return formatSseMessage(handleRawRequest(requestJson));
    }

    /**
     * 格式化 SSE 消息
     * <p>
     * 将 JSON 字符串包装为 SSE 事件格式。
     * </p>
     *
     * @param data JSON 数据字符串
     * @return SSE 格式的事件字符串，格式：{@code event: message\ndata: {json}\n\n}
     */
    protected String formatSseMessage(String data) {
        return "event: message\ndata: " + data + "\n\n";
    }

    /**
     * 获取消息端点 URL
     *
     * @return 消息端点 URL 路径
     */
    public String getMessageEndpoint() {
        return messageEndpoint;
    }
}
