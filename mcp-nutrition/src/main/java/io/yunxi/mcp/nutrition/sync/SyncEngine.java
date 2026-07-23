package io.yunxi.mcp.nutrition.sync;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import io.yunxi.mcp.common.embedding.EmbeddingBatchService;
import io.yunxi.mcp.nutrition.persistence.milvus.MilvusOperations;
import io.yunxi.mcp.nutrition.shared.entity.SyncCursorEntity;
import io.yunxi.mcp.nutrition.shared.repository.SyncCursorRepository;
import io.yunxi.mcp.nutrition.sync.config.SyncDatasourceRegistry;
import io.yunxi.mcp.nutrition.sync.config.SyncPipelineConfig;
import io.yunxi.mcp.nutrition.sync.config.SyncPipelineConfig.FieldConfig;
import io.yunxi.mcp.nutrition.sync.config.SyncPipelineConfig.IncrementalConfig;
import io.yunxi.mcp.nutrition.sync.config.SyncPipelineConfig.TransformConfig;
import io.yunxi.mcp.nutrition.sync.config.SyncPipelineConfigLoader;
import jakarta.annotation.PostConstruct;

/**
 * 同步引擎（通用管道编排）
 * <p>
 * 根据 sync-pipelines.yml 中定义的管道，从 MySQL 查询数据、转换、向量化并 upsert 到 Milvus。
 * 由 SyncPipelineConfigLoader 在启动后触发 syncAll()。
 * </p>
 *
 * @author yunxi-agent-platform
 * @version 2.0.0
 */
