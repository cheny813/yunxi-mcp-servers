package io.yunxi.mcp.common.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.yunxi.mcp.common.constants.McpConstants;
import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import io.yunxi.mcp.common.protocol.McpError;
import io.yunxi.mcp.common.protocol.McpRequest;
import io.yunxi.mcp.common.protocol.McpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP Server 抽象基类（Stdio 模式）
 * <p>
 * 实现基于标准输入输出（stdio）通信的 MCP (Model Context Protocol) 服务器。
 * </p>
 * <p>
 * MCP 协议定义了 AI Agent 与外部工具之间的通信标准。此类实现了基于 JSON-RPC 2.0 的 stdio 模式：
 * <ul>
 * <li>从标准输入读取 JSON-RPC 请求</li>
 * <li>处理请求并执行相应操作</li>
 * <li>将 JSON-RPC 响应写入标准输出</li>
 * </ul>
 * </p>
 * <p>
 * 支持的标准方法：
 * <ul>
 * <li>{@code initialize} - 初始化服务器连接，返回服务器信息和能力</li>
 * <li>{@code tools/list} - 列出所有可用的工具</li>
 * <li>{@code tools/call} - 调用指定工具执行操作</li>
 * <li>{@code ping} - 心跳检测，确认连接活跃</li>
 * </ul>
 * </p>
 * <p>
 * 使用示例：
 * 
 * <pre>{@code
 * public class MyMcpServer extends AbstractMcpServer {
 *
 *     public MyMcpServer() {
 *         super("my-mcp-server", "1.0.0");
 *         registerTool(new MyTool());
 *     }
 *
 *     public static void main(String[] args) {
 *         new MyMcpServer().start();
 *     }
 * }
 * }</pre>
 * </p>
 *

 * @version 1.0.0
 * @since 1.0.0
 */
public abstract class AbstractMcpServer {

    /**
     * 日志记录器
     * 使用 SLF4J 日志框架记录服务器运行状态和错误信息
     */
    protected final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * JSON 序列化/反序列化工具
     * 用于将 JSON-RPC 请求和响应与 Java 对象之间进行转换
     */
    protected final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 工具处理器映射表
     * 使用线程安全的 ConcurrentHashMap 存储工具名称与处理器对象的映射
     * Key: 工具名称 (tool name)
     * Value: 工具处理器实现
     */
    protected final Map<String, ToolHandler> toolHandlers = new ConcurrentHashMap<>();

    /**
     * 服务器信息
     * 包含服务器名称、版本等基本信息
     */
    protected final Map<String, Object> serverInfo = new LinkedHashMap<>();

    /**
     * 服务器能力声明
     * 声明服务器支持的功能和特性
     */
    protected final Map<String, Object> capabilities = new LinkedHashMap<>();

    /**
     * 服务器运行状态标志
     * 使用 volatile 关键字确保多线程环境下的可见性
     * true: 服务器运行中
     * false: 服务器已停止
     */
    private volatile boolean running = true;

    /**
     * 构造函数
     *
     * @param name    服务器名称，用于标识 MCP 服务器
     * @param version 服务器版本号，遵循语义化版本规范 (Semantic Versioning)
     * @throws IllegalArgumentException 如果 name 或 version 为 null 或空
     */
    protected AbstractMcpServer(String name, String version) {
        serverInfo.put("name", name);
        serverInfo.put("version", version);

        // 声明工具相关能力
        // listChanged: false 表示工具列表在运行期间不会变化
        capabilities.put("tools", Map.of("listChanged", false));
    }

    /**
     * 注册工具处理器
     * <p>
     * 将一个工具处理器注册到服务器，使其可以被客户端调用。
     * 每个工具必须有唯一的名称，重复注册会覆盖之前的处理器。
     * </p>
     *
     * @param handler 工具处理器实现，必须实现 ToolHandler 接口
     * @throws IllegalArgumentException 如果 handler 为 null
     * @see ToolHandler
     */
    public void registerTool(ToolHandler handler) {
        toolHandlers.put(handler.getName(), handler);
        log.info("注册工具: {}", handler.getName());
    }

