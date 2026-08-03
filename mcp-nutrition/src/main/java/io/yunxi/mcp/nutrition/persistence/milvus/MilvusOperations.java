package io.yunxi.mcp.nutrition.persistence.milvus;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import com.google.gson.JsonObject;

import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.GetCollectionStatsReq;
import io.milvus.v2.service.collection.request.GetLoadStateReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.collection.response.GetCollectionStatsResp;
import io.milvus.v2.service.collection.request.LoadCollectionReq;
import io.milvus.v2.service.index.request.CreateIndexReq;
import io.milvus.v2.service.index.request.DescribeIndexReq;
import io.milvus.v2.service.index.response.DescribeIndexResp;
import io.milvus.v2.service.vector.request.DeleteReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.request.QueryReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.UpsertReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.QueryResp;
import io.milvus.v2.service.vector.response.SearchResp;
import io.yunxi.mcp.common.embedding.EmbeddingService;
import io.yunxi.mcp.common.embedding.EmbeddingBatchService;
import io.yunxi.mcp.common.milvus.MilvusConfig;

/**
 * Milvus 操作门面（v2 SDK）
 *
 * <p>
 * 统一封装所有 Milvus 操作，内置 null 安全检查和错误处理，
 * 使下游业务层无需关心 MilvusClientV2 可能为 null 的问题。
 * </p>
 *
 * @author yunxi-agent-platform
 * @version 2.0.0
 */
@Service
@ConditionalOnProperty(name = "milvus.enabled", havingValue = "true", matchIfMissing = false)
public class MilvusOperations {

    private static final Logger log = LoggerFactory.getLogger(MilvusOperations.class);

    private final MilvusClientV2 milvusClient;
    private final EmbeddingService embeddingService;
    private final EmbeddingBatchService embeddingBatchService;
    private final MilvusConfig milvusConfig;
    private final Set<String> initializedCollections = ConcurrentHashMap.newKeySet();

    /**
     * 构造 Milvus 操作门面。
     *
     * @param milvusClientProvider  Milvus 客户端提供者（不可用时取到 null，向量功能降级）
     * @param embeddingService      文本向量化服务
     * @param embeddingBatchService 批量向量化服务
     * @param milvusConfig          Milvus 配置
     */
    public MilvusOperations(
            ObjectProvider<MilvusClientV2> milvusClientProvider,
            EmbeddingService embeddingService,
            EmbeddingBatchService embeddingBatchService,
            MilvusConfig milvusConfig) {
        this.milvusClient = milvusClientProvider.getIfAvailable();
        this.embeddingService = embeddingService;
        this.embeddingBatchService = embeddingBatchService;
        this.milvusConfig = milvusConfig;
        if (milvusClient != null) {
            log.info("MilvusOperations 初始化完成（Milvus 可用）");
        } else {
            log.warn("MilvusOperations 初始化完成（Milvus 不可用，向量功能降级）");
        }
    }

    /**
     * 判断 Milvus 客户端是否可用（已成功注入且非 null）。
     *
     * @return 可用时返回 true
     */
    public boolean isAvailable() {
        return milvusClient != null;
    }

    /**
     * 返回底层原生 Milvus 客户端（可能为 null）。
     *
     * @return Milvus 客户端，不可用时为 null
     */
    @Nullable
    public MilvusClientV2 getRawClient() {
        return milvusClient;
    }

