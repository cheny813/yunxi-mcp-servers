package io.yunxi.mcp.nutrition.sync;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.yunxi.mcp.common.embedding.EmbeddingBatchService;
import io.yunxi.mcp.common.milvus.MilvusConfig;
import io.yunxi.mcp.nutrition.persistence.milvus.MilvusOperations;
import io.yunxi.mcp.nutrition.sync.config.SyncDatasourceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 学校菜品向量同步（MySQL -&gt; Milvus）
 * <p>
 * 从营养库读取「有学校归属（school_id 非 NULL）」的菜品，分页向量化后写入 Milvus 集合
 * {@code <collection-prefix><schoolId>}，供后续菜品语义检索使用。
 * 公共菜品（school_id 为 NULL）由 sync-pipelines.yml 的 dish-library-sync 管道写入
 * {@code school_common_dishes}，两路互不重叠，不会出现语义错误的 {@code school_dishes_0} 集合。
 * 数据源连接统一通过 {@link SyncDatasourceRegistry} 获取（不再自建连接池）。
 * </p>
 *
 * @author yunxi-agent-platform
 * @version 2.1.0
 */
@Service
public class SchoolDishSyncRunner implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(SchoolDishSyncRunner.class);

    private final SyncDatasourceRegistry datasourceRegistry;
    private final EmbeddingBatchService embeddingBatchService;
    private final MilvusOperations milvusOps;
    private final MilvusConfig milvusConfig;

    @Value("${sync.dish.datasource:default}")
    private String dishDatasource;
    @Value("${sync.dish.collection-prefix:school_dishes_}")
    private String collectionPrefix;
    @Value("${sync.auto-sync-on-startup:true}")
    private boolean syncOnStartup;
    @Value("${sync.dish.query-page-size:1000}")
    private int queryPageSize;
    @Value("${sync.dish.cron:0 0 2 * * ?}")
    private String cron;

    public SchoolDishSyncRunner(SyncDatasourceRegistry datasourceRegistry,
                                EmbeddingBatchService embeddingBatchService,
                                MilvusOperations milvusOps,
                                MilvusConfig milvusConfig) {
        this.datasourceRegistry = datasourceRegistry;
        this.embeddingBatchService = embeddingBatchService;
        this.milvusOps = milvusOps;
        this.milvusConfig = milvusConfig;
    }

    @Override
    public void afterPropertiesSet() {
        log.info("SchoolDishSyncRunner 初始化完成（datasource={}, cron={}, syncOnStartup={}）",
                dishDatasource, cron, syncOnStartup);
        if (syncOnStartup) {
            try {
                syncAllSchoolDishes();
            } catch (Exception e) {
                log.error("启动时菜品同步失败", e);
            }
        }
    }

    /** 定时任务：每天按 cron 触发全量菜品同步 */
    @Scheduled(cron = "${sync.dish.cron:0 0 2 * * ?}")
    public void scheduledSync() {
        log.info("触发定时菜品同步");
        syncAllSchoolDishes();
    }

    /** 同步所有学校的菜品到 Milvus */
    public void syncAllSchoolDishes() {
        if (!milvusOps.isAvailable()) {
            log.warn("Milvus 不可用，跳过菜品同步");
            return;
        }
        JdbcTemplate jdbc = datasourceRegistry.getJdbcTemplate(dishDatasource);
        // 仅同步有学校归属的菜品（school_id 非 NULL）；公共菜品（school_id IS NULL）由
        // sync-pipelines.yml 的 dish-library-sync 管道写入 school_common_dishes，避免产生
        // 语义错误的 school_dishes_0 集合。
        List<Long> schoolIds = jdbc.queryForList(
                "SELECT DISTINCT school_id FROM dish_library WHERE school_id IS NOT NULL AND deleted = 0 ORDER BY school_id",
                Long.class);
        log.info("发现 {} 所学校的专属菜品，开始同步（公共菜品由 dish-library-sync 管道同步）", schoolIds.size());
        for (Long schoolId : schoolIds) {
            try {
                syncSchoolDishes(schoolId);
            } catch (Exception e) {
                log.error("同步学校 {} 菜品失败", schoolId, e);
            }
        }
    }

    /** 同步单个学校的菜品（分页） */
    public void syncSchoolDishes(Long schoolId) {
        String collectionName = collectionPrefix + schoolId;
        JdbcTemplate jdbc = datasourceRegistry.getJdbcTemplate(dishDatasource);
        int offset = 0;
        int synced = 0;
        while (true) {
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT id, name, type, update_time FROM dish_library WHERE COALESCE(school_id, 0) = ? AND deleted = 0 ORDER BY id LIMIT ? OFFSET ?",
                    schoolId, queryPageSize, offset);
            if (rows.isEmpty()) {
                break;
            }
            ensureCollection(collectionName);
            List<String> texts = new ArrayList<>();
            List<JsonObject> data = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                String id = String.valueOf(row.get("id"));
                String name = row.get("name") == null ? "" : String.valueOf(row.get("name"));
                String type = row.get("type") == null ? "" : String.valueOf(row.get("type"));
                String updateTime = row.get("update_time") == null ? "" : String.valueOf(row.get("update_time"));
                texts.add(buildEmbedText(name, type));
                JsonObject jo = new JsonObject();
                jo.addProperty("id", Long.parseLong(id));
                jo.addProperty("school_id", schoolId);
                jo.addProperty("name", name);
                jo.addProperty("type", type);
                // SearchDishesTool / GetDishDetailsTool 期望的 outputFields（与 school_common_dishes 对齐）
                jo.addProperty("description", "");
                jo.addProperty("ingredients", "");
                jo.addProperty("ingredient_ids", "");
                jo.addProperty("nutrients", "");
                jo.addProperty("update_time", updateTime);
                data.add(jo);
            }
            List<List<Float>> vectors = embeddingBatchService.embedBatchWithRetry(texts);
            for (int i = 0; i < data.size(); i++) {
                JsonArray vec = new JsonArray();
                for (Float v : vectors.get(i)) {
                    vec.add(v);
                }
                data.get(i).add("embedding", vec);
            }
            milvusOps.upsertBatch(collectionName, data, 100);
            synced += rows.size();
            offset += queryPageSize;
            if (rows.size() < queryPageSize) {
                break;
            }
        }
        log.info("学校 {} 菜品同步完成，共 {} 条 -> 集合 {}", schoolId, synced, collectionName);
    }

    private void ensureCollection(String collectionName) {
        if (milvusOps.hasCollection(collectionName)) {
            // 已有集合也需要加载到内存
            milvusOps.loadCollection(collectionName);
            return;
        }
        int dim = milvusOps.getEmbeddingDimension();
        // 使用 Int64 存储 id 和 school_id，与 school_common_dishes 对齐，
        // 确保 SearchDishTool/GetDishDetailsTool 的数值型 filter 表达式正常工作
        CreateCollectionReq.FieldSchema idField = CreateCollectionReq.FieldSchema.builder()
                .name("id").dataType(DataType.Int64)
                .isPrimaryKey(true).autoID(false).build();
        CreateCollectionReq.FieldSchema schoolField = CreateCollectionReq.FieldSchema.builder()
                .name("school_id").dataType(DataType.Int64).build();
        CreateCollectionReq.FieldSchema nameField = CreateCollectionReq.FieldSchema.builder()
                .name("name").dataType(DataType.VarChar).maxLength(512).build();
        CreateCollectionReq.FieldSchema descField = CreateCollectionReq.FieldSchema.builder()
                .name("description").dataType(DataType.VarChar).maxLength(4096).build();
        // SearchDishesTool / GetDishDetailsTool 期望的 outputFields（与 school_common_dishes 对齐）
        CreateCollectionReq.FieldSchema typeField = CreateCollectionReq.FieldSchema.builder()
                .name("type").dataType(DataType.VarChar).maxLength(128).build();
        CreateCollectionReq.FieldSchema ingredientsField = CreateCollectionReq.FieldSchema.builder()
                .name("ingredients").dataType(DataType.VarChar).maxLength(4096).build();
        CreateCollectionReq.FieldSchema ingredientIdsField = CreateCollectionReq.FieldSchema.builder()
                .name("ingredient_ids").dataType(DataType.VarChar).maxLength(2048).build();
        CreateCollectionReq.FieldSchema nutrientsField = CreateCollectionReq.FieldSchema.builder()
                .name("nutrients").dataType(DataType.VarChar).maxLength(4096).build();
        CreateCollectionReq.FieldSchema updateTimeField = CreateCollectionReq.FieldSchema.builder()
                .name("update_time").dataType(DataType.VarChar).maxLength(64).build();
        CreateCollectionReq.FieldSchema embedField = CreateCollectionReq.FieldSchema.builder()
                .name("embedding").dataType(DataType.FloatVector).dimension(dim).build();
        CreateCollectionReq.CollectionSchema schema = CreateCollectionReq.CollectionSchema.builder()
                .fieldSchemaList(List.of(idField, schoolField, nameField, descField,
                        typeField, ingredientsField, ingredientIdsField, nutrientsField, updateTimeField, embedField))
                .build();
        IndexParam indexParam = IndexParam.builder()
                .fieldName("embedding")
                .indexType(IndexParam.IndexType.IVF_FLAT)
                .metricType(IndexParam.MetricType.COSINE)
                .extraParams(Map.of("nlist", "1024"))
                .build();
        milvusOps.ensureCollection(collectionName, schema, List.of(indexParam));
        // 新建集合后加载到内存
        milvusOps.loadCollection(collectionName);
    }

    private String buildEmbedText(String name, String desc) {
        return (name == null ? "" : name) + " " + (desc == null ? "" : desc);
    }
}
