package io.yunxi.mcp.memory.embedding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mock 向量嵌入提供者（用于测试）
 * <p>
 * 基于 SHA-256 哈希的模拟嵌入服务，主要用于开发和测试环境。
 * 不依赖任何外部 API，通过对文本进行哈希运算生成确定性的向量。
 * </p>
 * <p>
 * 特点：
 * </p>
 * <ul>
 *   <li>确定性：相同文本总是产生相同的向量</li>
 *   <li>独立性：不依赖任何外部服务</li>
 *   <li>可配置：向量维度可通过配置自定义</li>
 * </ul>
 * <p>
 * 配置示例（application.yml）：
 * </p>
 * <pre>
 * embedding:
 *   provider: mock
 *   mock:
 *     dimension: 1024
 * </pre>
 * <p>
 * 激活条件：通过配置 {@code embedding.provider=mock} 启用。
 * </p>
 * <p>
 * 注意事项：
 * </p>
 * <ul>
 *   <li>仅用于本地开发和测试，不要在生产环境使用</li>
 *   <li>生成的向量不具备语义意义，仅用于验证流程</li>
 * </ul>
 *
 * @author yunxi-mcp-servers
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "embedding.provider", havingValue = "mock")
public class MockEmbeddingProvider implements EmbeddingProvider {

    /**
     * 向量维度
     * 可通过配置项 embedding.mock.dimension 指定，默认 1024
     */
    @Value("${embedding.mock.dimension:1024}")
    private int dimension;

    @Override
    public String getName() {
        return "Mock";
    }

    @Override
    public int getDimension() {
        return dimension;
    }

    @Override
    public int getBatchSize() {
        return 100;
    }

    /**
     * 将文本转换为向量
     * <p>
     * 生成算法：
     * </p>
     * <ol>
     *   <li>对输入文本进行 UTF-8 编码</li>
     *   <li>计算 SHA-256 哈希值（32 字节）</li>
     *   <li>将哈希值循环展开为指定维度的向量</li>
     *   <li>将字节值 [0,255] 映射到浮点数 [-1,1]</li>
     * </ol>
     *
     * @param text 输入文本
     * @return 指定维度的浮点向量，失败时返回全零向量
     */
    @Override
    public List<Float> embed(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<Float> embedding = new ArrayList<>(dimension);
        try {
            // 计算 SHA-256 哈希
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(text.getBytes("UTF-8"));
            // 循环展开为指定维度的向量
            for (int i = 0; i < dimension; i++) {
                int idx = i % hash.length;
                int val = hash[idx] & 0xFF;
                // 将 [0,255] 映射到 [-1,1]
                embedding.add((val - 128) / 128.0f);
            }
        } catch (Exception e) {
            log.error("生成 mock 向量失败: {}", e.getMessage());
            // 发生异常时返回全零向量
            for (int i = 0; i < dimension; i++) {
                embedding.add(0.0f);
            }
        }
        return embedding;
    }

    /**
     * 批量将多个文本转换为向量
     *
     * @param texts 文本列表
     * @return 向量列表（与输入文本一一对应）
     */
    @Override
    public List<List<Float>> embedBatch(List<String> texts) {
        List<List<Float>> results = new ArrayList<>();
        for (String text : texts) {
            results.add(embed(text));
        }
        return results;
    }
}
