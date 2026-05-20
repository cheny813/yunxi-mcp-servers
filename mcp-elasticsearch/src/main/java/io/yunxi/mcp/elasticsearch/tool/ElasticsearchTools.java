package io.yunxi.mcp.elasticsearch.tool;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.indices.*;
import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;

import java.util.HashMap;
import java.util.Map;

/**
 * Elasticsearch 工具集合
 * <p>
 * 提供 Elasticsearch 数据库操作的 MCP 工具实现，包括搜索文档、索引文档、列出索引和删除文档。
 * 使用 Elasticsearch Java Client 进行操作。
 * </p>
 *
 * <h3>提供的工具</h3>
 * <ul>
 * <li>{@link SearchTool} - 搜索文档</li>
 * <li>{@link IndexDocumentTool} - 索引文档</li>
 * <li>{@link ListIndicesTool} - 列出索引</li>
 * <li>{@link DeleteDocumentTool} - 删除文档</li>
 * </ul>
 */
public class ElasticsearchTools {

    /**
     * Elasticsearch 客户端
     * <p>
     * 用于执行 Elasticsearch 操作的客户端实例。
     * </p>
     */
    private final ElasticsearchClient client;

    /**
     * 构造函数
     *
     * @param client Elasticsearch 客户端
     */
    public ElasticsearchTools(ElasticsearchClient client) {
        this.client = client;
    }

    /**
     * 搜索文档
     * <p>
     * 工具名称: {@code search}
     * </p>
     * <p>
     * 在 Elasticsearch 中搜索文档，返回匹配的文档数量和结果。
     * </p>
     */
    public static class SearchTool implements ToolHandler {
        /**
         * Elasticsearch 客户端
         */
        private final ElasticsearchClient client;

        /**
         * 构造函数
         *
         * @param client Elasticsearch 客户端
         */
        public SearchTool(ElasticsearchClient client) {
            this.client = client;
        }

        /**
         * 获取工具定义
         *
         * @return 工具定义对象
         */
        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("search")
                    .description(
                            "Search documents in Elasticsearch. " +
                                    "在Elasticsearch中搜索文档。 " +
                                    "Use this when you need to search logs, find documents, or analyze indexed data. " +
                                    "适用于搜索日志、查找文档或分析索引数据等场景。 " +
                                    "Common use cases: log search, document retrieval, data analysis. " +
                                    "典型用例：日志搜索、文档检索、数据分析。")
                    .inputSchema(schema(
                            "index", "string", "Index name. Example: 'logs', 'products' | 索引名称。示例: 'logs', 'products'",
                            "query", "string",
                            "Search query (Lucene syntax). Example: 'status:200' | 搜索查询（Lucene语法）。示例: 'status:200'",
                            "size", "integer", "Number of results (default: 10). Example: 20 | 结果数量（默认: 10）。示例: 20"))
                    .build();
        }

