package io.yunxi.mcp.common.server;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.yunxi.mcp.common.handler.ToolHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Stdio 模式 MCP 服务器抽象基类 (v2.0 - 基于官方 MCP SDK)
 * <p>
 * 封装官方 MCP SDK 的 {@link McpSyncServer} + {@link StdioServerTransportProvider}，
 * 子类只需通过 {@link #registerTool(ToolHandler)} 注册工具并调用 {@link #start()} 启动。
 * </p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * public class MyServer extends AbstractStdioMcpServer {
 *     public MyServer(String name, String version) {
 *         super(name, version);
 *         registerTool(new FooTool());
 *     }
 *
 *     public static void main(String[] args) {
 *         new MyServer("my-server", "1.0.0").start();
 *     }
 * }
 * }</pre>
 *
 * @version 2.0.0
 * @since 2.0.0
 */
public abstract class AbstractStdioMcpServer {

    private final String serverName;
    private final String serverVersion;
    private final List<ToolHandler> toolHandlers = new ArrayList<>();
    private McpSyncServer server;

    protected AbstractStdioMcpServer(String serverName, String serverVersion) {
        this.serverName = serverName;
        this.serverVersion = serverVersion;
    }

    /**
     * 注册一个 MCP 工具
     */
    protected void registerTool(ToolHandler handler) {
        this.toolHandlers.add(handler);
    }

    /**
     * 构建并启动 Stdio 模式的 MCP 服务器
     * <p>进程会一直运行直到收到关闭信号 (Ctrl+C) 或 JVM 退出。</p>
     */
    public void start() {
        StdioServerTransportProvider transportProvider =
                new StdioServerTransportProvider(McpJsonMapper.createDefault());

        var spec = McpServer.sync(transportProvider)
                .serverInfo(serverName, serverVersion);

        for (ToolHandler handler : toolHandlers) {
            spec = spec.tools(toSpecification(handler));
        }

        server = spec.build();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (server != null) {
                server.close();
            }
        }));

        // 保持主线程运行，监听 stdio
        Thread.currentThread().setName("mcp-stdio-server");
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 将 ToolHandler 适配为官方 SDK 的 SyncToolSpecification
     */
    private McpServerFeatures.SyncToolSpecification toSpecification(ToolHandler handler) {
        McpSchema.JsonSchema inputSchema = convertInputSchema(handler.getInputSchema());

        McpSchema.Tool tool = McpSchema.Tool.builder()
                .name(handler.getName())
                .description(handler.getDescription())
                .inputSchema(inputSchema)
                .build();

        return new McpServerFeatures.SyncToolSpecification(
                tool,
                (exchange, arguments) -> {
                    try {
                        String result = handler.execute(arguments != null ? arguments : Map.of());
                        return McpSchema.CallToolResult.builder()
                                .content(List.of(new McpSchema.TextContent(result)))
                                .isError(false)
                                .build();
                    } catch (Exception e) {
                        return McpSchema.CallToolResult.builder()
                                .content(List.of(new McpSchema.TextContent("Error: " + e.getMessage())))
                                .isError(true)
                                .build();
                    }
                }
        );
    }

    /**
     * 将 ToolHandler 的 JSON Schema Map 转换为 SDK 的 JsonSchema record (v2.0)
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
