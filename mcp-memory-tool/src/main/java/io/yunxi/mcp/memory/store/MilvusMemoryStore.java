package io.yunxi.mcp.memory.store;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.vector.request.DeleteReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp;
import io.yunxi.mcp.memory.config.MilvusProperties;
import io.yunxi.mcp.memory.embedding.EmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Milvus 向量存储的记忆存储服务
 * <p>
 * 使用 Milvus 向量数据库作为记忆存储后端，实现高效的语义检索功能。
 * 适用于需要基于向量相似度进行记忆检索的场景。
 * </p>
 * <p>
 * 功能特性：
 * </p>
 * <ul>
 *   <li>向量存储 - 记忆内容自动向量化存储，支持语义检索</li>
 *   <li>语义搜索 - 基于向量相似度返回最相关的记忆</li>
 *   <li>多用户隔离 - 通过 userId 字段实现用户间数据隔离</li>
 *   <li>自动建表 - 启动时自动创建记忆集合和索引</li>
 * </ul>
 * <p>
 * 数据结构：
 * </p>
 * <ul>
 *   <li>id - 条目唯一标识（UUID）</li>
 *   <li>userId - 用户ID（用于多用户隔离）</li>
 *   <li>target - 目标存储（memory/user）</li>
 *   <li>category - 记忆分类</li>
 *   <li>content - 记忆内容（文本）</li>
 *   <li>embedding - 向量嵌入（1024维）</li>
 *   <li>timestamp - 时间戳</li>
 * </ul>
 * <p>
 * 索引配置：
 * </p>
 * <ul>
 *   <li>索引类型：IVF_FLAT（倒排索引）</li>
 *   <li>度量类型：IP（内积）</li>
 *   <li>nlist：256</li>
 * </ul>
 * <p>
 * 激活条件：通过配置 {@code memory.store.type=milvus} 启用。
 * </p>
 *
 * @author yunxi-mcp-servers
 * @see <a href="https://milvus.io/">Milvus Vector Database</a>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "memory.store.type", havingValue = "milvus")
public class MilvusMemoryStore implements MemoryStoreInterface {

    /** 字段名常量 - ID */
    private static final String FIELD_ID = "id";
    /** 字段名常量 - 用户ID */
    private static final String FIELD_USER_ID = "userId";
    /** 字段名常量 - 目标存储 */
    private static final String FIELD_TARGET = "target";
    /** 字段名常量 - 分类 */
    private static final String FIELD_CATEGORY = "category";
    /** 字段名常量 - 内容 */
    private static final String FIELD_CONTENT = "content";
    /** 字段名常量 - 向量嵌入 */
    private static final String FIELD_EMBEDDING = "embedding";
    /** 字段名常量 - 时间戳 */
    private static final String FIELD_TIMESTAMP = "timestamp";

    /** Milvus 客户端 */
    @Autowired
    private MilvusClientV2 milvusClient;

    /** 嵌入服务 */
    @Autowired
    private EmbeddingService embeddingService;

    /** Milvus 配置属性 */
    @Autowired
    private MilvusProperties milvusProperties;

    /** JSON 序列化工具 */
    private final Gson gson = new Gson();

