package io.yunxi.mcp.common.constants;

/**
 * MCP 协议常量定义
 * <p>
 * 定义 MCP (Model Context Protocol) 协议相关的常量，包括版本号和协议规范引用。
 * </p>
 *
 * <h3>MCP 协议版本说明</h3>
 * <p>
 * MCP 协议使用日期格式 {@code YYYY-MM-DD} 作为版本号，每个版本代表协议的某个稳定状态。
 * 服务器在响应 {@code initialize} 请求时必须声明它支持的协议版本。
 * </p>
 *
 * <h3>版本命名规则</h3>
 * <ul>
 *   <li>格式：{@code YYYY-MM-DD}</li>
 *   <li>含义：表示该版本的发布日期或最后一次更新的日期</li>
 *   <li>用途：用于协议兼容性检查和版本协商</li>
 * </ul>
 *
 * <h3>当前支持的版本</h3>
 * <ul>
 *   <li>{@link #PROTOCOL_VERSION} - 2024-11-05: 当前 MCP 协议的稳定版本</li>
 * </ul>
 *
 * <h3>协议规范参考</h3>
 * <ul>
 *   <li><a href="https://spec.modelcontextprotocol.io/">MCP 官方规范</a></li>
 *   <li><a href="https://github.com/modelcontextprotocol">MCP GitHub</a></li>
 * </ul>
 *
 * @version 1.0.0
 * @since 1.0.0
 * @see <a href="https://spec.modelcontextprotocol.io/">MCP Specification</a>
 */
public final class McpConstants {

    /**
     * MCP 协议版本号
     * <p>
     * 当前支持的 MCP 协议版本为 {@code 2024-11-05}。
     * </p>
     * <p>
     * <b>版本历史</b>：
     * <ul>
     *   <li>2024-11-05 - 稳定的 MCP 协议版本，支持工具调用、资源访问等核心功能</li>
     * </ul>
     * </p>
     * <p>
     * <b>使用方式</b>：
     * <pre>{@code
     * // 在 initialize 响应中声明协议版本
     * Map<String, Object> result = new HashMap<>();
     * result.put("protocolVersion", McpConstants.PROTOCOL_VERSION);
     * }</pre>
     * </p>
     * <p>
     * <b>版本兼容性</b>：
     * <ul>
     *   <li>服务器必须声明它支持的协议版本</li>
     *   <li>客户端应该检查协议版本以确保兼容性</li>
     *   <li>如果协议不兼容，客户端应终止连接或回退到兼容模式</li>
     * </ul>
     * </p>
     */
    public static final String PROTOCOL_VERSION = "2024-11-05";

    /**
     * MCP 协议规范 URL
     * <p>
     * MCP 协议官方规范文档的 URL。
     * </p>
     */
    public static final String SPEC_URL = "https://spec.modelcontextprotocol.io/";

    /**
     * JSON-RPC 版本号
     * <p>
     * MCP 协议基于 JSON-RPC 2.0 规范。
     * 所有请求和响应都必须包含 {@code "jsonrpc": "2.0"} 字段。
     * </p>
     *
     * @see <a href="https://www.jsonrpc.org/specification">JSON-RPC 2.0 Specification</a>
     */
    public static final String JSONRPC_VERSION = "2.0";

    /**
     * 私有构造函数，防止实例化
     * <p>
     * 这是一个纯常量类，不允许创建实例。
     * </p>
     */
    private McpConstants() {
        throw new UnsupportedOperationException("常量类不允许实例化");
    }
}