@Service
public class SyncEngine implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(SyncEngine.class);
    private final Gson gson = new Gson();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired(required = false)
    private SyncPipelineConfigLoader pipelineConfigLoader;

    @Autowired
    private SyncDatasourceRegistry datasourceRegistry;

    @Autowired
    private MilvusCollectionService collectionService;

    @Autowired
    private MilvusOperations milvusOps;

    @Autowired(required = false)
    private ExternalDbQueryService externalDbQueryService;

    @Autowired
    private EmbeddingBatchService embeddingBatchService;

    @Autowired(required = false)
    private SyncCursorRepository syncCursorRepository;

    @Value("${sync.batch-size:100}")
    private int batchSize;

    @Value("${sync.enabled:true}")
    private boolean enabled;

    @Value("${sync.auto-sync-on-startup:true}")
    private boolean autoSyncOnStartup;

    /** 单次查询最大行数（0 = 不限制，JDBC setMaxRows(0) 即无上限）。默认 0，大数据量场景可设非 0 值做保护。 */
    @Value("${sync.query-max-rows:0}")
    private int queryMaxRows;

    private List<SyncPipelineConfig> pipelineConfigs = new ArrayList<>();
    private final ReentrantLock syncLock = new ReentrantLock();

    /** 当前 JVM 会话中已成功完成同步的管道名称（防止同一次启动重复执行） */
    private final Set<String> completedSyncNames = ConcurrentHashMap.newKeySet();

    @Override
    @PostConstruct
    public void afterPropertiesSet() {
        // 校验阶段：提前校验配置，避免运行时才发现错误
        pipelineConfigs = (pipelineConfigLoader != null)
                ? pipelineConfigLoader.getPipelines() : new ArrayList<>();

        if (!enabled) {
            log.info("SyncEngine: 数据同步已禁用（sync.enabled=false），跳过");
            return;
        }

        if (pipelineConfigs.isEmpty()) {
            log.warn("SyncEngine: 未配置任何同步管道（sync.pipelines 为空），请检查 sync-pipelines.yml");
            return;
        }

        log.info("SyncEngine: 已加载 {} 个同步管道", pipelineConfigs.size());

        // fail-fast：逐一校验 datasource 可解析（default 复用 spring.datasource，无需在 sync.sources 中配置）
        for (SyncPipelineConfig cfg : pipelineConfigs) {
            String dsId = cfg.getSource().getDatasource();
            if (dsId == null || dsId.isBlank()) {
                throw new IllegalStateException(
                        "SyncEngine: 管道 " + cfg.getName() + " 未声明 source.datasource，已禁止向后兼容");
            }
            if (!datasourceRegistry.isResolvable(dsId)) {
                throw new IllegalStateException(
                        "SyncEngine: 管道 " + cfg.getName() + " 的 datasource='" + dsId
                                + "' 未在 sync.sources 中配置（default 自动复用 spring.datasource）");
            }
            log.info("  管道: {} -> {} (datasource={})", cfg.getName(), cfg.getTarget().getCollection(), dsId);
        }

        // 启动时自动执行全量同步（可通过 sync.auto-sync-on-startup=false 关闭）
        if (autoSyncOnStartup) {
            log.info("SyncEngine: 启动自动同步已启用，开始执行 {} 条管道", pipelineConfigs.size());
            try {
                syncAll();
            } catch (Exception e) {
                log.error("SyncEngine: 启动自动同步失败", e);
            }
        } else {
            log.info("SyncEngine: 启动自动同步已禁用（sync.auto-sync-on-startup=false）");
        }
    }

    /**
     * 执行指定名称的管道（用于 MCP Tool 按需触发）。
     */
    public void syncPipeline(String name) {
        SyncPipelineConfig cfg = pipelineConfigs.stream()
                .filter(p -> p.getName().equals(name))
                .findFirst()
                .orElse(null);
        if (cfg == null) {
            log.warn("SyncEngine: 未找到管道: {}", name);
            throw new IllegalArgumentException("未找到管道: " + name);
        }
        if (!cfg.isEnabled()) {
            log.warn("SyncEngine: 管道 {} 已被禁用", name);
            return;
        }
        executePipeline(cfg);
    }

    /**
     * 执行所有启用的管道
     */
    public void syncAll() {
        if (!enabled) {
            log.info("SyncEngine: 数据同步已禁用，跳过 syncAll");
            return;
        }

        if (!syncLock.tryLock()) {
            log.warn("SyncEngine.syncAll: 同步正在进行中，跳过本次执行（防止并发）");
            return;
        }

        try {
            log.info("SyncEngine: 开始执行 {} 个同步管道", pipelineConfigs.size());
            int successCount = 0;
            for (SyncPipelineConfig cfg : pipelineConfigs) {
                if (!cfg.isEnabled()) {
                    log.info("  跳过禁用的管道: {}", cfg.getName());
                    continue;
                }
                try {
                    executePipeline(cfg);
                    successCount++;
                } catch (Exception e) {
                    log.error("  管道执行失败: {}", cfg.getName(), e);
                }
            }
            log.info("SyncEngine: 同步完成，成功 {}/{}", successCount, pipelineConfigs.size());
        } finally {
            syncLock.unlock();
        }
    }

    /**
     * 执行单条管道
     */
    private void executePipeline(SyncPipelineConfig cfg) {
        String pipelineName = cfg.getName();
        String dsId = cfg.getSource().getDatasource();

        // 0. 防重：同一会话内已完成的管道直接跳过
        if (completedSyncNames.contains(pipelineName)) {
            log.info("管道 {} 已在本会话完成同步，跳过", pipelineName);
            return;
        }

        log.info("=== 开始同步管道: {} (datasource={}) ===", pipelineName, dsId);

        // 1. 确保集合存在（必须在数据查询之前，即使查询失败也建好 Schema）
        collectionService.ensureSyncCollection(cfg);

        // 2. 构建 SQL 并查询 MySQL
        String sql = buildSql(cfg);
        log.info("管道 {} 执行 SQL: {}", pipelineName, sql);

        String jsonResult = externalDbQueryService != null
                ? externalDbQueryService.queryByDsId(dsId, sql, queryMaxRows)
                : "[]";

        List<Map<String, Object>> rows = parseJsonResult(jsonResult);
        if (rows.isEmpty()) {
            log.warn("管道 {} 查询结果为空，跳过同步（集合 {} 已就绪）", pipelineName,
                    cfg.getTarget().getCollection());
            completedSyncNames.add(pipelineName);
            return;
        }

        int mysqlCount = rows.size();
        log.info("管道 {} MySQL 查询结果: {} 条", pipelineName, mysqlCount);

        // 3. 增量游标处理
        IncrementalConfig incremental = cfg.getSource().getIncremental();
        boolean isIncremental = incremental != null && incremental.getColumn() != null;
        if (isIncremental) {
            SyncCursorEntity last = readCursor(pipelineName);
            if (last != null && last.getCursorValue() != null) {
                log.info("管道 {} 增量同步，上次游标={}", pipelineName, last.getCursorValue());
            } else {
                log.info("管道 {} 首次全量同步（游标未保存，本次同步后将自动建立）", pipelineName);
            }
        }
        String collectionName = cfg.getTarget().getCollection();

        long syncedRows;

        if (isIncremental) {
            // 增量管道：upsert + 游标
            List<Map<String, Object>> enrichedRows = applyTransforms(cfg, rows);
            syncedRows = writeToMilvus(cfg, enrichedRows, incremental, null);
        } else {
            // 非增量管道：ID 级 diff 同步（只处理增量和删量，存量数据不动）
            syncedRows = diffSync(cfg, rows, collectionName, pipelineName, mysqlCount);
        }

        // 6. 验证同步数量（非增量管道必须与 MySQL 一致）
        if (!isIncremental) {
            verifyCount(collectionName, mysqlCount, syncedRows, pipelineName);
        }

        // 7. 更新增量游标（首次全量同步也必须保存，从结果行中计算最大游标值）
        if (isIncremental) {
            String newCursor = computeCursor(cfg, rows);
            if (newCursor != null) {
                updateCursor(cfg, newCursor, syncedRows);
                log.info("管道 {} 游标已更新: {}", pipelineName, newCursor);
            } else {
                log.warn("管道 {} 无法计算游标值（{} 列为空），下次仍将全量同步", pipelineName, incremental.getColumn());
            }
        }

        // 8. 标记完成
        completedSyncNames.add(pipelineName);
        log.info("=== 管道 {} 同步完成，MySQL {} 条 → Milvus {} 条 ===", pipelineName, mysqlCount, syncedRows);
    }

    /**
     * 清空 Milvus 集合（全量同步前调用，确保数据不叠加）
     */
    private void clearCollection(String collectionName, String pipelineName) {
        try {
            long beforeCount = milvusOps.getCollectionStatistics(collectionName);
            if (beforeCount > 0) {
                milvusOps.delete(collectionName, "id >= 0");
                log.info("管道 {} 已清空集合 {}（删除 {} 条）", pipelineName, collectionName, beforeCount);
            } else {
                log.info("管道 {} 集合 {} 为空，无需清空", pipelineName, collectionName);
            }
        } catch (Exception e) {
            log.error("管道 {} 清空集合失败: {} → {}", pipelineName, collectionName, e.getMessage());
            throw new RuntimeException("清空集合失败: " + collectionName, e);
        }
    }

    /**
     * 验证同步后 Milvus 数量与 MySQL 源数量一致
     */
    private void verifyCount(String collectionName, int mysqlCount, long syncedRows, String pipelineName) {
        try {
            long milvusCount = milvusOps.getCollectionStatistics(collectionName);
            if (milvusCount != mysqlCount) {
                log.error("管道 {} 数量校验失败！MySQL={} 条, Milvus={} 条（写入 {} 条）, 差值={}",
                        pipelineName, mysqlCount, milvusCount, syncedRows, milvusCount - mysqlCount);
            } else {
                log.info("管道 {} 数量校验通过：MySQL {} = Milvus {} ✓", pipelineName, mysqlCount, milvusCount);
            }
        } catch (Exception e) {
            log.warn("管道 {} 数量校验异常: {}", pipelineName, e.getMessage());
        }
    }

    /**
     * ID 级差异对比同步（非增量管道核心逻辑）。
     *
     * 对比 MySQL 与 Milvus 两端的主键 ID 集合：
     * - MySQL 有 / Milvus 无 → 向量化后 insert
     * - Milvus 有 / MySQL 无 → 删除过期记录
     * - 两端都有 → 跳过（存量不变，无需重新向量化）
     *
     * 这样重启时只会处理差异数据，大幅减少向量化耗时。
     */
    private long diffSync(SyncPipelineConfig cfg, List<Map<String, Object>> rows,
                          String collectionName, String pipelineName, int mysqlCount) {
        // 1. 获取主键字段名
        String pkField = cfg.getTarget().getPrimaryKey();
        if (pkField == null || pkField.isEmpty()) {
            log.warn("管道 {} 未配置主键字段，降级为全量清空+重写", pipelineName);
            return fallbackFullSync(cfg, rows, collectionName, pipelineName, mysqlCount);
        }

        // 2. 收集 MySQL ID 集合
        Set<String> mysqlIds = new LinkedHashSet<>();
        for (Map<String, Object> row : rows) {
            Object idVal = row.get(pkField);
            if (idVal != null) {
                mysqlIds.add(String.valueOf(idVal));
            }
        }
        if (mysqlIds.size() != mysqlCount) {
            log.warn("管道 {} MySQL 主键去重 {}/{}（可能有重复 ID），继续 diff", pipelineName, mysqlIds.size(),
                    mysqlCount);
        }

        // 3. 查询 Milvus 中已存在的所有 ID
        Set<String> milvusIds;
        try {
            milvusIds = milvusOps.queryAllIds(collectionName);
        } catch (Exception e) {
            log.warn("管道 {} 查询 Milvus ID 失败，降级为全量清空+重写: {}", pipelineName, e.getMessage());
            return fallbackFullSync(cfg, rows, collectionName, pipelineName, mysqlCount);
        }

        // 3.5 脏数据检测：queryAllIds 返回空但 Milvus 统计行数 > 0
        //   说明集合未加载或索引缺失，继续 diff 会导致全部当作"新增"→数据累积
        if (milvusIds.isEmpty()) {
            Long milvusCount = milvusOps.getCollectionStatistics(collectionName);
            if (milvusCount != null && milvusCount > 0) {
                log.warn("管道 {} 检测到脏数据：queryAllIds 返回 0 但 Milvus 统计行数={}, 降级为全量清空+重写",
                        pipelineName, milvusCount);
                return fallbackFullSync(cfg, rows, collectionName, pipelineName, mysqlCount);
            }
        }

        // 4. Diff
        Set<String> toInsert = new LinkedHashSet<>(mysqlIds);
        toInsert.removeAll(milvusIds);

        Set<String> toDelete = new LinkedHashSet<>(milvusIds);
        toDelete.removeAll(mysqlIds);

        log.info("管道 {} diff: MySQL={}, Milvus={}, +新增{}, -删除{}, =不变{}",
                pipelineName, mysqlIds.size(), milvusIds.size(),
                toInsert.size(), toDelete.size(),
                mysqlIds.size() - toInsert.size());

        // 5. 删除过期记录
        if (!toDelete.isEmpty()) {
            deleteRecordsByIds(collectionName, toDelete, pipelineName);
        }

        // 6. 无新增 → 跳过向量化（最快路径：存量不变，重启零开销）
        if (toInsert.isEmpty()) {
            log.info("管道 {} 无新增记录，跳过向量化（存量 {} 条不变）", pipelineName, mysqlIds.size());
            return 0;
        }

        // 7. 过滤仅新增的行 → 向量化 → 写入
        List<Map<String, Object>> newRows = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object idVal = row.get(pkField);
            if (idVal != null && toInsert.contains(String.valueOf(idVal))) {
                newRows.add(row);
            }
        }

        log.info("管道 {} 仅对 {} 条新增记录执行向量化（跳过 {} 条存量）",
                pipelineName, newRows.size(), mysqlIds.size() - toInsert.size());

        List<Map<String, Object>> enrichedRows = applyTransforms(cfg, newRows);
        long syncedRows = writeNewToMilvus(cfg, enrichedRows);

        return syncedRows;
    }

    /**
     * 降级方案：全量清空 + 全量重写（仅在 Milvus 查询失败时使用）。
     */
    private long fallbackFullSync(SyncPipelineConfig cfg, List<Map<String, Object>> rows,
                                  String collectionName, String pipelineName, int mysqlCount) {
        clearCollection(collectionName, pipelineName);
        List<Map<String, Object>> enrichedRows = applyTransforms(cfg, rows);
        long syncedRows = writeToMilvus(cfg, enrichedRows, null, null);
        verifyCount(collectionName, mysqlCount, syncedRows, pipelineName);
        return syncedRows;
    }

    /**
     * 将新增记录写入 Milvus（仅 insert，不 upsert 存量）。
     */
    private long writeNewToMilvus(SyncPipelineConfig cfg, List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return 0;
        }
        // 复用 writeToMilvus，但不传增量参数（全量写入）
        return writeToMilvus(cfg, rows, null, null);
    }

    /**
     * 按主键 ID 分批删除 Milvus 中的过期记录。
     */
    private void deleteRecordsByIds(String collectionName, Set<String> ids, String pipelineName) {
        List<String> idList = new ArrayList<>(ids);
        int deleteBatchSize = 500;
        for (int i = 0; i < idList.size(); i += deleteBatchSize) {
            int end = Math.min(i + deleteBatchSize, idList.size());
            List<String> batch = idList.subList(i, end);
            String idsExpr = String.join(", ", batch);
            String filter = String.format("id in [%s]", idsExpr);
            try {
                milvusOps.delete(collectionName, filter);
                log.info("管道 {} 删除过期记录 {}/{} 条", pipelineName, i + batch.size(), idList.size());
            } catch (Exception e) {
                log.error("管道 {} 删除过期记录失败: {}", pipelineName, e.getMessage());
            }
        }
    }

    /**
     * 构建 SQL（增量时追加游标条件）
     */
    private String buildSql(SyncPipelineConfig cfg) {
        String baseSql = cfg.getSource().getSql();
        IncrementalConfig incremental = cfg.getSource().getIncremental();
        if (incremental != null && incremental.getColumn() != null) {
            SyncCursorEntity last = readCursor(cfg.getName());
            if (last != null && last.getCursorValue() != null) {
                String cursor = last.getCursorValue();
                // 支持 ? 占位符
                if (baseSql.contains("?")) {
                    return baseSql.replace("?", "'" + cursor + "'");
                }
                return baseSql + " AND " + incremental.getColumn() + " > '" + cursor + "'";
            }
        }
        return baseSql;
    }

    /**
     * 读取增量游标
     */
    private SyncCursorEntity readCursor(String pipelineName) {
        try {
            return syncCursorRepository != null ? syncCursorRepository.findByPipelineName(pipelineName) : null;
        } catch (Exception e) {
            log.warn("读取增量游标失败: {}", pipelineName, e);
            return null;
        }
    }

    /**
     * 更新增量游标
     */
    private void updateCursor(SyncPipelineConfig cfg, String cursor, long syncedRows) {
        try {
            SyncCursorEntity entity = new SyncCursorEntity();
            entity.setPipelineName(cfg.getName());
            entity.setCursorValue(cursor);
            entity.setSyncedRows(syncedRows);
            entity.setUpdatedAt(LocalDateTime.now());
            if (syncCursorRepository != null) {
                syncCursorRepository.upsert(entity);
            }
        } catch (Exception e) {
            log.warn("更新增量游标失败: {}", cfg.getName(), e);
        }
    }

    /**
     * 从结果行中计算增量游标值（取 incremental 列的最大值）
     */
    private String computeCursor(SyncPipelineConfig cfg, List<Map<String, Object>> rows) {
        IncrementalConfig inc = cfg.getSource().getIncremental();
        if (inc == null || inc.getColumn() == null || rows == null || rows.isEmpty()) {
            return null;
        }
        String col = inc.getColumn();
        Comparable<Object> maxVal = null;
        for (Map<String, Object> row : rows) {
            Object val = row.get(col);
            if (val == null) continue;
            if (maxVal == null) {
                @SuppressWarnings("unchecked")
                Comparable<Object> cv = (Comparable<Object>) val;
                maxVal = cv;
            } else {
                @SuppressWarnings("unchecked")
                Comparable<Object> cv = (Comparable<Object>) val;
                if (cv.compareTo(maxVal) > 0) {
                    maxVal = cv;
                }
            }
        }
        if (maxVal != null) {
            return maxVal.toString();
        }
        // 回退到 initialValue
        return inc.getInitialValue();
    }

    /**
     * 写入 Milvus（向量化 + upsert）
     */
    private long writeToMilvus(SyncPipelineConfig cfg, List<Map<String, Object>> rows,
                               IncrementalConfig incremental, String cursorValue) {
        if (!milvusOps.isAvailable()) {
            log.error("Milvus 不可用，跳过向量写入: {}", cfg.getTarget().getCollection());
            return 0;
        }

        // 提取向量文本
        List<String> embedTexts = extractEmbedTexts(cfg, rows);
        if (embedTexts.isEmpty()) {
            log.warn("管道 {} 无向量文本，跳过", cfg.getName());
            return 0;
        }

        // 批量向量化
        List<List<Float>> vectors = embeddingBatchService.embedBatchWithRetry(embedTexts);
        if (vectors.isEmpty() || vectors.size() != rows.size()) {
            log.error("向量化结果数量不匹配: vectors={}, rows={}", vectors.size(), rows.size());
            return 0;
        }

        // 构建 Milvus 数据行
        List<JsonObject> dataList = buildMilvusData(cfg, rows, vectors);
        if (dataList.isEmpty()) {
            return 0;
        }

        // 分批 upsert
        collectionService.upsertBatch(cfg.getTarget().getCollection(), dataList, batchSize);
        return dataList.size();
    }

    /**
     * 应用转换配置：将模板渲染结果写入目标字段（如 embedding_text）。
     */
    private List<Map<String, Object>> applyTransforms(SyncPipelineConfig cfg, List<Map<String, Object>> rows) {
        List<TransformConfig> transforms = cfg.getTransform();
        if (transforms == null || transforms.isEmpty()) {
            return rows;
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> enriched = new LinkedHashMap<>(row);
            for (TransformConfig t : transforms) {
                if (t.getField() == null || t.getTemplate() == null) continue;
                enriched.put(t.getField(), renderTemplate(t.getTemplate(), row));
            }
            result.add(enriched);
        }
        return result;
    }

    /**
     * 渲染模板：将 {字段名} 替换为数据行对应值。
     */
    private String renderTemplate(String template, Map<String, Object> row) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\{(\\w+)\\}");
        java.util.regex.Matcher m = p.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            Object v = row.get(m.group(1));
            m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(v != null ? v.toString() : ""));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * 提取向量文本（从转换后的 embedding_text 字段；多转换则拼接）
     */
    private List<String> extractEmbedTexts(SyncPipelineConfig cfg, List<Map<String, Object>> rows) {
        List<String> texts = new ArrayList<>();
        List<TransformConfig> transforms = cfg.getTransform();
        if (transforms == null || transforms.isEmpty()) {
            log.warn("管道 {} 未配置 transform，无法提取向量文本", cfg.getName());
            return texts;
        }
        for (Map<String, Object> row : rows) {
            StringBuilder sb = new StringBuilder();
            for (TransformConfig t : transforms) {
                Object v = t.getField() != null ? row.get(t.getField()) : null;
                if (v != null) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(v);
                }
            }
            texts.add(sb.toString());
        }
        return texts;
    }

    /**
     * 构建 Milvus 数据行
     */
    private List<JsonObject> buildMilvusData(SyncPipelineConfig cfg, List<Map<String, Object>> rows,
                                             List<List<Float>> vectors) {
        List<JsonObject> result = new ArrayList<>();
        List<FieldConfig> fields = cfg.getTarget().getFields();
        if (fields == null || fields.isEmpty()) {
            log.error("管道 {} 未配置 target.fields", cfg.getName());
            return result;
        }

        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            JsonObject data = new JsonObject();

            for (FieldConfig f : fields) {
                String fieldName = f.getName();
                if ("embedding".equals(fieldName)) {
                    // 向量字段
                    List<Float> vec = vectors.get(i);
                    com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
                    for (Float v : vec) {
                        arr.add(v);
                    }
                    data.add(fieldName, arr);
                } else {
                    Object value = row.get(f.getName());
                    if (value == null) {
                        // Milvus 非向量字段默认 non-nullable，按类型填安全值
                        String ftype = f.getType();
                        if ("VarChar".equals(ftype)) {
                            data.addProperty(fieldName, "");
                        } else {
                            log.warn("字段 {} (type={}) 值为 null，跳过该字段（建议 SQL 层用 COALESCE 处理）",
                                    fieldName, ftype);
                        }
                    } else if (value instanceof Number) {
                        data.addProperty(fieldName, (Number) value);
                    } else if (value instanceof Boolean) {
                        data.addProperty(fieldName, (Boolean) value);
                    } else {
                        data.addProperty(fieldName, value.toString());
                    }
                }
            }
            result.add(data);
        }
        return result;
    }

    /**
     * 解析 JSON 结果为 List<Map>
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseJsonResult(String jsonResult) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (jsonResult == null || jsonResult.isBlank()) {
            return result;
        }
        try {
            JsonNode root = objectMapper.readTree(jsonResult);
            if (root.isArray()) {
                for (JsonNode node : root) {
                    Map<String, Object> map = objectMapper.convertValue(node, Map.class);
                    result.add(map);
                }
            } else if (root.has("error")) {
                log.error("查询返回错误: {}", root.get("error").asText());
            }
        } catch (Exception e) {
            log.error("解析查询结果失败", e);
        }
        return result;
    }

    /**
     * @deprecated 逻辑已迁移至 {@link MilvusCollectionService#ensureSyncCollection(SyncPipelineConfig)}，
     * 保留此方法仅为向后兼容，不应再被调用。
     */
    @Deprecated
    public void ensureCollection(SyncPipelineConfig cfg) {
        collectionService.ensureSyncCollection(cfg);
    }

    public List<SyncPipelineConfig> getPipelineConfigs() {
        return pipelineConfigs;
    }

    public void setPipelineConfigs(List<SyncPipelineConfig> pipelineConfigs) {
        this.pipelineConfigs = pipelineConfigs;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
