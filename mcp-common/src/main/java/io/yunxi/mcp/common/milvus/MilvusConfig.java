package io.yunxi.mcp.common.milvus;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Milvus 向量数据库连接配置（全平台唯一配置源）。
 * <p>
 * 所有需要 Milvus 的 MCP 模块共用本配置，prefix 统一为 {@code milvus}。
 * 字段合并了各业务模块所需的全部连接参数与集合参数，避免同 prefix 多
 * {@code @ConfigurationProperties} bean 在聚合部署时互相覆盖的陷阱。
 * </p>
 * <p>
 * 启用语义：{@code milvus.enabled} 缺省为 {@code false}。只有显式声明
 * {@code milvus.enabled=true} 的模块才会创建 {@link io.milvus.v2.client.MilvusClientV2}
 * Bean，否则下游通过 {@code ObjectProvider} / {@code @Autowired(required = false)}
 * 拿到 {@code null}，向量功能优雅降级。
 * </p>
 *
 * @author yunxi-mcp-servers
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "milvus")
public class MilvusConfig {

    /** 是否启用 Milvus（关闭时向量功能降级，不创建客户端 Bean） */
    private boolean enabled = false;

    /** Milvus 服务地址（与 uri 二选一，uri 优先级更高） */
    private String host = "localhost";

    /** Milvus 服务端口 */
    private Integer port = 19530;

    /** 完整 URI（优先于 host+port；云平台常用，如 http://instance:19530） */
    private String uri;

    /** 数据库名称 */
    private String database = "default";

    /** 用户名（认证模式时使用） */
    private String username;

    /** 密码（认证模式时使用） */
    private String password;

    /** 认证 Token（替代用户名密码，优先级更高） */
    private String token = "";

    /** 连接超时（秒） */
    private int connectTimeout = 5;

    /** 查询超时（秒） */
    private int queryTimeout = 30;

    /** 集合名称（知识库 / 记忆等模块复用） */
    private String collectionName = "default_collection";

    /** 向量维度（需与嵌入服务 Provider 匹配） */
    private int embeddingDimension = 1024;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public Integer getPort() { return port; }
    public void setPort(Integer port) { this.port = port; }
    public String getUri() { return uri; }
    public void setUri(String uri) { this.uri = uri; }
    public String getDatabase() { return database; }
    public void setDatabase(String database) { this.database = database; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public int getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(int connectTimeout) { this.connectTimeout = connectTimeout; }
    public int getQueryTimeout() { return queryTimeout; }
    public void setQueryTimeout(int queryTimeout) { this.queryTimeout = queryTimeout; }
    public String getCollectionName() { return collectionName; }
    public void setCollectionName(String collectionName) { this.collectionName = collectionName; }
    public int getEmbeddingDimension() { return embeddingDimension; }
    public void setEmbeddingDimension(int embeddingDimension) { this.embeddingDimension = embeddingDimension; }

    /**
     * 解析最终连接 URI。
     * <p>显式 {@code uri} 优先；否则由 {@code host:port} 拼出。</p>
     */
    public String resolveUri() {
        if (uri != null && !uri.isBlank()) {
            return uri;
        }
        return "http://" + host + ":" + port;
    }
}