    /**
     * 列出当前 Milvus 实例中的全部集合名称。
     *
     * @return 集合名称列表，不可用时返回空列表
     */
    public List<String> listCollections() {
        if (!isAvailable())
            return Collections.emptyList();
        try {
            io.milvus.v2.service.collection.response.ListCollectionsResp resp = milvusClient.listCollections();
            return resp.getCollectionNames();
        } catch (Exception e) {
            log.error("列出集合失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 检查指定集合是否存在。
     *
     * @param collectionName 集合名称
     * @return 存在时返回 true，不可用时返回 false
     */
    public boolean hasCollection(String collectionName) {
        if (!isAvailable())
            return false;
        try {
            HasCollectionReq req = HasCollectionReq.builder().collectionName(collectionName).build();
            return milvusClient.hasCollection(req);
        } catch (Exception e) {
            log.error("检查集合存在失败: {}", collectionName, e);
            return false;
        }
    }

    /**
     * 获取集合的实体数量统计。
     *
     * @param collectionName 集合名称
     * @return 实体数量，不可用时返回 -1
     */
    public long getCollectionStatistics(String collectionName) {
        if (!isAvailable())
            return -1;
        try {
            GetCollectionStatsReq req = GetCollectionStatsReq.builder().collectionName(collectionName).build();
            GetCollectionStatsResp resp = milvusClient.getCollectionStats(req);
            return resp.getNumOfEntities();
        } catch (Exception e) {
            log.error("获取集合统计信息失败: {}", collectionName, e);
            return -1;
        }
    }

    /**
     * 创建集合（仅 schema，无索引）。
     *
     * @param collectionName 集合名称
     * @param schema         集合 schema
     * @return 创建成功返回 true，不可用时返回 false
     */
    public boolean createCollection(String collectionName, CreateCollectionReq.CollectionSchema schema) {
        if (!isAvailable())
            return false;
        try {
            CreateCollectionReq req = CreateCollectionReq.builder()
                    .collectionName(collectionName).collectionSchema(schema).build();
            milvusClient.createCollection(req);
            initializedCollections.add(collectionName);
            return true;
        } catch (Exception e) {
            log.error("创建集合失败: {}", collectionName, e);
            return false;
        }
    }

    /**
     * 创建集合（含描述与索引参数）。
     *
     * @param collectionName 集合名称
     * @param description    集合描述
     * @param schema         集合 schema
     * @param indexParams    索引参数列表
     * @return 创建成功返回 true，不可用时返回 false
     */
    public boolean createCollection(String collectionName, String description,
            CreateCollectionReq.CollectionSchema schema, List<IndexParam> indexParams) {
        if (!isAvailable())
            return false;
        try {
            CreateCollectionReq req = CreateCollectionReq.builder()
                    .collectionName(collectionName).description(description)
                    .collectionSchema(schema).indexParams(indexParams).build();
            milvusClient.createCollection(req);
            initializedCollections.add(collectionName);
            return true;
        } catch (Exception e) {
            log.error("创建集合失败（含索引）: {}", collectionName, e);
            return false;
        }
    }

    /**
     * 创建集合（带索引参数，省略描述）。
     *
     * @param collectionName 集合名称
     * @param schema         集合 schema
     * @param indexParams    索引参数列表
     * @return 创建成功返回 true，不可用时返回 false
     */
    public boolean createCollection(String collectionName, CreateCollectionReq.CollectionSchema schema,
            List<IndexParam> indexParams) {
        if (!isAvailable())
            return false;
        try {
            CreateCollectionReq req = CreateCollectionReq.builder()
                    .collectionName(collectionName).collectionSchema(schema).indexParams(indexParams).build();
            milvusClient.createCollection(req);
            initializedCollections.add(collectionName);
            return true;
        } catch (Exception e) {
            log.error("创建集合失败（含索引）: {}", collectionName, e);
            return false;
        }
    }

    /**
     * 确保集合存在（不存在则创建，无索引）。已初始化则跳过。
     *
     * @param collectionName 集合名称
     * @param schema         集合 schema
     * @return 本次创建返回 true，已存在或重复初始化返回 false
     */
    public boolean ensureCollection(String collectionName, CreateCollectionReq.CollectionSchema schema) {
        if (!isAvailable())
            return false;
        if (initializedCollections.contains(collectionName))
            return false;
        try {
            if (!hasCollection(collectionName)) {
                createCollection(collectionName, schema);
                return true;
            }
            initializedCollections.add(collectionName);
            return false;
        } catch (Exception e) {
            log.error("确保集合存在失败: {}", collectionName, e);
            return false;
        }
    }

    /**
     * 确保集合存在（带索引参数）。已初始化则跳过。
     *
     * @param collectionName 集合名称
     * @param schema         集合 schema
     * @param indexParams    索引参数列表
     * @return 本次创建返回 true，已存在或重复初始化返回 false
     */
    public boolean ensureCollection(String collectionName, CreateCollectionReq.CollectionSchema schema,
            List<IndexParam> indexParams) {
        if (!isAvailable())
            return false;
        if (initializedCollections.contains(collectionName))
            return false;
        try {
            if (!hasCollection(collectionName)) {
                createCollection(collectionName, schema, indexParams);
                return true;
            }
            initializedCollections.add(collectionName);
            return false;
        } catch (Exception e) {
            log.error("确保集合存在失败: {}", collectionName, e);
            return false;
        }
    }

    /**
     * 删除集合并清理本地初始化缓存。
     *
     * @param collectionName 集合名称
     * @return 删除成功返回 true，不可用时返回 false
     */
    public boolean dropCollection(String collectionName) {
        if (!isAvailable())
            return false;
        try {
            milvusClient.dropCollection(io.milvus.v2.service.collection.request.DropCollectionReq.builder()
                    .collectionName(collectionName).build());
            initializedCollections.remove(collectionName);
            return true;
        } catch (Exception e) {
            log.error("删除集合失败: {}", collectionName, e);
            return false;
        }
    }

    /**
     * 将集合加载到内存（Milvus 创建/已有集合后必须显式加载才能查询/搜索）。
     *
     * @param collectionName 集合名称
     * @return 加载成功返回 true，不可用时返回 false
     */
    public boolean loadCollection(String collectionName) {
        if (!isAvailable())
            return false;
        try {
            LoadCollectionReq req = LoadCollectionReq.builder().collectionName(collectionName).build();
            milvusClient.loadCollection(req);
            log.info("集合加载请求已发送: {}", collectionName);

            // Milvus loadCollection 是异步的，必须轮询等待加载完成
            long start = System.currentTimeMillis();
            long timeoutMs = 60_000; // 最多等待 60 秒
            while (true) {
                GetLoadStateReq stateReq = GetLoadStateReq.builder().collectionName(collectionName).build();
                if (Boolean.TRUE.equals(milvusClient.getLoadState(stateReq))) {
                    long elapsed = System.currentTimeMillis() - start;
                    log.info("集合已加载到内存: {} (耗时 {}ms)", collectionName, elapsed);
                    return true;
                }
                if (System.currentTimeMillis() - start > timeoutMs) {
                    log.error("集合加载超时 ({}ms): {}", timeoutMs, collectionName);
                    return false;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        } catch (Exception e) {
            log.warn("加载集合失败: {}", collectionName, e.getMessage());
            return false;
        }
    }

    /**
     * 描述集合中指定字段的索引信息。
     *
     * @param collectionName 集合名称
     * @param fieldName      字段名称（通常是向量字段，如 "embedding"）
     * @return 索引描述响应，不可用或无索引时返回 null
     */
    @Nullable
    public DescribeIndexResp describeIndex(String collectionName, String fieldName) {
        if (!isAvailable())
            return null;
        try {
            return milvusClient.describeIndex(DescribeIndexReq.builder()
                    .collectionName(collectionName)
                    .fieldName(fieldName)
                    .build());
        } catch (Exception e) {
            log.warn("查询索引失败: collection={}, field={}: {}", collectionName, fieldName, e.getMessage());
            return null;
        }
    }

    /**
     * 为已存在的集合创建向量索引（用于修复缺失索引的遗留集合）。
     *
     * @param collectionName 集合名称
     * @param indexParams    索引参数列表（包含 fieldName、indexType、metricType、extraParams）
     * @return 创建成功返回 true
     */
    public boolean createIndexForExisting(String collectionName, List<IndexParam> indexParams) {
        if (!isAvailable())
            return false;
        try {
            milvusClient.createIndex(CreateIndexReq.builder()
                    .collectionName(collectionName)
                    .indexParams(indexParams)
                    .build());
            log.info("为已有集合创建索引成功: collection={}, params={}", collectionName, indexParams);
            return true;
        } catch (Exception e) {
            log.error("为已有集合创建索引失败: collection={}: {}", collectionName, e.getMessage());
            return false;
        }
    }

    /**
     * 标记集合已完成初始化（加入缓存）。
     *
     * @param collectionName 集合名称
     */
    public void markCollectionInitialized(String collectionName) {
        initializedCollections.add(collectionName);
    }

    /**
     * 清除集合的初始化缓存标记。
     *
     * @param collectionName 集合名称
     */
    public void clearCollectionCache(String collectionName) {
        initializedCollections.remove(collectionName);
    }

    /**
     * 向集合插入数据行。
     *
     * @param collectionName 集合名称
     * @param data           待插入的 JSON 行数据
     * @return 插入成功返回 true，不可用时返回 false
     */
    public boolean insert(String collectionName, List<JsonObject> data) {
        if (!isAvailable())
            return false;
        try {
            milvusClient.insert(InsertReq.builder().collectionName(collectionName).data(data).build());
            return true;
        } catch (Exception e) {
            log.error("插入数据失败: collection={}, count={}", collectionName, data.size(), e);
            return false;
        }
    }

    /**
     * 向集合 upsert（插入或更新）数据行。
     *
     * @param collectionName 集合名称
     * @param data           待 upsert 的 JSON 行数据
     * @return 操作成功返回 true，不可用时返回 false
     */
    public boolean upsert(String collectionName, List<JsonObject> data) {
        if (!isAvailable())
            return false;
        try {
            milvusClient.upsert(UpsertReq.builder().collectionName(collectionName).data(data).build());
            return true;
        } catch (Exception e) {
            log.error("Upsert 数据失败: collection={}, count={}", collectionName, data.size(), e);
            return false;
        }
    }

    /**
     * 分批 upsert 数据行（按 batchSize 切片，单批失败仅告警不中断）。
     *
     * @param collectionName 集合名称
     * @param dataList       待 upsert 的 JSON 行数据
     * @param batchSize      每批大小
     */
    public void upsertBatch(String collectionName, List<JsonObject> dataList, int batchSize) {
        if (!isAvailable() || dataList == null || dataList.isEmpty())
            return;
        int totalSize = dataList.size();
        int offset = 0;
        while (offset < totalSize) {
            int end = Math.min(offset + batchSize, totalSize);
            List<JsonObject> batch = dataList.subList(offset, end);
            try {
                milvusClient.upsert(UpsertReq.builder().collectionName(collectionName).data(batch).build());
            } catch (Exception e) {
                log.warn("批量 upsert 失败: collection={}, offset={}", collectionName, offset, e);
            }
            offset = end;
        }
    }

    /**
     * 按向量在集合中做语义搜索（默认向量字段）。
     *
     * @param collectionName 集合名称
     * @param vector         查询向量
     * @param topK           返回最相似结果数量
     * @param searchFields   输出字段列表
     * @param filterExpr     可选的过滤表达式（Milvus expr）
     * @return 搜索结果列表，不可用时返回空列表
     */
    @SuppressWarnings("deprecation")
    public List<SearchResp.SearchResult> search(String collectionName, List<Float> vector,
            int topK, List<String> searchFields, @Nullable String filterExpr) {
        if (!isAvailable())
            return Collections.emptyList();
        try {
            SearchReq.SearchReqBuilder b = SearchReq.builder()
                    .collectionName(collectionName).data(Collections.singletonList(new FloatVec(vector)))
                    .topK(topK).outputFields(searchFields);
            if (filterExpr != null && !filterExpr.isEmpty())
                b.filter(filterExpr);
            SearchResp resp = milvusClient.search(b.build());
            List<List<SearchResp.SearchResult>> results = resp.getSearchResults();
            return results != null && !results.isEmpty() ? results.get(0) : Collections.emptyList();
        } catch (Exception e) {
            log.error("向量搜索失败: collection={}", collectionName, e);
            return Collections.emptyList();
        }
    }

    /**
     * 按向量在集合中做语义搜索（指定向量字段）。
     *
     * @param collectionName 集合名称
     * @param vector         查询向量
     * @param annsField      向量字段名
     * @param topK           返回最相似结果数量
     * @param filterExpr     可选的过滤表达式
     * @param outputFields   输出字段列表
     * @return 搜索结果列表，不可用时返回空列表
     */
    @SuppressWarnings("deprecation")
    public List<SearchResp.SearchResult> search(String collectionName, List<Float> vector,
            String annsField, int topK, @Nullable String filterExpr, List<String> outputFields) {
        if (!isAvailable())
            return Collections.emptyList();
        try {
            SearchReq.SearchReqBuilder b = SearchReq.builder()
                    .collectionName(collectionName).data(Collections.singletonList(new FloatVec(vector)))
                    .annsField(annsField).topK(topK).outputFields(outputFields);
            if (filterExpr != null && !filterExpr.isEmpty())
                b.filter(filterExpr);
            SearchResp resp = milvusClient.search(b.build());
            List<List<SearchResp.SearchResult>> results = resp.getSearchResults();
            return results != null && !results.isEmpty() ? results.get(0) : Collections.emptyList();
        } catch (Exception e) {
            log.error("向量搜索失败: collection={}", collectionName, e);
            return Collections.emptyList();
        }
    }

    /**
     * 按过滤表达式删除集合数据。
     *
     * @param collectionName 集合名称
     * @param filterExpr     Milvus 删除过滤表达式
     * @return 删除成功返回 true，不可用时返回 false
     */
    public boolean delete(String collectionName, String filterExpr) {
        if (!isAvailable())
            return false;
        try {
            milvusClient.delete(DeleteReq.builder().collectionName(collectionName).filter(filterExpr).build());
            return true;
        } catch (Exception e) {
            log.error("删除数据失败: collection={}", collectionName, e);
            return false;
        }
    }

    /**
     * 按条件查询 Milvus 实体（非向量搜索，仅标量过滤）。
     *
     * @param collectionName 集合名
     * @param filter         标量过滤表达式（如 "id >= 0"）
     * @param outputFields   返回字段列表
     * @param offset         分页偏移
     * @param limit          分页大小
     * @return 实体列表，每个实体为 Map&lt;String, Object&gt;，失败返回空列表
     */
    public List<Map<String, Object>> query(String collectionName, String filter,
                                           List<String> outputFields, long offset, long limit) {
        if (!isAvailable()) {
            return Collections.emptyList();
        }
        try {
            QueryResp resp = milvusClient.query(QueryReq.builder()
                    .collectionName(collectionName)
                    .filter(filter)
                    .outputFields(outputFields)
                    .offset(offset)
                    .limit(limit)
                    .build());
            return resp.getQueryResults().stream()
                    .map(QueryResp.QueryResult::getEntity)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("查询数据失败: collection={}", collectionName, e);
            return Collections.emptyList();
        }
    }

    /**
     * 分页查询集合中所有主键 ID（用于 diff 同步）。
     *
     * @param collectionName 集合名
     * @return 所有 ID 的字符串集合
     */
    public Set<String> queryAllIds(String collectionName) {
        Set<String> allIds = new LinkedHashSet<>();
        long offset = 0;
        long limit = 2000;
        while (true) {
            List<Map<String, Object>> batch = query(collectionName, "id >= 0",
                    List.of("id"), offset, limit);
            if (batch.isEmpty()) {
                break;
            }
            for (Map<String, Object> entity : batch) {
                Object idVal = entity.get("id");
                if (idVal != null) {
                    allIds.add(String.valueOf(idVal));
                }
            }
            if (batch.size() < limit) {
                break;
            }
            offset += limit;
        }
        log.debug("查询集合 {} ID 数量: {}", collectionName, allIds.size());
        return allIds;
    }

    /**
     * 返回当前嵌入模型维度。
     *
     * @return 向量维度
     */
    public int getEmbeddingDimension() {
        return embeddingService.getDimension();
    }

    /**
     * 将文本嵌入为向量（异常时返回空列表）。
     *
     * @param text 待嵌入文本
     * @return 向量，失败返回空列表
     */
    public List<Float> embed(String text) {
        try {
            return embeddingService.embed(text);
        } catch (Exception e) {
            log.error("Text embedding failed", e);
            return Collections.emptyList();
        }
    }

    /**
     * 批量嵌入文本（带重试）。
     *
     * @param texts 待嵌入文本列表
     * @return 向量列表
     */
    public List<List<Float>> embedBatch(List<String> texts) {
        return embeddingBatchService.embedBatchWithRetry(texts);
    }

    /**
     * 返回 Milvus 配置。
     *
     * @return MilvusConfig 实例
     */
    public MilvusConfig getConfig() {
        return milvusConfig;
    }
}
