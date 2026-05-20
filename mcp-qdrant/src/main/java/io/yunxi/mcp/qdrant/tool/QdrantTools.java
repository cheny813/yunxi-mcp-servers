package io.yunxi.mcp.qdrant.tool;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points.SearchPoints;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.WithPayloadSelector;
import io.yunxi.mcp.qdrant.QdrantConfig;
import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Qdrant 向量数据库工具
 * <p>
 * 提供 Qdrant 向量数据库的 MCP 工具实现。
 * 支持向量搜索、集合管理、点数据操作等。
 * </p>
 */
@Slf4j
@Component
public class QdrantTools {

    private final QdrantConfig config;
    private final QdrantClient client;

    @Autowired
    public QdrantTools(QdrantConfig config, QdrantClient client) {
        this.config = config;
        this.client = client;
    }

    /**
     * 向量搜索工具
     */
    public static class SearchTool implements ToolHandler {
        private final QdrantTools parent;

        @Autowired
        public SearchTool(QdrantTools parent) {
            this.parent = parent;
        }

        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("qdrant_search")
                    .description(
                            "Qdrant 向量搜索 (Vector Search) - 在向量数据库中搜索相似内容。" +
                                    "典型用例：语义搜索、相似图片查找、推荐系统。")
                    .inputSchema(QdrantTools.schema(
                            "collection", "string", "Collection name. 集合名称。",
                            "query_vector", "array", "Query vector. 查询向量。",
                            "top_k", "integer", "Number of results (default 10). 返回结果数。",
                            "score_threshold", "float", "Minimum score (0-1). 最小相似度分数。")
                    )
                    .build();
        }

        @Override
        public ToolResult execute(Map<String, Object> args) {
            String collection = (String) args.get("collection");
            Object queryVector = args.get("query_vector");
            Integer topK = args.containsKey("top_k") ? ((Number) args.get("top_k")).intValue() : null;
            Double scoreThreshold = args.containsKey("score_threshold")
                    ? ((Number) args.get("score_threshold")).doubleValue() : null;

            if (collection == null || collection.isBlank()) {
                return ToolResult.error("Error: collection is required");
            }

            if (queryVector == null) {
                return ToolResult.error("Error: query_vector is required");
            }

            return parent.search(collection, queryVector, topK, scoreThreshold);
        }
    }

    /**
     * 列出集合工具
     */
    public static class ListCollectionsTool implements ToolHandler {
        private final QdrantTools parent;

        @Autowired
        public ListCollectionsTool(QdrantTools parent) {
            this.parent = parent;
        }

        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("qdrant_list_collections")
                    .description(
                            "列出 Qdrant 集合 (List Collections) - 获取所有集合的名称。" +
                                    "典型用例：查看有哪些向量集合、检查数据分布。")
                    .inputSchema(QdrantTools.schema())
                    .build();
        }

        @Override
        public ToolResult execute(Map<String, Object> args) {
            return parent.listCollections();
        }
    }

    /**
     * 获取集合信息工具
     */
    public static class GetCollectionInfoTool implements ToolHandler {
        private final QdrantTools parent;

        @Autowired
        public GetCollectionInfoTool(QdrantTools parent) {
            this.parent = parent;
        }

        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("qdrant_collection_info")
                    .description(
                            "获取集合信息 (Collection Info) - 获取指定集合的详细信息。" +
                                    "典型用例：查看集合状态、向量维度、索引配置。")
                    .inputSchema(QdrantTools.schema(
                            "collection", "string", "Collection name. 集合名称。")
                    )
                    .build();
        }

        @Override
        public ToolResult execute(Map<String, Object> args) {
            String collection = (String) args.get("collection");
            if (collection == null || collection.isBlank()) {
                return ToolResult.error("Error: collection is required");
            }
            return parent.getCollectionInfo(collection);
        }
    }

    /**
     * 执行向量搜索
     */
    private ToolResult search(String collection, Object queryVector,
                               Integer topK, Double scoreThreshold) {
        try {
            int k = topK != null ? topK : config.getDefaultTopK();
            double threshold = scoreThreshold != null ? scoreThreshold : config.getDefaultScoreThreshold();

            // 转换向量
            List<Float> vectorList;
            if (queryVector instanceof List) {
                @SuppressWarnings("unchecked")
                List<Number> numList = (List<Number>) queryVector;
                vectorList = new ArrayList<>(numList.stream().map(Number::floatValue).toList());
            } else {
                return ToolResult.error("Error: query_vector must be a list of numbers");
            }

            // 构建 SearchPoints 请求
            SearchPoints searchPoints = SearchPoints.newBuilder()
                    .setCollectionName(collection)
                    .addAllVector(vectorList)
                    .setLimit(k)
                    .setWithPayload(WithPayloadSelector.newBuilder().setEnable(true))
                    .build();

            // 执行搜索
            List<ScoredPoint> results = client.searchAsync(searchPoints).get();

            if (results.isEmpty()) {
                return ToolResult.text("No results found in collection: " + collection);
            }

            StringBuilder sb = new StringBuilder();
            sb.append("🔍 搜索结果 (Collection: ").append(collection).append(")\n\n");

            int index = 1;
            for (ScoredPoint result : results) {
                double score = result.getScore();
                if (score < threshold) continue;

                sb.append(index).append(". Score: ").append(String.format("%.4f", score)).append("\n");
                sb.append("\n");
                index++;
            }

            if (index == 1) {
                return ToolResult.text("No results above threshold " + threshold);
            }

            return ToolResult.text(sb.toString());

        } catch (Exception e) {
            log.error("Qdrant search error: {}", e.getMessage(), e);
            return ToolResult.error("Error: " + e.getMessage());
        }
    }

    /**
     * 列出所有集合
     */
    private ToolResult listCollections() {
        try {
            List<String> collections = client.listCollectionsAsync().get();

            StringBuilder sb = new StringBuilder();
            sb.append("📁 Qdrant Collections (共 ").append(collections.size()).append(" 个):\n\n");

            for (String name : collections) {
                sb.append("• ").append(name).append("\n");
            }

            return ToolResult.text(sb.toString());

        } catch (Exception e) {
            log.error("List collections error: {}", e.getMessage(), e);
            return ToolResult.error("Error: " + e.getMessage());
        }
    }

    /**
     * 获取集合信息
     */
    private ToolResult getCollectionInfo(String collection) {
        try {
            var info = client.getCollectionInfoAsync(collection).get();

            StringBuilder sb = new StringBuilder();
            sb.append("📊 Collection: ").append(collection).append("\n\n");
            sb.append("Status: ").append(info.getStatus()).append("\n");
            sb.append("Vectors Count: ").append(info.getVectorsCount()).append("\n");
            sb.append("Points Count: ").append(info.getPointsCount()).append("\n");

            return ToolResult.text(sb.toString());

        } catch (Exception e) {
            log.error("Get collection info error: {}", e.getMessage(), e);
            return ToolResult.error("Error: " + e.getMessage());
        }
    }

    /**
     * 构建输入参数 Schema
     */
    private static Map<String, Object> schema(String... props) {
        Map<String, Object> s = new HashMap<>();
        s.put("type", "object");
        Map<String, Object> properties = new HashMap<>();
        for (int i = 0; i < props.length; i += 3) {
            Map<String, Object> p = new HashMap<>();
            p.put("type", props[i + 1]);
            p.put("description", props[i + 2]);
            properties.put(props[i], p);
        }
        s.put("properties", properties);
        return s;
    }
}
