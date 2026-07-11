package io.yunxi.mcp.common.config;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.yunxi.mcp.common.handler.ToolHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * MCP Server 工厂（基于官方 MCP SDK 0.17.0）
 * <p>
 * 自动发现所有 {@link ToolHandler} Spring Bean，创建 McpSyncServer 实例，
 * 通过 SSE 传输协议暴露工具供 AI Agent 调用。
 * </p>
 *
 * <h3>使用方式</h3>
 * <p>各 MCP 服务器模块只需：</p>
 * <ol>
 * <li>实现 {@link ToolHandler} 接口并标注 {@code @Component}</li>
 * <li>在 {@code application.yml} 中配置端口和服务器名称</li>
 * <li>添加此模块的 Spring 自动配置依赖</li>
 * </ol>
 *
 * <pre>{@code
 * // application.yml
 * mcp:
 *   server:
 *     name: yunxi-mcp-database
 *     version: 1.0.0
 * server:
 *   port: 40101
 * }</pre>
 *
 * @version 2.0.0
 * @since 2.0.0
 */
@Configuration
public class McpServerFactory {

    private static final Logger log = LoggerFactory.getLogger(McpServerFactory.class);

    @Value("${mcp.server.name:yunxi-mcp-server}")
    private String serverName;

    @Value("${mcp.server.version:1.0.0}")
    private String serverVersion;

    @Value("${server.port:8080}")
    private int serverPort;

    /**
     * 创建 SSE 传输提供者
     * <p>
     * 官方 MCP SDK 的 SSE 传输实现，处理 /mcp/sse（建立 SSE 连接）和
     * /mcp/message（发送 JSON-RPC 消息）两个端点。
     * </p>
     */
    @Bean
    public HttpServletSseServerTransportProvider mcpTransportProvider() {
        return HttpServletSseServerTransportProvider.builder()
                .jsonMapper(McpJsonMapper.createDefault())
                .baseUrl("http://localhost:" + serverPort)
                .sseEndpoint("/mcp/sse")
                .messageEndpoint("/mcp/message")
                .build();
    }

    /**
     * 创建 McpSyncServer Bean
     * <p>
     * 自动发现所有 ToolHandler 实现并注册为 MCP 工具。
     * </p>
     *
     * @param transportProvider SSE 传输提供者
     * @param toolHandlers      所有 ToolHandler Spring Bean
     * @return McpSyncServer 实例
     */
    @Bean(destroyMethod = "close")
    public McpSyncServer mcpSyncServer(
            HttpServletSseServerTransportProvider transportProvider,
            List<ToolHandler> toolHandlers) {

        McpSyncServer server = McpServer.sync(transportProvider)
                .serverInfo(serverName, serverVersion)
                .build();

        log.info("MCP Server 初始化完成: name={}, version={}, port={}",
                serverName, serverVersion, serverPort);

        // 自动注册所有 ToolHandler
        for (ToolHandler handler : toolHandlers) {
            registerTool(server, handler);
        }

        log.info("MCP Server 工具注册完成: 共 {} 个工具", toolHandlers.size());
        return server;
    }

    /**
     * 将 ToolHandler 注册到 McpSyncServer
     * <p>
     * 创建 SDK 的 SyncToolSpecification，将 ToolHandler 适配为
     * 官方 MCP SDK 的工具规范。
     * </p>
     */
    private void registerTool(McpSyncServer server, ToolHandler handler) {
        try {
            McpSchema.Tool tool = McpSchema.Tool.builder()
                    .name(handler.getName())
                    .description(handler.getDescription())
                    .inputSchema(convertInputSchema(handler.getInputSchema()))
                    .build();

            McpServerFeatures.SyncToolSpecification spec = new McpServerFeatures.SyncToolSpecification(
                    tool,
                    (exchange, params) -> executeHandler(handler, params)
            );

            server.addTool(spec);
            log.info("注册 MCP 工具: {}", handler.getName());
        } catch (Exception e) {
            log.error("注册工具失败: {}, 错误: {}", handler.getName(), e.getMessage());
        }
    }

    /**
     * 执行 ToolHandler 并包装为 SDK 的 CallToolResult
     */
    private McpSchema.CallToolResult executeHandler(ToolHandler handler, Map<String, Object> arguments) {
        try {
            String result = handler.execute(arguments != null ? arguments : Map.of());
            return McpSchema.CallToolResult.builder()
                    .content(List.of(new McpSchema.TextContent(result)))
                    .isError(false)
                    .build();
        } catch (Exception e) {
            log.error("工具执行失败: {}", handler.getName(), e);
            return McpSchema.CallToolResult.builder()
                    .content(List.of(new McpSchema.TextContent(
                            "Error: " + e.getMessage())))
                    .isError(true)
                    .build();
        }
    }

    /**
     * 将 ToolHandler 的 JSON Schema Map 转换为 SDK 格式
     */
    @SuppressWarnings("unchecked")
    private McpSchema.JsonSchema convertInputSchema(Map<String, Object> schema) {
        if (schema == null || schema.isEmpty()) {
            return new McpSchema.JsonSchema("object", Map.of(), List.of(), null, null, null);
        }

        String type = (String) schema.getOrDefault("type", "object");
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        List<String> required = (List<String>) schema.get("required");

        Map<String, Object> props = properties != null ? convertProperties(properties) : Map.of();

        return new McpSchema.JsonSchema(
                type,
                props,
                required != null ? required : List.of(),
                null,
                null,
                null);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> convertProperties(Map<String, Object> properties) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            Map<String, Object> propDef = (Map<String, Object>) entry.getValue();
            String propType = (String) propDef.getOrDefault("type", "string");
            String propDesc = (String) propDef.get("description");

            Map<String, Object> prop = new java.util.LinkedHashMap<>();
            prop.put("type", propType);
            if (propDesc != null) {
                prop.put("description", propDesc);
            }
            result.put(entry.getKey(), prop);
        }
        return result;
    }
}
