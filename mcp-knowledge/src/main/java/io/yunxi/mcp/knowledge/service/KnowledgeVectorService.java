package io.yunxi.mcp.knowledge.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp;
import io.yunxi.mcp.knowledge.config.MilvusClientConfig;
import io.yunxi.mcp.knowledge.embedding.EmbeddingService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;

/**
 * 向量知识服务
 * 实现知识的向量存储和语义搜索
 *
 * @author yunxi-mcp-servers
 */
@Slf4j
@Service
public class KnowledgeVectorService {

    private static final Gson gson = new Gson();

    @Autowired
    private MilvusClientConfig milvusClientConfig;

    @Autowired
    private EmbeddingService embeddingService;

    private MilvusClientV2 getClient() {
        return milvusClientConfig.getClient();
    }

    private String getCollectionName() {
        return milvusClientConfig.getCollectionName();
    }

    @PostConstruct
    public void init() {
        log.info("KnowledgeVectorService 初始化完成");
    }

    /**
     * 添加知识条目（自动向量化）
     *
     * @param item 知识条目
     * @return 是否添加成功
     */
    public boolean addKnowledge(KnowledgeItem item) {
        try {
            // 生成向量
            String textToEmbed = item.getTitle() + " " + item.getContent() + " "
                    + (item.getTags() != null ? item.getTags() : "");
            List<Float> embedding = embeddingService.embed(textToEmbed);

            if (embedding == null || embedding.isEmpty()) {
                log.error("知识向量化失败: {}", item.getTitle());
                return false;
            }

            // 构建插入数据 - 使用 Gson JsonObject
            JsonObject data = new JsonObject();
            data.addProperty("id", item.getId() != null ? item.getId() : UUID.randomUUID().toString());
            data.addProperty("district_id", item.getDistrictId() != null ? item.getDistrictId() : "");
            data.addProperty("title", item.getTitle());
            data.addProperty("content", item.getContent());
            data.addProperty("type", item.getType() != null ? item.getType() : "general");
            data.addProperty("tags", item.getTags() != null ? item.getTags() : "");
            // Convert List<Float> to float[] for FloatVec
            float[] embeddingArray = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                embeddingArray[i] = embedding.get(i);
            }
            data.add("embedding", gson.toJsonTree(embeddingArray));

            io.milvus.v2.service.vector.request.InsertReq insertReq = io.milvus.v2.service.vector.request.InsertReq
                    .builder()
                    .collectionName(getCollectionName())
                    .data(Collections.singletonList(data))
                    .build();

            getClient().insert(insertReq);
            log.info("知识添加成功: {}", item.getTitle());
            return true;

        } catch (Exception e) {
            log.error("添加知识失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 向量语义搜索
     *
     * @param keyword    搜索关键词
     * @param districtId 区县ID（可选，用于过滤）
     * @param limit      返回结果数量限制
     * @return 知识条目列表
     */
    public List<KnowledgeItem> search(String keyword, String districtId, int limit) {
        try {
            // 将关键词向量化
            List<Float> queryEmbedding = embeddingService.embed(keyword);
            if (queryEmbedding == null || queryEmbedding.isEmpty()) {
                log.error("关键词向量化失败");
                return Collections.emptyList();
            }

            // 构建搜索请求
            SearchReq.SearchReqBuilder searchBuilder = SearchReq.builder()
                    .collectionName(getCollectionName())
                    .annsField("embedding")
                    .data(Collections.singletonList(new FloatVec(toFloatArray(queryEmbedding))))
                    .topK(limit)
                    .outputFields(Arrays.asList("id", "district_id", "title", "content", "type", "tags"));

            // 添加过滤条件 (可选: 按区县过滤)
            if (districtId != null && !districtId.isEmpty()) {
                searchBuilder.filter("district_id == '" + districtId + "'");
            }

            SearchReq searchReq = searchBuilder.build();
            SearchResp searchResp = getClient().search(searchReq);

            // 解析结果
            List<KnowledgeItem> results = new ArrayList<>();
            if (searchResp.getSearchResults() != null) {
                for (Object entity : searchResp.getSearchResults()) {
                    if (entity instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) entity;
                        KnowledgeItem item = new KnowledgeItem();
                        item.setId((String) map.get("id"));
                        item.setDistrictId((String) map.get("district_id"));
                        item.setTitle((String) map.get("title"));
                        item.setContent((String) map.get("content"));
                        item.setType((String) map.get("type"));
                        item.setTags((String) map.get("tags"));
                        results.add(item);
                    }
                }
            }

            log.info("语义搜索完成, 关键词: {}, 结果数: {}", keyword, results.size());
            return results;

        } catch (Exception e) {
            log.error("搜索知识失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 删除知识
     *
     * @param id 知识ID
     * @return 是否删除成功
     */
    public boolean deleteKnowledge(String id) {
        try {
            io.milvus.v2.service.vector.request.DeleteReq deleteReq = io.milvus.v2.service.vector.request.DeleteReq
                    .builder()
                    .collectionName(getCollectionName())
                    .filter("id == '" + id + "'")
                    .build();

            getClient().delete(deleteReq);
            log.info("知识删除成功: {}", id);
            return true;

        } catch (Exception e) {
            log.error("删除知识失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取知识列表
     *
     * @param districtId 区县ID（可选）
     * @param type       知识类型（可选）
     * @param limit      返回结果数量限制
     * @return 知识条目列表
     */
    public List<KnowledgeItem> listKnowledge(String districtId, String type, int limit) {
        try {
            // 构建查询请求 (基于过滤条件)
            io.milvus.v2.service.vector.request.QueryReq.QueryReqBuilder queryBuilder = io.milvus.v2.service.vector.request.QueryReq
                    .builder()
                    .collectionName(getCollectionName())
                    .outputFields(Arrays.asList("id", "district_id", "title", "content", "type", "tags"))
                    .limit((long) limit);

            // 构建过滤条件
            StringBuilder filter = new StringBuilder();
            if (districtId != null && !districtId.isEmpty()) {
                filter.append("district_id == '").append(districtId).append("'");
            }
            if (type != null && !type.isEmpty()) {
                if (filter.length() > 0) {
                    filter.append(" and ");
                }
                filter.append("type == '").append(type).append("'");
            }

            if (filter.length() > 0) {
                queryBuilder.filter(filter.toString());
            }

            io.milvus.v2.service.vector.request.QueryReq queryReq = queryBuilder.build();
            io.milvus.v2.service.vector.response.QueryResp queryResp = getClient().query(queryReq);

            // 解析结果
            List<KnowledgeItem> results = new ArrayList<>();
            if (queryResp.getQueryResults() != null) {
                for (Object entity : queryResp.getQueryResults()) {
                    if (entity instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) entity;
                        KnowledgeItem item = new KnowledgeItem();
                        item.setId((String) map.get("id"));
                        item.setDistrictId((String) map.get("district_id"));
                        item.setTitle((String) map.get("title"));
                        item.setContent((String) map.get("content"));
                        item.setType((String) map.get("type"));
                        item.setTags((String) map.get("tags"));
                        results.add(item);
                    }
                }
            }

            log.info("知识列表查询完成, 结果数: {}", results.size());
            return results;

        } catch (Exception e) {
            log.error("获取知识列表失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 将 List<Float> 转换为 float[]
     *
     * @param list 浮点数列表
     * @return 浮点数组
     */
    private float[] toFloatArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    /**
     * 知识条目
     * 用于存储知识的结构化数据
     */
    @Data
    public static class KnowledgeItem {
        private String id;
        private String districtId;
        private String title;
        private String content;
        private String type;
        private String tags;
    }
}