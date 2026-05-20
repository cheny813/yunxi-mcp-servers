package io.yunxi.mcp.memory.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Milvus 向量数据库配置属性
 * <p>
 * 配置 Milvus 连接参数，用于向量存储和语义检索。
 * </p>
 * <p>
 * 配置前缀：{@code milvus}
 * </p>
 * <p>
 * 激活条件：需要设置 {@code milvus.enabled=true} 才能启用。
 * </p>
 *
 * @author yunxi-mcp-servers
 */
@Data
@ConfigurationProperties(prefix = "milvus")
public class MilvusProperties {

    /** 是否启用 Milvus */
    public boolean enabled = false;

    /** Milvus 服务地址（废弃，请使用 uri） */
    public String host;

    /** Milvus 服务端口（废弃，请使用 uri） */
    public int port = 19530;

    /** 完整 URI（优先级高于 host+port），优先使用环境变量 MILVUS_URI 配置 */
    public String uri;

    /** 认证 Token（用于云服务或启用了认证的 Milvus） */
    public String token = "";

    /** 记忆集合名称 */
    public String collectionName = "agent_memories";

    /** 向量维度（需与嵌入服务 Provider 匹配） */
    public int embeddingDimension = 1024;
}
