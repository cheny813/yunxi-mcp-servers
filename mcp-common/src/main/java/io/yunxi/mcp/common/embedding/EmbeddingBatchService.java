package io.yunxi.mcp.common.embedding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 批量向量嵌入服务（带分批 + 指数退避重试 + 零向量兜底）
 * <p>
 * 合并了原先 nutrition 模块中两版 EmbeddingBatchService 的功能：
 * <ul>
 *   <li>自动按 {@code embedding.batch-size} 拆分超大批次</li>
 *   <li>每批次最多重试 {@code embedding.max-retries} 次（指数退避）</li>
 *   <li>最终失败的批次以零向量占位，保持索引对齐</li>
 * </ul>
 * </p>
 *
 * @author yunxi-mcp-servers
 * @since 2.0.0
 */
@Service
public class EmbeddingBatchService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingBatchService.class);

    private final EmbeddingService embeddingService;

    @Value("${embedding.batch-size:20}")
    private int batchSize;

    @Value("${embedding.max-retries:3}")
    private int maxRetries;

    @Value("${embedding.initial-retry-delay-ms:2000}")
    private long initialRetryDelayMs;

    public EmbeddingBatchService(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    /**
     * 返回当前批次大小配置
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * 分批调用 embedding API（带重试 + 零向量兜底）
     * <p>
     * 将 texts 按 {@link #batchSize} 切分为子批次，
     * 每批次调用 {@link #embedBatchWithRetryInternal(List)} 执行；
     * 最终失败的批次以零向量占位，保持输出索引与输入对齐。
     * </p>
     *
     * @param texts 文本列表
     * @return 向量列表（与输入索引对齐）
     */
    public List<List<Float>> embedBatchWithRetry(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        List<List<Float>> allEmbeddings = new ArrayList<>();
        for (int i = 0; i < texts.size(); i += batchSize) {
            int end = Math.min(i + batchSize, texts.size());
            List<String> batch = texts.subList(i, end);
            log.info("embedding 进度: {}/{}", end, texts.size());

            List<List<Float>> vectors = embedBatchWithRetryInternal(batch);
            if (vectors == null) {
                // 零向量兜底，保持索引对齐
                List<Float> emptyVec = new ArrayList<>();
                for (int d = 0; d < embeddingService.getDimension(); d++) {
                    emptyVec.add(0.0f);
                }
                for (int j = 0; j < batch.size(); j++) {
                    allEmbeddings.add(emptyVec);
                }
            } else {
                allEmbeddings.addAll(vectors);
            }
        }
        return allEmbeddings;
    }

    /**
     * 对单个子批次执行 embedding（指数退避重试）。
     *
     * @param batch 文本子批次
     * @return 向量列表，所有重试均失败后返回 null（由外层兜底）
     */
    private List<List<Float>> embedBatchWithRetryInternal(List<String> batch) {
        int attempt = 0;
        long delay = initialRetryDelayMs;

        while (attempt <= maxRetries) {
            try {
                List<List<Float>> result = embeddingService.embedBatch(batch);
                // 检测空向量/空结果——视为失败，触发重试
                if (isResultEmpty(result)) {
                    throw new IllegalStateException("embedding 返回空向量，可能 provider 配置错误或 API 响应异常");
                }
                return result;
            } catch (Exception e) {
                attempt++;
                if (attempt > maxRetries) {
                    log.error("批量向量化失败，已达最大重试次数 {}: {}", maxRetries, e.getMessage());
                    return null;
                }
                log.warn("批量向量化第 {} 次失败，{}ms 后重试: {}", attempt, delay, e.getMessage());
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
                delay *= 2;
            }
        }
        return null;
    }

    /**
     * 检查嵌入结果是否全为空（所有向量维度为 0 或结果列表为空）。
     */
    private boolean isResultEmpty(List<List<Float>> result) {
        if (result == null || result.isEmpty()) {
            return true;
        }
        for (List<Float> vec : result) {
            if (vec == null || vec.isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
