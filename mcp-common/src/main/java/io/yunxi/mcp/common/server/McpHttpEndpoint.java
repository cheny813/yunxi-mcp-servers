package io.yunxi.mcp.common.server;

import io.yunxi.mcp.common.protocol.McpError;
import io.yunxi.mcp.common.protocol.McpRequest;
import io.yunxi.mcp.common.protocol.McpResponse;

/**
 * MCP Server HTTP 端点
 * <p>
 * 提供基于 HTTP 的 MCP (Model Context Protocol) 服务端点，支持通过 HTTP 请求进行 MCP 协议通信。
 * </p>
 * <p>
 * HTTP 模式允许客户端通过 HTTP POST 请求发送 JSON-RPC 消息，适用于远程调用场景。
 * 相比 stdio 模式，HTTP 模式具有以下优势：
 * <ul>
 * <li>支持跨机器、跨网络的远程调用</li>
 * <li>可以集成到 Web 框架中（如 Spring Boot）</li>
 * <li>支持负载均衡和反向代理</li>
 * <li>便于调试和监控</li>
 * </ul>
 * </p>
 * <p>
 * 使用示例：
 * 
 * <pre>
 * {
 * &#64;code
 * &#64;RestController
 * &#64;RequestMapping("/mcp")
 * public class McpController {
 * private final McpHttpEndpoint endpoint;
 *
 * public McpController() {
 * this.endpoint = new McpHttpEndpoint("my-server", "1.0.0");
 * endpoint.registerTool(new MyTool());
 * }
 *
 * @PostMapping
 * public String handle(@RequestBody String json) {
 * return endpoint.handleRequest(json);
 * }
 * }
 * }
 * </pre>
 * </p>
 * <p>
 * HTTP 请求格式：
 * 
 * <pre>{@code
 * POST /mcp
 * Content-Type: application/json
 * {
 * "jsonrpc": "2.0",
 * "id": 1,
 * "method": "tools/call",
 * "params": {
 * "name": "my_tool",
 * "arguments": {...}
 * }
 * }
 * }</pre>
 * </p>
 *

 * @version 1.0.0
 * @since 1.0.0
 */
public class McpHttpEndpoint extends AbstractMcpEndpoint {

    /**
     * 构造 HTTP 端点
     *
     * @param name    服务器名称
     * @param version 服务器版本
     */
    public McpHttpEndpoint(String name, String version) {
        super(name, version);
    }

    /**
     * 处理 HTTP 模式的 MCP 请求
     * <p>
     * 接收 JSON 格式的请求字符串，解析后分发到相应的处理方法，
     * 并将响应序列化为 JSON 字符串返回。
     * </p>
     *
     * @param requestJson JSON-RPC 请求字符串
     * @return JSON-RPC 响应字符串
     */
    public String handleRequest(String requestJson) {
        return handleRawRequest(requestJson);
    }
}
