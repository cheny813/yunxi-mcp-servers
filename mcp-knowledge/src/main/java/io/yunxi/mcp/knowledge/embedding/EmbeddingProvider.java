package io.yunxi.mcp.knowledge.embedding;

import java.util.List;

/**
 * 文本嵌入向量提供者接口
 *
 * @author yunxi-mcp-servers
 */
public interface EmbeddingProvider {

    /**
     * 获取提供者名称
     *
     * @return 提供者名称
     */
    String getName();

    /**
     * 获取向量维度
     *
     * @return 向量维度
     */
    int getDimension();

    /**
     * 将文本转换为向量
     *
     * @param text 输入文本
     * @return 浮点向量列表
     */
    List<Float> embed(String text);

    /**
     * 批量将多个文本转换为向量
     *
     * @param texts 文本列表
     * @return 向量列表
     */
    default List<List<Float>> embedBatch(List<String> texts) {
        return List.of();
    }

    /**
     * 获取批处理大小
     *
     * @return 批处理大小
     */
    default int getBatchSize() {
        return 10;
    }
}