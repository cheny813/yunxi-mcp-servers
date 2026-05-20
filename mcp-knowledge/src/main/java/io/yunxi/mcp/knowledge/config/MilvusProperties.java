package io.yunxi.mcp.knowledge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Milvus 向量数据库配置属性
 * <p>
 * 配置 Milvus 连接参数，用于向量存储和语义检索。
 * </p>
 * <p>
 * 配置前缀：{@code milvus}
 * </p>
 *
 * @author yunxi-mcp-servers
 */
@Data
@Component
@ConfigurationProperties(prefix = "milvus")
public class MilvusProperties {

    /** Milvus 服务主机地址 */
    private String host = "localhost";

    /** Milvus 服务端口 */
    private Integer port = 19530;

    /** 知识库集合名称 */
    private String collectionName = "knowledge_base";
}