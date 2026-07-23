package io.yunxi.mcp.nutrition.sync.config;

import java.util.ArrayList;
import java.util.List;

/**
 * 同步管道配置
 * <p>
 * 对应 sync-pipelines.yml 中 static-sync.pipelines 的每一项，
 * 描述一个从 MySQL 到 Milvus 的同步任务（含数据源、SQL、转换、目标集合与索引）。
 * </p>
 *
 * @author yunxi-agent-platform
 * @version 2.0.0
 */
public class SyncPipelineConfig {

    /** 管道名称（对应 Milvus 集合名） */
    private String name;

    /** 是否启用 */
    private boolean enabled = true;

    /** 定时调度表达式（Cron） */
    private String schedule;

    /** 数据源配置 */
    private SourceConfig source = new SourceConfig();

    /** 转换配置（向量文本拼接规则） */
    private List<TransformConfig> transform = new ArrayList<>();

    /** 目标集合配置 */
    private TargetConfig target = new TargetConfig();

    public static class SourceConfig {
        /** 数据源名称（必须在 sync.sources 注册表中配置） */
        private String datasource;
        /** 查询 SQL（增量可用 ? 作为游标占位符） */
        private String sql;
        /** 增量配置（null 表示全量） */
        private IncrementalConfig incremental;

        public String getDatasource() { return datasource; }
        public void setDatasource(String datasource) { this.datasource = datasource; }
        public String getSql() { return sql; }
        public void setSql(String sql) { this.sql = sql; }
        public IncrementalConfig getIncremental() { return incremental; }
        public void setIncremental(IncrementalConfig incremental) { this.incremental = incremental; }
    }

    public static class IncrementalConfig {
        /** 作为游标的列名 */
        private String column;
        /** 首次全量同步的初始游标值 */
        private String initialValue;

        public String getColumn() { return column; }
        public void setColumn(String column) { this.column = column; }
        public String getInitialValue() { return initialValue; }
        public void setInitialValue(String initialValue) { this.initialValue = initialValue; }
    }

    public static class TransformConfig {
        /** 转换类型：concat（拼接模板） */
        private String type;
        /** 目标字段名（如 embedding_text） */
        private String field;
        /** 模板字符串，支持 {字段名} 占位符，从源数据行取值替换 */
        private String template;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getField() { return field; }
        public void setField(String field) { this.field = field; }
        public String getTemplate() { return template; }
        public void setTemplate(String template) { this.template = template; }
    }

    public static class TargetConfig {
        /** Milvus 集合名称 */
        private String collection;
        /** 集合描述 */
        private String collectionDescription;
        /** 主键字段名 */
        private String primaryKey;
        /** 字段定义 */
        private List<FieldConfig> fields = new ArrayList<>();
        /** 索引配置 */
        private IndexConfig index;

        public String getCollection() { return collection; }
        public void setCollection(String collection) { this.collection = collection; }
        public String getCollectionDescription() { return collectionDescription; }
        public void setCollectionDescription(String collectionDescription) { this.collectionDescription = collectionDescription; }
        public String getPrimaryKey() { return primaryKey; }
        public void setPrimaryKey(String primaryKey) { this.primaryKey = primaryKey; }
        public List<FieldConfig> getFields() { return fields; }
        public void setFields(List<FieldConfig> fields) { this.fields = fields; }
        public IndexConfig getIndex() { return index; }
        public void setIndex(IndexConfig index) { this.index = index; }
    }

    public static class FieldConfig {
        /** 字段名 */
        private String name;
        /** 字段类型：Int64 / VarChar / Float / FloatVector */
        private String type;
        /** 是否主键 */
        private boolean primaryKey = false;
        /** 向量维度（仅 FloatVector 需要） */
        private Integer dimension;
        /** 分级（用于推导维度，可选） */
        private String grade;
        /** VarChar 最大长度 */
        private Integer maxLength;
        /** 字段描述 */
        private String description;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public boolean isPrimaryKey() { return primaryKey; }
        public void setPrimaryKey(boolean primaryKey) { this.primaryKey = primaryKey; }
        public Integer getDimension() { return dimension; }
        public void setDimension(Integer dimension) { this.dimension = dimension; }
        public String getGrade() { return grade; }
        public void setGrade(String grade) { this.grade = grade; }
        public Integer getMaxLength() { return maxLength; }
        public void setMaxLength(Integer maxLength) { this.maxLength = maxLength; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class IndexConfig {
        /** 索引字段 */
        private String field;
        /** 索引类型 */
        private String indexType;
        /** 度量类型 */
        private String metricType;

        public String getField() { return field; }
        public void setField(String field) { this.field = field; }
        public String getIndexType() { return indexType; }
        public void setIndexType(String indexType) { this.indexType = indexType; }
        public String getMetricType() { return metricType; }
        public void setMetricType(String metricType) { this.metricType = metricType; }
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getSchedule() { return schedule; }
    public void setSchedule(String schedule) { this.schedule = schedule; }
    public SourceConfig getSource() { return source; }
    public void setSource(SourceConfig source) { this.source = source; }
    public List<TransformConfig> getTransform() { return transform; }
    public void setTransform(List<TransformConfig> transform) { this.transform = transform; }
    public TargetConfig getTarget() { return target; }
    public void setTarget(TargetConfig target) { this.target = target; }
}
