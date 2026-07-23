package io.yunxi.mcp.common.milvus;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Milvus 向量数据库连接配置
 * <p>
 * 提供 host/port/token/embedding 统一配置，
 * 各业务模块只需声明依赖即可复用同一套 Milvus 连接参数。
 * 配置前缀：{@code milvus}。
 * </p>
 *
 * @author yunxi-mcp-servers
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "milvus")
public class MilvusConfig {

    /** 是否启用 Milvus（关闭时向量功能降级） */
    private boolean enabled = true;

    /** Milvus 服务地址 */
    private String host = "localhost";

    /** Milvus 服务端口 */
    private Integer port = 19530;

    /** Milvus 认证 Token（云平台使用，本地可空） */
    private String token = "";

    /** 嵌入模型配置（维度必须与 Milvus 集合 schema 一致） */
    private Embedding embedding = new Embedding();

    public static class Embedding {
        /** 嵌入模型名称 */
        private String model = "text-embedding-v3";
        /** 向量维度 */
        private int dimension = 1024;
        /** API Key（如 DashScope API Key） */
        private String apiKey = "";

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public int getDimension() { return dimension; }
        public void setDimension(int dimension) { this.dimension = dimension; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public Integer getPort() { return port; }
    public void setPort(Integer port) { this.port = port; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public Embedding getEmbedding() { return embedding; }
    public void setEmbedding(Embedding embedding) { this.embedding = embedding; }
}
