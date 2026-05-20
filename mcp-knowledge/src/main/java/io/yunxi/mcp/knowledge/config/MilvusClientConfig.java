package io.yunxi.mcp.knowledge.config;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

/**
 * Milvus 客户端配置
 *
 * @author yunxi-mcp-servers
 */
@Slf4j
@Component
public class MilvusClientConfig {

        @Autowired
        private MilvusProperties milvusProperties;

        private MilvusClientV2 milvusClient;

        /** 字段名常量 */
        private static final String FIELD_ID = "id";
        private static final String FIELD_DISTRICT_ID = "district_id";
        private static final String FIELD_TITLE = "title";
        private static final String FIELD_CONTENT = "content";
        private static final String FIELD_TYPE = "type";
        private static final String FIELD_TAGS = "tags";
        private static final String FIELD_EMBEDDING = "embedding";

        @PostConstruct
        public void init() {
                try {
                        // 创建 Milvus 客户端
                        ConnectConfig connectConfig = ConnectConfig.builder()
                                        .uri("http://" + milvusProperties.getHost() + ":" + milvusProperties.getPort())
                                        .build();
                        milvusClient = new MilvusClientV2(connectConfig);

                        // 确保集合存在
                        createCollectionIfNotExists();
                        log.info("Milvus 客户端初始化完成, collection: {}", milvusProperties.getCollectionName());
                } catch (Exception e) {
                        log.error("Milvus 客户端初始化失败: {}", e.getMessage(), e);
                }
        }

        private void createCollectionIfNotExists() {
                try {
                        // 检查集合是否已存在
                        HasCollectionReq hasReq = HasCollectionReq.builder()
                                        .collectionName(milvusProperties.getCollectionName())
                                        .build();

                        if (milvusClient.hasCollection(hasReq)) {
                                log.debug("知识库集合已存在: {}", milvusProperties.getCollectionName());
                                return;
                        }

                        // 创建集合 schema
                        CreateCollectionReq.FieldSchema idField = CreateCollectionReq.FieldSchema.builder()
                                        .name(FIELD_ID).dataType(DataType.VarChar).maxLength(64)
                                        .isPrimaryKey(true).autoID(false).build();

                        CreateCollectionReq.FieldSchema districtIdField = CreateCollectionReq.FieldSchema.builder()
                                        .name(FIELD_DISTRICT_ID).dataType(DataType.VarChar).maxLength(32).build();

                        CreateCollectionReq.FieldSchema titleField = CreateCollectionReq.FieldSchema.builder()
                                        .name(FIELD_TITLE).dataType(DataType.VarChar).maxLength(255).build();

                        CreateCollectionReq.FieldSchema contentField = CreateCollectionReq.FieldSchema.builder()
                                        .name(FIELD_CONTENT).dataType(DataType.VarChar).maxLength(4096).build();

                        CreateCollectionReq.FieldSchema typeField = CreateCollectionReq.FieldSchema.builder()
                                        .name(FIELD_TYPE).dataType(DataType.VarChar).maxLength(50).build();

                        CreateCollectionReq.FieldSchema tagsField = CreateCollectionReq.FieldSchema.builder()
                                        .name(FIELD_TAGS).dataType(DataType.VarChar).maxLength(255).build();

                        CreateCollectionReq.FieldSchema embeddingField = CreateCollectionReq.FieldSchema.builder()
                                        .name(FIELD_EMBEDDING).dataType(DataType.FloatVector).dimension(1024).build();

                        // 构建集合 schema
                        CreateCollectionReq.CollectionSchema schema = CreateCollectionReq.CollectionSchema.builder()
                                        .fieldSchemaList(Arrays.asList(
                                                        idField, districtIdField, titleField, contentField,
                                                        typeField, tagsField, embeddingField))
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
                                        .collectionName(milvusProperties.getCollectionName())
                                        .collectionSchema(schema)
                                        .indexParams(Collections.singletonList(indexParam))
                                        .build();

                        milvusClient.createCollection(createReq);
                        log.info("知识库集合创建完成: {}", milvusProperties.getCollectionName());

                } catch (Exception e) {
                        log.error("创建 Milvus 集合失败: {}", e.getMessage(), e);
                }
        }

        public MilvusClientV2 getClient() {
                return milvusClient;
        }

        public String getCollectionName() {
                return milvusProperties.getCollectionName();
        }
}