    /**
     * 初始化方法
     * <p>
     * 在 Bean 构造完成后执行，检查 Milvus 连接并创建记忆集合（如果不存在）。
     * </p>
     */
    @PostConstruct
    public void init() {
        if (milvusClient == null) {
            log.warn("Milvus 客户端未初始化");
            return;
        }

        // 重试机制：最多重试3次，每次间隔2秒
        int maxRetries = 3;
        int retryDelayMs = 2000;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // 先验证连接是否可用
                milvusClient.listCollections();
                log.info("Milvus 连接验证成功");

                createCollectionIfNotExists();
                log.info("MilvusMemoryStore 初始化完成: collection={}", milvusProperties.collectionName);
                return; // 成功则退出
            } catch (Exception e) {
                log.warn("Milvus 初始化尝试 {}/{} 失败: {}", attempt, maxRetries, e.getMessage());
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(retryDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        log.error("Milvus 初始化失败，已达到最大重试次数");
    }

    /**
     * 创建记忆集合（如果不存在）
     * <p>
     * 自动创建包含向量字段的集合，并建立 IVF_FLAT 索引。
     * </p>
     */
    private void createCollectionIfNotExists() {
        // 检查集合是否已存在
        HasCollectionReq hasReq = HasCollectionReq.builder()
                .collectionName(milvusProperties.collectionName)
                .build();

        if (milvusClient.hasCollection(hasReq)) {
            log.debug("记忆集合已存在: {}", milvusProperties.collectionName);
            return;
        }

        // 获取向量维度
        int dimension = embeddingService.getDimension();

        // 构建字段 schema
        CreateCollectionReq.FieldSchema idField = CreateCollectionReq.FieldSchema.builder()
                .name(FIELD_ID).dataType(DataType.VarChar).maxLength(64)
                .isPrimaryKey(true).autoID(false).build();

        CreateCollectionReq.FieldSchema userIdField = CreateCollectionReq.FieldSchema.builder()
                .name(FIELD_USER_ID).dataType(DataType.VarChar).maxLength(128).build();

        CreateCollectionReq.FieldSchema targetField = CreateCollectionReq.FieldSchema.builder()
                .name(FIELD_TARGET).dataType(DataType.VarChar).maxLength(32).build();

        CreateCollectionReq.FieldSchema categoryField = CreateCollectionReq.FieldSchema.builder()
                .name(FIELD_CATEGORY).dataType(DataType.VarChar).maxLength(128).build();

        CreateCollectionReq.FieldSchema contentField = CreateCollectionReq.FieldSchema.builder()
                .name(FIELD_CONTENT).dataType(DataType.VarChar).maxLength(8192).build();

        CreateCollectionReq.FieldSchema embeddingField = CreateCollectionReq.FieldSchema.builder()
                .name(FIELD_EMBEDDING).dataType(DataType.FloatVector).dimension(dimension).build();

        CreateCollectionReq.FieldSchema timestampField = CreateCollectionReq.FieldSchema.builder()
                .name(FIELD_TIMESTAMP).dataType(DataType.Int64).build();

        // 构建集合 schema
        CreateCollectionReq.CollectionSchema schema = CreateCollectionReq.CollectionSchema.builder()
                .fieldSchemaList(Arrays.asList(
                        idField, userIdField, targetField, categoryField,
                        contentField, embeddingField, timestampField))
                .build();

        // 构建索引参数
        IndexParam indexParam = IndexParam.builder()
                .fieldName(FIELD_EMBEDDING)
                .indexType(IndexParam.IndexType.IVF_FLAT)
                .metricType(IndexParam.MetricType.IP)
                .extraParams(Map.of("nlist", "256"))
                .build();

        // 创建集合
        CreateCollectionReq createReq = CreateCollectionReq.builder()
                .collectionName(milvusProperties.collectionName)
                .collectionSchema(schema)
                .indexParams(Collections.singletonList(indexParam))
                .build();

        milvusClient.createCollection(createReq);
        log.info("创建记忆集合成功: collection={}, dimension={}", milvusProperties.collectionName, dimension);
    }

    /**
     * 添加记忆条目
     * <p>
     * 将记忆内容向量化后存储到 Milvus。
     * </p>
     *
     * @param userId   用户ID
     * @param target   目标存储
     * @param category 分类
     * @param content  内容
     * @return 条目ID
     */
    @Override
    public String addEntry(String userId, String target, String category, String content) {
        if (milvusClient == null) {
            log.warn("Milvus 不可用");
            return "";
        }

        try {
            // 生成唯一 ID 和时间戳
            String id = generateId();
            long timestamp = System.currentTimeMillis();

            // 将内容转换为向量
            List<Float> embedding = embeddingService.embed(content);
            if (embedding == null || embedding.isEmpty()) {
                log.warn("向量化失败: userId={}, content={}", userId, content);
                return "";
            }

            // 构建数据对象
            JsonObject data = new JsonObject();
            data.addProperty(FIELD_ID, id);
            data.addProperty(FIELD_USER_ID, userId != null ? userId : "default");
            data.addProperty(FIELD_TARGET, target);
            data.addProperty(FIELD_CATEGORY, category);
            data.addProperty(FIELD_CONTENT, content);
            data.add(FIELD_EMBEDDING, gson.toJsonTree(embedding));
            data.addProperty(FIELD_TIMESTAMP, timestamp);

            // 插入数据
            InsertReq insertReq = InsertReq.builder()
                    .collectionName(milvusProperties.collectionName)
                    .data(Collections.singletonList(data))
                    .build();

            milvusClient.insert(insertReq);
            log.debug("记忆已添加: userId={}, target={}, id={}", userId, target, id);

            return id;

        } catch (Exception e) {
            log.error("添加记忆失败: userId={}, error={}", userId, e.getMessage(), e);
            return "";
        }
    }

    /**
     * 替换记忆条目
     * <p>
     * Milvus 不支持直接更新，采用删除后重新插入的方式实现替换。
     * </p>
     *
     * @param userId     用户ID
     * @param target     目标存储
     * @param entryId    条目ID
     * @param newContent 新内容
     * @return 新条目ID
     */
    @Override
    public String replaceEntry(String userId, String target, String entryId, String newContent) {
        if (milvusClient == null) {
            log.warn("Milvus 不可用");
            return "";
        }

        try {
            // Milvus 不支持直接更新，需要删除后重新插入
            removeEntry(userId, target, entryId);

            // 查询原记录获取 category（简化处理，假设 category 不变）
            // 实际应用中可以先查询获取 category
            String newId = addEntry(userId, target, "replaced", newContent);

            log.debug("记忆已替换: userId={}, target={}, oldId={}, newId={}", userId, target, entryId, newId);
            return newId;

        } catch (Exception e) {
            log.error("替换记忆失败: userId={}, id={}, error={}", userId, entryId, e.getMessage(), e);
            return "";
        }
    }

    /**
     * 删除记忆条目
     *
     * @param userId  用户ID
     * @param target  目标存储
     * @param entryId 条目ID
     */
    @Override
    public void removeEntry(String userId, String target, String entryId) {
        if (milvusClient == null) {
            log.warn("Milvus 不可用");
            return;
        }

        try {
            String filter = FIELD_ID + " == \"" + entryId + "\"";

            DeleteReq deleteReq = DeleteReq.builder()
                    .collectionName(milvusProperties.collectionName)
                    .filter(filter)
                    .build();

            milvusClient.delete(deleteReq);
            log.debug("记忆已删除: userId={}, id={}", userId, entryId);

        } catch (Exception e) {
            log.error("删除记忆失败: userId={}, id={}, error={}", userId, entryId, e.getMessage(), e);
        }
    }

    /**
     * 获取相关记忆
     * <p>
     * 使用向量语义检索返回与 context 最相关的记忆内容。
     * 如果未提供 context，则返回与"记忆"关键词相关的记忆。
     * </p>
     *
     * @param userId  用户ID
     * @param target  目标存储
     * @param context 上下文（用于语义检索）
     * @return 记忆内容
     */
    @Override
    public String getRelevantMemory(String userId, String target, String context) {
        if (milvusClient == null) {
            log.warn("Milvus 不可用");
            return "(读取失败: Milvus 不可用)";
        }

        try {
            // 构建过滤条件
            String filter = FIELD_USER_ID + " == \"" + (userId != null ? userId : "default") + "\"";
            if (target != null) {
                filter += " && " + FIELD_TARGET + " == \"" + target + "\"";
            }

            // 向量化查询文本
            String queryText = context != null ? context : "记忆";
            List<Float> queryVector = embeddingService.embed(queryText);
            if (queryVector == null || queryVector.isEmpty()) {
                return "(读取失败: 向量化失败)";
            }

            // 执行向量搜索
            SearchReq searchReq = SearchReq.builder()
                    .collectionName(milvusProperties.collectionName)
                    .data(Collections.singletonList(new FloatVec(queryVector)))
                    .annsField(FIELD_EMBEDDING)
                    .topK(10)
                    .filter(filter)
                    .outputFields(Arrays.asList(
                            FIELD_CATEGORY, FIELD_CONTENT, FIELD_TIMESTAMP))
                    .build();

            SearchResp searchResp = milvusClient.search(searchReq);
            List<List<SearchResp.SearchResult>> searchResults = searchResp.getSearchResults();

            if (searchResults == null || searchResults.isEmpty()) {
                return "(无相关记忆)";
            }

            // 解析结果
            StringBuilder sb = new StringBuilder();
            for (SearchResp.SearchResult hit : searchResults.get(0)) {
                Map<String, Object> entity = hit.getEntity();
                String category = entity.getOrDefault(FIELD_CATEGORY, "").toString();
                String content = entity.getOrDefault(FIELD_CONTENT, "").toString();
                Long timestamp = (Long) entity.get(FIELD_TIMESTAMP);

                // 格式化时间戳
                String timeStr = timestamp != null ?
                        LocalDateTime.ofEpochSecond(timestamp / 1000, (int) (timestamp % 1000) * 1000000, null)
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) :
                        "";

                sb.append("【").append(timeStr).append("】").append("\n");
                sb.append("【").append(category).append("】").append("\n");
                sb.append(content).append("\n\n");
            }

            return sb.toString().trim();

        } catch (Exception e) {
            log.error("获取记忆失败: userId={}, error={}", userId, e.getMessage(), e);
            return "(读取失败: " + e.getMessage() + ")";
        }
    }

    /**
     * 获取记忆统计
     * <p>
     * Milvus 作为向量数据库，不关注字符数限制，
     * 因此返回无限制的统计信息。
     * </p>
     *
     * @param userId 用户ID
     * @return 统计信息
     */
    @Override
    public MemoryStats getMemoryStats(String userId) {
        MemoryStats stats = new MemoryStats();
        stats.setMemoryUsage(0);
        stats.setMemoryLimit(Integer.MAX_VALUE);
        stats.setMemoryUsagePercentage(0.0);
        stats.setUserUsage(0);
        stats.setUserLimit(Integer.MAX_VALUE);
        stats.setUserUsagePercentage(0.0);

        // Milvus 不需要统计字符数限制
        return stats;
    }

    /**
     * 生成唯一 ID
     *
     * @return 32 位 UUID 字符串
     */
    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }
}
