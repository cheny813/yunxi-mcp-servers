package io.yunxi.mcp.memory.embedding;

import java.util.List;

/**
 * 向量嵌入服务提供者接口
 * <p>
 * 定义文本嵌入服务的标准接口，所有嵌入 Provider 实现类都需要实现此接口。
 * 目前支持的实现包括：
 * </p>
 * <ul>
 *   <li>{@link io.yunxi.mcp.memory.embedding.DashScopeEmbeddingProvider} - 阿里云 DashScope</li>
 *   <li>{@link io.yunxi.mcp.memory.embedding.OpenAIEmbeddingProvider} - OpenAI（占位实现）</li>
 *   <li>{@link io.yunxi.mcp.memory.embedding.MockEmbeddingProvider} - Mock 测试实现</li>
 * </ul>
 * <p>
 * 使用 Spring 的条件注解 {@code @ConditionalOnProperty} 来激活特定的 Provider：
 * </p>
 * <pre>
 * # 激活 DashScope（默认）
 * embedding.provider=dashscope
 *
 * # 激活 OpenAI
 * embedding.provider=openai
 *
 * # 激活 Mock（测试用）
 * embedding.provider=mock
 * </pre>
 *
 * @author yunxi-mcp-servers
 */
public interface EmbeddingProvider {

    /**
     * 获取 Provider 名称
     * <p>
     * 用于日志显示和配置验证。
     * </p>
     *
     * @return Provider 的显示名称（如 "DashScope"、"OpenAI"）
     */
    String getName();

    /**
     * 获取向量维度
     * <p>
     * 返回该 Provider 生成的向量维度。
     * 不同 Provider 可能支持不同的维度：
     * </p>
     * <ul>
     *   <li>DashScope text-embedding-v3: 1024 维（默认）</li>
     *   <li>OpenAI text-embedding-ada-002: 1536 维</li>
     * </ul>
     *
     * @return 向量维度
     */
    int getDimension();

    /**
     * 获取批量处理的建议批次大小
     * <p>
     * 不同 Provider 对批量处理有不同的限制和建议：
     * </p>
     * <ul>
     *   <li>DashScope: 25 条/批</li>
     *   <li>OpenAI: 2048 条/批</li>
     * </ul>
     *
     * @return 建议的批次大小
     */
    int getBatchSize();

    /**
     * 将单个文本转换为向量
     * <p>
     * 将输入文本转换为固定维度的浮点向量。
     * 通常用于单条文本的向量化场景，如添加记忆时对内容进行向量化。
     * </p>
     *
     * @param text 输入文本，不能为空
     * @return 向量列表，失败时返回空列表而非抛出异常
     */
    List<Float> embed(String text);

    /**
     * 批量将多个文本转换为向量
     * <p>
     * 一次性处理多条文本，通常比逐条调用 embed() 更高效。
     * 部分 Provider（如 DashScope）在服务端进行了批量优化。
     * </p>
     *
     * @param texts 文本列表，不能为空
     * @return 向量列表（与输入文本一一对应），失败时返回空列表
     */
    List<List<Float>> embedBatch(List<String> texts);
}
