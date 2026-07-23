package io.yunxi.mcp.common.embedding;

import java.util.Collections;
import java.util.List;

/**
 * 文本向量嵌入提供者 SPI 接口
 * <p>
 * 所有向量嵌入服务（DashScope、OpenAI、Mock 等）必须实现此接口。
 * 多个实现可通过 {@link EmbeddingService} 统一管理和路由。
 * </p>
 *
 * @author yunxi-mcp-servers
 */
public interface EmbeddingProvider {

    /**
     * 获取提供者名称
     *
     * @return 提供者名称，如 "DashScope"、"OpenAI"、"Mock"
     */
    String getName();

    /**
     * 获取提供者名称（别名，兼容 EmbeddingService 的路由查找）
     *
     * @return 提供者名称
     */
    default String getProviderName() {
        return getName();
    }

    /**
     * 获取模型名称（用于日志和监控）
     *
     * @return 模型名称，如 "text-embedding-v3"，默认 "unknown"
     */
    default String getModelName() {
        return "unknown";
    }

    /**
     * 获取向量维度
     *
     * @return 向量维度，如 DashScope text-embedding-v3 为 1024
     */
    int getDimension();

    /**
     * 获取批处理大小（每次批量请求的最大文本数）
     *
     * @return 批处理大小，默认 10
     */
    default int getBatchSize() {
        return 10;
    }

    /**
     * 将单个文本转换为向量
     *
     * @param text 输入文本
     * @return 浮点向量列表，失败时返回空列表
     */
    List<Float> embed(String text);

    /**
     * 批量将多个文本转换为向量
     * <p>
     * 默认逐条调用 {@link #embed(String)}，子类可覆盖为真正的批量请求以提升性能。
     * </p>
     *
     * @param texts 文本列表
     * @return 向量列表（与输入文本一一对应）
     */
    default List<List<Float>> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }
        java.util.ArrayList<List<Float>> results = new java.util.ArrayList<>();
        for (String text : texts) {
            results.add(embed(text));
        }
        return results;
    }
}