        /**
         * 执行搜索操作
         * <p>
         * 在指定的索引中执行搜索查询，返回匹配的文档数量。
         * </p>
         *
         * @param args 参数 Map，支持以下字段：
         *             <ul>
         *             <li>index (必需) - 索引名称</li>
         *             <li>query (可选) - 搜索查询，默认为 "*"</li>
         *             <li>size (可选) - 返回结果数量，默认为 10</li>
         *             </ul>
         * @return 工具执行结果，包含搜索结果信息
         */
        @Override
        public ToolResult execute(Map<String, Object> args) {
            String index = (String) args.get("index");
            String query = (String) args.getOrDefault("query", "*");
            int size = args.containsKey("size") ? ((Number) args.get("size")).intValue() : 10;

            try {
                SearchResponse<Void> response = client.search(s -> s
                        .index(index)
                        .query(q -> q.queryString(qs -> qs.query(query)))
                        .size(size),
                        Void.class);

                StringBuilder sb = new StringBuilder();
                sb.append("Found ").append(response.hits().total().value()).append(" hit(s)\n");
                return ToolResult.text(sb.toString());
            } catch (Exception e) {
                return ToolResult.error("Error: " + e.getMessage());
            }
        }
    }

    /**
     * 索引文档
     * <p>
     * 工具名称: {@code index_document}
     * </p>
     * <p>
     * 将文档索引到 Elasticsearch 中，支持指定文档 ID 或自动生成 ID。
     * </p>
     */
    public static class IndexDocumentTool implements ToolHandler {
        /**
         * Elasticsearch 客户端
         */
        private final ElasticsearchClient client;

        /**
         * 构造函数
         *
         * @param client Elasticsearch 客户端
         */
        public IndexDocumentTool(ElasticsearchClient client) {
            this.client = client;
        }

        /**
         * 获取工具定义
         *
         * @return 工具定义对象
         */
        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("index_document")
                    .description(
                            "Index a document into Elasticsearch. " +
                                    "将文档索引到Elasticsearch。 " +
                                    "Use this when you need to store logs, add documents, or update indexed data. " +
                                    "适用于存储日志、添加文档或更新索引数据等场景。 " +
                                    "Common use cases: log ingestion, document storage, data indexing. " +
                                    "典型用例：日志采集、文档存储、数据索引。")
                    .inputSchema(schema(
                            "index", "string", "Index name. Example: 'logs', 'products' | 索引名称。示例: 'logs', 'products'",
                            "id", "string", "Document ID (optional). Example: 'doc-001' | 文档ID（可选）。示例: 'doc-001'",
                            "document", "string",
                            "JSON document. Example: '{\"title\":\"test\"}' | JSON文档。示例: '{\"title\":\"test\"}'"))
                    .build();
        }

        /**
         * 执行索引操作
         * <p>
         * 将 JSON 格式的文档索引到指定的索引中。
         * </p>
         *
         * @param args 参数 Map，支持以下字段：
         *             <ul>
         *             <li>index (必需) - 索引名称</li>
         *             <li>id (可选) - 文档 ID，如果不指定则自动生成</li>
         *             <li>document (必需) - JSON 格式的文档内容</li>
         *             </ul>
         * @return 工具执行结果，包含索引的文档 ID 和结果
         */
        @Override
        public ToolResult execute(Map<String, Object> args) {
            String index = (String) args.get("index");
            String id = (String) args.get("id");
            String document = (String) args.get("document");

            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> doc = new com.fasterxml.jackson.databind.ObjectMapper().readValue(document,
                        Map.class);

                IndexResponse response;
                if (id != null && !id.isBlank()) {
                    response = client.index(i -> i.index(index).id(id).document(doc));
                } else {
                    response = client.index(i -> i.index(index).document(doc));
                }
                return ToolResult.text("Indexed document: " + response.id() + "\nResult: " + response.result());
            } catch (Exception e) {
                return ToolResult.error("Error: " + e.getMessage());
            }
        }
    }

    /**
     * 列出索引
     * <p>
     * 工具名称: {@code list_indices}
     * </p>
     * <p>
     * 返回 Elasticsearch 中的所有索引列表。
     * </p>
     */
    public static class ListIndicesTool implements ToolHandler {
        /**
         * Elasticsearch 客户端
         */
        private final ElasticsearchClient client;

        /**
         * 构造函数
         *
         * @param client Elasticsearch 客户端
         */
        public ListIndicesTool(ElasticsearchClient client) {
            this.client = client;
        }

        /**
         * 获取工具定义
         *
         * @return 工具定义对象
         */
        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("list_indices")
                    .description(
                            "List all Elasticsearch indices. " +
                                    "列出所有Elasticsearch索引。 " +
                                    "Use this when you need to explore available data, manage indices, or check storage. "
                                    +
                                    "适用于探索可用数据、管理索引或检查存储等场景。 " +
                                    "Common use cases: index discovery, storage management, data exploration. " +
                                    "典型用例：索引发现、存储管理、数据探索。")
                    .inputSchema(emptySchema())
                    .build();
        }

        /**
         * 执行列出索引操作
         * <p>
         * 获取 Elasticsearch 中所有的索引列表。
         * </p>
         *
         * @param args 参数 Map，此工具不需要参数
         * @return 工具执行结果，包含索引列表
         */
        @Override
        public ToolResult execute(Map<String, Object> args) {
            try {
                GetIndexResponse response = client.indices().get(a -> a.index("*"));

                StringBuilder sb = new StringBuilder();
                sb.append("Indices (").append(response.result().size()).append("):\n\n");
                for (String indexName : response.result().keySet()) {
                    sb.append("- ").append(indexName).append("\n");
                }
                return ToolResult.text(sb.toString());
            } catch (Exception e) {
                return ToolResult.error("Error: " + e.getMessage());
            }
        }
    }

    /**
     * 删除文档
     * <p>
     * 工具名称: {@code delete_document}
     * </p>
     * <p>
     * 从 Elasticsearch 中删除指定 ID 的文档。
     * </p>
     */
    public static class DeleteDocumentTool implements ToolHandler {
        /**
         * Elasticsearch 客户端
         */
        private final ElasticsearchClient client;

        /**
         * 构造函数
         *
         * @param client Elasticsearch 客户端
         */
        public DeleteDocumentTool(ElasticsearchClient client) {
            this.client = client;
        }

        /**
         * 获取工具定义
         *
         * @return 工具定义对象
         */
        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("delete_document")
                    .description(
                            "Delete a document from Elasticsearch. " +
                                    "从Elasticsearch删除文档。 " +
                                    "Use this when you need to remove outdated data, clean up documents, or manage storage. "
                                    +
                                    "适用于删除过期数据、清理文档或管理存储等场景。 " +
                                    "Common use cases: data cleanup, storage management, document removal. " +
                                    "典型用例：数据清理、存储管理、文档删除。")
                    .inputSchema(schema(
                            "index", "string", "Index name. Example: 'logs', 'products' | 索引名称。示例: 'logs', 'products'",
                            "id", "string", "Document ID. Example: 'doc-001' | 文档ID。示例: 'doc-001'"))
                    .build();
        }

        /**
         * 执行删除操作
         * <p>
         * 从指定的索引中删除指定 ID 的文档。
         * </p>
         *
         * @param args 参数 Map，支持以下字段：
         *             <ul>
         *             <li>index (必需) - 索引名称</li>
         *             <li>id (必需) - 文档 ID</li>
         *             </ul>
         * @return 工具执行结果，包含删除的文档 ID 和结果
         */
        @Override
        public ToolResult execute(Map<String, Object> args) {
            String index = (String) args.get("index");
            String id = (String) args.get("id");

            try {
                DeleteResponse response = client.delete(d -> d.index(index).id(id));
                return ToolResult.text("Deleted: " + id + "\nResult: " + response.result());
            } catch (Exception e) {
                return ToolResult.error("Error: " + e.getMessage());
            }
        }
    }

    /**
     * 构建空的输入参数 Schema
     *
     * @return 空的参数定义 Map
     */
    private static Map<String, Object> emptySchema() {
        Map<String, Object> s = new HashMap<>();
        s.put("type", "object");
        s.put("properties", new HashMap<>());
        return s;
    }

    /**
     * 构建输入参数 Schema（支持多个参数）
     *
     * @param props 可变参数，每3个为一组：名称、类型、描述
     * @return 参数定义 Map
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
