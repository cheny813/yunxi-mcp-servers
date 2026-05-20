package io.yunxi.mcp.api;

import io.yunxi.mcp.common.server.AbstractMcpServer;
import io.yunxi.mcp.api.tool.HttpGetTool;
import io.yunxi.mcp.api.tool.HttpPostTool;

/**
 * API Gateway MCP 服务器 (Stdio 模式)
 * <p>
 * 本模块为 MCP 客户端提供 HTTP API 调用能力，让 AI Agent 可以通过 stdio 模式
 * 发起任意的 HTTP GET/POST 请求。
 * </p>
 *
 * <h3>架构说明</h3>
 * <p>
 * 本服务仅支持 Stdio 模式，不提供 HTTP/SSE 远程调用能力。原因如下：
 * </p>
 * <ul>
 *   <li>本服务的核心功能是"代理 HTTP 请求"，作为 AI Agent 的 HTTP 工具</li>
 *   <li>远程调用应直接访问目标服务，无需经过此网关转发</li>
 *   <li>保持架构简洁，避免不必要的转发层</li>
 * </ul>
 *
 * <h3>提供的工具</h3>
 * <ul>
 *   <li>{@code http_get} - 发起 HTTP GET 请求</li>
 *   <li>{@code http_post} - 发起 HTTP POST 请求</li>
 * </ul>
 *
 * <h3>使用方式</h3>
 * <pre>
 * # 方式1: 直接运行 (Stdio 模式)
 * java -jar mcp-api-gateway-1.0.0.jar
 *
 * # 方式2: 配置到 Claude Desktop
 * {
 *   "mcpServers": {
 *     "api-gateway": {
 *       "command": "java",
 *       "args": ["-jar", "mcp-api-gateway-1.0.0.jar"]
 *     }
 *   }
 * }
 * </pre>
 *
 * @see io.yunxi.mcp.api.tool.HttpGetTool
 * @see io.yunxi.mcp.api.tool.HttpPostTool
 */
public class ApiGatewayMcpServer extends AbstractMcpServer {

    /**
     * 构造函数
     * <p>
     * 创建 API Gateway MCP 服务器实例，并注册可用的 HTTP 工具。
     * </p>
     *
     * @param name    服务器名称
     * @param version 服务器版本号
     */
    public ApiGatewayMcpServer(String name, String version) {
        super(name, version);

        // 注册工具
        registerTool(new HttpGetTool());
        registerTool(new HttpPostTool());
    }

    /**
     * 主入口
     * <p>
     * 启动 Stdio 模式的 MCP 服务器，开始监听标准输入。
     * 服务器会一直运行直到接收到关闭信号或发生错误。
     * </p>
     *
     * @param args 命令行参数（未使用）
     */
    public static void main(String[] args) {
        ApiGatewayMcpServer server = new ApiGatewayMcpServer(
                "yunxi-mcp-api-gateway",
                "1.0.0"
        );

        server.start();
    }
}
