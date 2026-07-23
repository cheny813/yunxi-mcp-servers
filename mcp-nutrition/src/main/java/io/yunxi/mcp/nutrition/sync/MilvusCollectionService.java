package io.yunxi.mcp.nutrition.sync;

import com.google.gson.JsonObject;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.yunxi.mcp.nutrition.persistence.milvus.MilvusOperations;
import io.yunxi.mcp.nutrition.sync.config.SyncPipelineConfig;
import io.yunxi.mcp.nutrition.sync.config.SyncPipelineConfig.FieldConfig;
import io.yunxi.mcp.nutrition.sync.config.SyncPipelineConfig.IndexConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Milvus 集合服务
 *
 * <p>
 * 封装 Milvus 的集合操作（创建、upsert、统计计数等），
 * 通过 MilvusOperations 门面访问 Milvus，保证 null 安全。
 * </p>
 *
 * @author yunxi-agent-platform
 * @version 2.1.0
 */
@Service
public class MilvusCollectionService {

    private static final Logger log = LoggerFactory.getLogger(MilvusCollectionService.class);

    /** Milvus 操作门面 */
    private final MilvusOperations milvusOps;

    /**
     * 构造方法
     *
     * @param milvusOps Milvus 操作门面
     */
    public MilvusCollectionService(MilvusOperations milvusOps) {
        this.milvusOps = milvusOps;
    }

    /**
     * 批量 upsert 数据到 Milvus（已存在则更新，不存在则插入）
     *
     * @param collectionName 集合名称
     * @param dataList       数据列表
     * @param batchSize      批量大小
     */
    public void upsertBatch(String collectionName, List<JsonObject> dataList, int batchSize) {
        milvusOps.upsertBatch(collectionName, dataList, batchSize);
    }

    /**
     * 获取 Milvus 集合的记录数量
     * <p>
     * 使用 {@link MilvusOperations#getCollectionStatistics(String)} 从元数据获取行数，
     * 相比 QueryIterator 遍历更高效可靠（O(1) 复杂度）。
     * </p>
     *
     * @param collectionName 集合名称
     * @return 记录数量，集合不存在或 Milvus 不可用时返回 0
     */
    public long getCollectionCount(String collectionName) {
        if (!milvusOps.isAvailable()) {
            return 0;
        }
        if (!isCollectionExists(collectionName)) {
            return 0;
        }
        return milvusOps.getCollectionStatistics(collectionName);
    }

    /**
     * 检查集合是否存在
     *
     * @param collectionName 集合名称
     * @return true-存在，false-不存在
     */
    public boolean isCollectionExists(String collectionName) {
        return milvusOps.hasCollection(collectionName);
    }

    /**
     * 确保同步管道对应的 Milvus 集合存在（含字段 Schema、向量索引、内存加载）。
     * <p>
     * 此方法从 SyncEngine 中提取，供 SyncEngine 和 SyncCollectionInitializer 共同调用，
     * 避免 SyncCollectionInitializer 依赖 SyncEngine（防止 SyncEngine @PostConstruct
     * 异常导致初始化器连锁失败）。
     * </p>
     *
     * @param cfg 同步管道配置
     */
    public void ensureSyncCollection(SyncPipelineConfig cfg) {
        if (!milvusOps.isAvailable()) {
            return;
        }
        String collectionName = cfg.getTarget().getCollection();
        if (milvusOps.hasCollection(collectionName)) {
            milvusOps.markCollectionInitialized(collectionName);
            // 已有集合也需要加载到内存（Milvus 重启后集合可能处于未加载状态）
            boolean loaded = milvusOps.loadCollection(collectionName);
            if (!loaded) {
                // 加载失败常见原因：集合缺少向量索引 → 先补建索引再重试
                IndexConfig idx = cfg.getTarget().getIndex();
                String vectorField = idx != null && idx.getField() != null ? idx.getField() : "embedding";
                String idxType = idx != null && idx.getIndexType() != null ? idx.getIndexType() : "IVF_FLAT";
                String metricStr = idx != null && idx.getMetricType() != null ? idx.getMetricType() : "COSINE";
                IndexParam.MetricType metricType = IndexParam.MetricType.valueOf(metricStr);

                log.info("集合 {} 加载失败，尝试补建索引: field={}, index={}, metric={}",
                        collectionName, vectorField, idxType, metricType);

                List<IndexParam> indexParams = List.of(IndexParam.builder()
                        .fieldName(vectorField)
                        .indexType(IndexParam.IndexType.valueOf(idxType))
                        .metricType(metricType)
                        .extraParams(Map.of("nlist", "1024"))
                        .build());

                if (milvusOps.createIndexForExisting(collectionName, indexParams)) {
                    milvusOps.loadCollection(collectionName);
                } else {
                    log.warn("集合 {} 补建索引失败，请手动检查并重建集合", collectionName);
                }
            }
            return;
        }

        List<CreateCollectionReq.FieldSchema> fieldSchemas = new ArrayList<>();
        for (FieldConfig f : cfg.getTarget().getFields()) {
            var b = CreateCollectionReq.FieldSchema.builder()
                    .name(f.getName())
                    .dataType(DataType.valueOf(f.getType()))
                    .description(f.getDescription() != null ? f.getDescription() : "");
            if (f.isPrimaryKey()) {
                b.isPrimaryKey(true).autoID(false);
            }
            if ("FloatVector".equals(f.getType()) && f.getDimension() != null) {
                b.dimension(f.getDimension());
            }
            if ("VarChar".equals(f.getType()) && f.getMaxLength() != null) {
                b.maxLength(f.getMaxLength());
            }
            fieldSchemas.add(b.build());
        }

        CreateCollectionReq.CollectionSchema schema = CreateCollectionReq.CollectionSchema.builder()
                .fieldSchemaList(fieldSchemas).build();

        List<IndexParam> indexParams = new ArrayList<>();
        IndexConfig idx = cfg.getTarget().getIndex();
        String indexType = idx != null && idx.getIndexType() != null ? idx.getIndexType() : "IVF_FLAT";
        if (idx != null && idx.getField() != null) {
            IndexParam.MetricType metric = IndexParam.MetricType.valueOf(
                    idx.getMetricType() != null ? idx.getMetricType() : "COSINE");
            indexParams.add(IndexParam.builder()
                    .fieldName(idx.getField())
                    .indexType(IndexParam.IndexType.valueOf(indexType))
                    .metricType(metric)
                    .extraParams(com.google.common.collect.ImmutableMap.of("nlist", "1024"))
                    .build());
        } else {
            indexParams.add(IndexParam.builder()
                    .fieldName("embedding")
                    .indexType(IndexParam.IndexType.valueOf(indexType))
                    .metricType(IndexParam.MetricType.COSINE)
                    .extraParams(com.google.common.collect.ImmutableMap.of("nlist", "1024"))
                    .build());
        }

        milvusOps.createCollection(collectionName, cfg.getTarget().getCollectionDescription(), schema, indexParams);
        // 新建集合后加载到内存
        milvusOps.loadCollection(collectionName);
    }
}