    /**
     * 启动 MCP 服务器（Stdio 模式）
     * <p>
     * 服务器启动后会执行以下操作：
     * <ol>
     * <li>记录服务器启动信息</li>
     * <li>监听标准输入，读取客户端发送的 JSON-RPC 请求</li>
     * <li>解析请求并调用相应的方法处理</li>
     * <li>将处理结果序列化为 JSON 写入标准输出</li>
     * </ol>
     * </p>
     * <p>
     * 此方法会阻塞运行，直到调用 {@link #stop()} 方法或发生 IO 错误。
     * </p>
     * <p>
     * 支持的请求格式：
     * 
     * <pre>{@code
     * {
     *   "jsonrpc": "2.0",
     *   "id": 1,
     *   "method": "tools/call",
     *   "params": {
     *     "name": "my_tool",
     *     "arguments": {...}
     *   }
     * }
     * }</pre>
     * </p>
     */
    public void start() {
        log.info("MCP Server 启动: {}", serverInfo.get("name"));
        log.info("已注册工具数量: {}", toolHandlers.size());

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                PrintWriter writer = new PrintWriter(System.out, true)) {

            String line;
            // 循环读取标准输入，直到服务器停止或流关闭
            while (running && (line = reader.readLine()) != null) {
                try {
                    // 解析 JSON-RPC 请求
                    McpRequest request = objectMapper.readValue(line, McpRequest.class);
                    // 处理请求
                    McpResponse response = handleRequest(request);
                    // 序列化响应
                    String responseJson = objectMapper.writeValueAsString(response);
                    // 写入标准输出
                    writer.println(responseJson);
                } catch (Exception e) {
                    log.error("处理请求失败: {}", e.getMessage());
                    // 返回解析错误响应
                    McpResponse errorResponse = McpResponse.error(null, McpError.PARSE_ERROR,
                            "Parse error: " + e.getMessage());
                    writer.println(objectMapper.writeValueAsString(errorResponse));
                }
            }
        } catch (IOException e) {
            log.error("IO 错误: {}", e.getMessage());
        }
    }

    /**
     * 停止 MCP 服务器
     * <p>
     * 设置运行状态标志为 false，使服务器的主循环退出。
     * 调用此方法后，{@link #start()} 方法会在处理完当前请求后返回。
     * </p>
     * <p>
     * 注意：此方法不会立即关闭 I/O 流，而是优雅地停止消息处理循环。
     * </p>
     */
    public void stop() {
        running = false;
        log.info("MCP Server 停止");
    }

    /**
     * 处理 MCP JSON-RPC 请求
     * <p>
     * 根据请求的 method 字段分发到相应的处理方法：
     * <ul>
     * <li>{@code initialize} → {@link #handleInitialize(McpRequest)}</li>
     * <li>{@code tools/list} → {@link #handleToolsList(McpRequest)}</li>
     * <li>{@code tools/call} → {@link #handleToolsCall(McpRequest)}</li>
     * <li>{@code ping} → 返回成功响应</li>
     * <li>其他方法 → 返回 METHOD_NOT_FOUND 错误</li>
     * </ul>
     * </p>
     *
     * @param request JSON-RPC 请求对象
     * @return JSON-RPC 响应对象
     * @see McpRequest
     * @see McpResponse
     */
    protected McpResponse handleRequest(McpRequest request) {
        String method = request.getMethod();
        Object id = request.getId();

        log.debug("收到请求: method={}, id={}", method, id);

        return switch (method) {
            case "initialize" -> handleInitialize(request);
            case "tools/list" -> handleToolsList(request);
            case "tools/call" -> handleToolsCall(request);
            case "ping" -> McpResponse.success(id, Map.of());
            default -> McpResponse.error(id, McpError.METHOD_NOT_FOUND, "Method not found: " + method);
        };
    }

    /**
     * 处理初始化请求
     * <p>
     * initialize 方法是客户端连接后的第一个必需调用。
     * 服务器返回以下信息：
     * <ul>
     * <li>protocolVersion: 协议版本（使用 {@link McpConstants#PROTOCOL_VERSION}）</li>
     * <li>capabilities: 服务器能力声明</li>
     * <li>serverInfo: 服务器元信息（名称、版本等）</li>
     * </ul>
     * </p>
     *
     * @param request JSON-RPC 初始化请求对象
     * @return 包含服务器信息的成功响应
     */
    protected McpResponse handleInitialize(McpRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("protocolVersion", McpConstants.PROTOCOL_VERSION);
        result.put("capabilities", capabilities);
        result.put("serverInfo", serverInfo);
        return McpResponse.success(request.getId(), result);
    }

    /**
     * 处理工具列表请求
     * <p>
     * 返回所有已注册的工具的元数据，包括：
     * <ul>
     * <li>name: 工具名称</li>
     * <li>description: 工具描述</li>
     * <li>inputSchema: 工具参数的 JSON Schema 定义</li>
     * </ul>
     * </p>
     *
     * @param request JSON-RPC 工具列表请求对象
     * @return 包含工具列表的成功响应
     * @see ToolDefinition
     */
    protected McpResponse handleToolsList(McpRequest request) {
        List<ToolDefinition> tools = toolHandlers.values().stream()
                .map(ToolHandler::getDefinition)
                .toList();
        return McpResponse.success(request.getId(), Map.of("tools", tools));
    }

    /**
     * 处理工具调用请求
     * <p>
     * 执行指定工具并返回执行结果。处理流程：
     * <ol>
     * <li>验证请求参数完整性</li>
     * <li>查找指定的工具处理器</li>
     * <li>执行工具并获取结果</li>
     * <li>将结果包装为 MCP 标准响应格式</li>
     * </ol>
     * </p>
     * <p>
     * 如果工具执行过程中抛出异常，会将异常信息作为错误返回，
     * 同时设置 isError 标志为 true。
     * </p>
     *
     * @param request JSON-RPC 工具调用请求对象，必须包含以下字段：
     *                - name: 工具名称
     *                - arguments: 工具参数（可选）
     * @return 包含工具执行结果的响应，格式为：
     * 
     *         <pre>{@code
     *         {
     *           "content": [
     *             {
     *               "type": "text",
     *               "text": "执行结果"
     *             }
     *           ],
     *           "isError": false
     *         }
     *         }</pre>
     */
    @SuppressWarnings("unchecked")
    protected McpResponse handleToolsCall(McpRequest request) {
        Map<String, Object> params = request.getParams();
        if (params == null) {
            return McpResponse.error(request.getId(), McpError.INVALID_PARAMS, "Missing params");
        }

        String toolName = (String) params.get("name");
        if (toolName == null) {
            return McpResponse.error(request.getId(), McpError.INVALID_PARAMS, "Missing tool name");
        }

        ToolHandler handler = toolHandlers.get(toolName);
        if (handler == null) {
            return McpResponse.error(request.getId(), McpError.INVALID_PARAMS, "Unknown tool: " + toolName);
        }

        try {
            Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");
            if (arguments == null) {
                arguments = new HashMap<>();
            }

            ToolResult result = handler.execute(arguments);
            return McpResponse.success(request.getId(),
                    Map.of("content", result.getContent(), "isError", result.isError()));
        } catch (Exception e) {
            log.error("工具执行失败: {}", toolName, e);
            return McpResponse.success(request.getId(), Map.of(
                    "content", List.of(Map.of("type", "text", "text", "Error: " + e.getMessage())),
                    "isError", true));
        }
    }
}
