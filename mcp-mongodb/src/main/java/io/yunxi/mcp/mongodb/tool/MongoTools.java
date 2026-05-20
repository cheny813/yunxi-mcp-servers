package io.yunxi.mcp.mongodb.tool;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MongoDB 工具集合
 * <p>
 * 提供 MongoDB 数据库操作的 MCP 工具实现，包括数据库管理、集合操作和文档 CRUD。
 * 使用 MongoDB Java Driver 进行数据库操作。
 * </p>
 *
 * <h3>提供的工具</h3>
 * <ul>
 * <li>{@link ListDatabasesTool} - 列出所有数据库</li>
 * <li>{@link ListCollectionsTool} - 列出数据库中的集合</li>
 * <li>{@link FindTool} - 查询文档</li>
 * <li>{@link InsertTool} - 插入文档</li>
 * <li>{@link DeleteTool} - 删除文档</li>
 * </ul>
 */
public class MongoTools {

    /**
     * MongoDB 客户端
     * <p>
     * 用于与 MongoDB 数据库进行交互。
     * </p>
     */
    private final MongoClient mongoClient;

    /**
     * 构造函数
     *
     * @param mongoClient MongoDB 客户端实例
     */
    public MongoTools(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    /**
     * 列出所有数据库
     * <p>
     * 工具名称: {@code mongodb_list_databases}
     * </p>
     * <p>
     * 返回当前 MongoDB 实例下所有数据库的列表。
     * </p>
     */
    public static class ListDatabasesTool implements ToolHandler {
        /**
         * MongoDB 客户端
         */
        private final MongoClient mongoClient;

        /**
         * 构造函数
         *
         * @param mongoClient MongoDB 客户端实例
         */
        public ListDatabasesTool(MongoClient mongoClient) {
            this.mongoClient = mongoClient;
        }

        /**
         * 获取工具定义
         *
         * @return 工具定义对象
         */
        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("list_databases")
                    .description(
                            "List all MongoDB databases. " +
                                    "列出所有MongoDB数据库。 " +
                                    "Use this when you need to explore available databases, discover data, or manage storage. "
                                    +
                                    "适用于探索可用数据库、发现数据或管理存储等场景。 " +
                                    "Common use cases: database discovery, storage management, data exploration. " +
                                    "典型用例：数据库发现、存储管理、数据探索。")
                    .inputSchema(emptySchema())
                    .build();
        }

        /**
         * 执行列出数据库操作
         *
         * @param args 参数 Map（此工具无需参数）
         * @return 工具执行结果，包含数据库列表
         */
        @Override
        public ToolResult execute(Map<String, Object> args) {
            try {
                StringBuilder sb = new StringBuilder();
                int count = 0;
                // 遍历并输出所有数据库名称
                for (String dbName : mongoClient.listDatabaseNames()) {
                    sb.append("- ").append(dbName).append("\n");
                    count++;
                }
                return ToolResult.text("Databases (" + count + "):\n\n" + sb);
            } catch (Exception e) {
                return ToolResult.error("Error: " + e.getMessage());
            }
        }
    }

    /**
     * 列出集合
     * <p>
     * 工具名称: {@code mongodb_list_collections}
     * </p>
     * <p>
     * 返回指定数据库中的所有集合列表。
     * </p>
     */
    public static class ListCollectionsTool implements ToolHandler {
        /**
         * MongoDB 客户端
         */
        private final MongoClient mongoClient;

        /**
         * 构造函数
         *
         * @param mongoClient MongoDB 客户端实例
         */
        public ListCollectionsTool(MongoClient mongoClient) {
            this.mongoClient = mongoClient;
        }

        /**
         * 获取工具定义
         *
         * @return 工具定义对象
         */
        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("list_collections")
                    .description(
                            "List collections in a MongoDB database. " +
                                    "列出MongoDB数据库中的集合。 " +
                                    "Use this when you need to explore database structure, find collections, or plan queries. "
                                    +
                                    "适用于探索数据库结构、查找集合或规划查询等场景。 " +
                                    "Common use cases: schema exploration, collection discovery, query planning. " +
                                    "典型用例：模式探索、集合发现、查询规划。")
                    .inputSchema(schema("database", "string",
                            "Database name. Example: 'mydb', 'test' | 数据库名称。示例: 'mydb', 'test'"))
                    .build();
        }

        /**
         * 执行列出集合操作
         *
         * @param args 参数 Map：
         *             <ul>
         *             <li>database (必填) - 数据库名称</li>
         *             </ul>
         * @return 工具执行结果，包含集合列表
         */
        @Override
        public ToolResult execute(Map<String, Object> args) {
            String database = (String) args.get("database");
            try {
                // 获取指定数据库
                MongoDatabase db = mongoClient.getDatabase(database);
                StringBuilder sb = new StringBuilder();
                int count = 0;
                // 遍历并输出所有集合名称
                for (String collName : db.listCollectionNames()) {
                    sb.append("- ").append(collName).append("\n");
                    count++;
                }
                return ToolResult.text("Collections in " + database + " (" + count + "):\n\n" + sb);
            } catch (Exception e) {
                return ToolResult.error("Error: " + e.getMessage());
            }
        }
    }

    /**
     * 查询文档
     * <p>
     * 工具名称: {@code mongodb_find}
     * </p>
     * <p>
     * 从指定集合中查询文档，支持限制返回数量。
     * </p>
     */
    public static class FindTool implements ToolHandler {
        /**
         * MongoDB 客户端
         */
        private final MongoClient mongoClient;

        /**
         * 构造函数
         *
         * @param mongoClient MongoDB 客户端实例
         */
        public FindTool(MongoClient mongoClient) {
            this.mongoClient = mongoClient;
        }

        /**
         * 获取工具定义
         *
         * @return 工具定义对象
         */
        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("find")
                    .description(
                            "Find documents in a MongoDB collection. " +
                                    "在MongoDB集合中查询文档。 " +
                                    "Use this when you need to retrieve data, search documents, or analyze stored information. "
                                    +
                                    "适用于检索数据、搜索文档或分析存储信息等场景。 " +
                                    "Common use cases: data retrieval, document search, data analysis. " +
                                    "典型用例：数据检索、文档搜索、数据分析。")
                    .inputSchema(schema(
                            "database", "string", "Database name. Example: 'mydb' | 数据库名称。示例: 'mydb'",
                            "collection", "string", "Collection name. Example: 'users' | 集合名称。示例: 'users'",
                            "filter", "string",
                            "JSON filter (optional). Example: '{\"status\":\"active\"}' | JSON过滤器（可选）。示例: '{\"status\":\"active\"}'",
                            "limit", "integer", "Maximum documents to return. Example: 10 | 最大返回文档数。示例: 10"))
                    .build();
        }

        /**
         * 执行查询文档操作
         *
         * @param args 参数 Map：
         *             <ul>
         *             <li>database (必填) - 数据库名称</li>
         *             <li>collection (必填) - 集合名称</li>
         *             <li>filter (可选) - 查询过滤器（JSON 格式）</li>
         *             <li>limit (可选) - 返回文档数量限制，默认 10</li>
         *             </ul>
         * @return 工具执行结果，包含查询到的文档列表
         */
        @Override
        public ToolResult execute(Map<String, Object> args) {
            String database = (String) args.get("database");
            String collection = (String) args.get("collection");
            // 获取返回数量限制
            int limit = args.containsKey("limit") ? ((Number) args.get("limit")).intValue() : 10;

            try {
                // 获取数据库和集合
                MongoDatabase db = mongoClient.getDatabase(database);
                MongoCollection<Document> coll = db.getCollection(collection);

                StringBuilder sb = new StringBuilder();
                int count = 0;
                // 执行查询并遍历结果
                for (Document doc : coll.find().limit(limit)) {
                    sb.append(doc.toJson()).append("\n\n");
                    count++;
                }
                return ToolResult.text("Found " + count + " document(s):\n\n" + sb);
            } catch (Exception e) {
                return ToolResult.error("Error: " + e.getMessage());
            }
        }
    }

    /**
     * 插入文档
     * <p>
     * 工具名称: {@code mongodb_insert}
     * </p>
     * <p>
     * 向指定集合中插入一个 JSON 文档。
     * </p>
     */
    public static class InsertTool implements ToolHandler {
        /**
         * MongoDB 客户端
         */
        private final MongoClient mongoClient;

        /**
         * 构造函数
         *
         * @param mongoClient MongoDB 客户端实例
         */
        public InsertTool(MongoClient mongoClient) {
            this.mongoClient = mongoClient;
        }

        /**
         * 获取工具定义
         *
         * @return 工具定义对象
         */
        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("insert")
                    .description(
                            "Insert a document into a MongoDB collection. " +
                                    "向MongoDB集合插入文档。 " +
                                    "Use this when you need to store data, add records, or save new documents. " +
                                    "适用于存储数据、添加记录或保存新文档等场景。 " +
                                    "Common use cases: data storage, record creation, document insertion. " +
                                    "典型用例：数据存储、记录创建、文档插入。")
                    .inputSchema(schema(
                            "database", "string", "Database name. Example: 'mydb' | 数据库名称。示例: 'mydb'",
                            "collection", "string", "Collection name. Example: 'users' | 集合名称。示例: 'users'",
                            "document", "string",
                            "JSON document to insert. Example: '{\"name\":\"John\"}' | 要插入的JSON文档。示例: '{\"name\":\"John\"}'"))
                    .build();
        }

        /**
         * 执行插入文档操作
         *
         * @param args 参数 Map：
         *             <ul>
         *             <li>database (必填) - 数据库名称</li>
         *             <li>collection (必填) - 集合名称</li>
         *             <li>document (必填) - 要插入的 JSON 文档</li>
         *             </ul>
         * @return 工具执行结果，包含插入文档的 ID
         */
        @Override
        public ToolResult execute(Map<String, Object> args) {
            String database = (String) args.get("database");
            String collection = (String) args.get("collection");
            String document = (String) args.get("document");

            try {
                // 获取数据库和集合
                MongoDatabase db = mongoClient.getDatabase(database);
                MongoCollection<Document> coll = db.getCollection(collection);
                // 解析并插入文档
                Document doc = Document.parse(document);
                coll.insertOne(doc);
                return ToolResult.text("Inserted document with ID: " + doc.getObjectId("_id"));
            } catch (Exception e) {
                return ToolResult.error("Error: " + e.getMessage());
            }
        }
    }

    /**
     * 删除文档
     * <p>
     * 工具名称: {@code mongodb_delete}
     * </p>
     * <p>
     * 从指定集合中删除符合过滤条件的文档。
     * </p>
     */
    public static class DeleteTool implements ToolHandler {
        /**
         * MongoDB 客户端
         */
        private final MongoClient mongoClient;

        /**
         * 构造函数
         *
         * @param mongoClient MongoDB 客户端实例
         */
        public DeleteTool(MongoClient mongoClient) {
            this.mongoClient = mongoClient;
        }

        /**
         * 获取工具定义
         *
         * @return 工具定义对象
         */
        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("delete")
                    .description(
                            "Delete documents from a MongoDB collection. " +
                                    "从MongoDB集合删除文档。 " +
                                    "Use this when you need to remove records, clean up data, or manage storage. " +
                                    "适用于删除记录、清理数据或管理存储等场景。 " +
                                    "Common use cases: data cleanup, record removal, storage management. " +
                                    "典型用例：数据清理、记录删除、存储管理。")
                    .inputSchema(schema(
                            "database", "string", "Database name. Example: 'mydb' | 数据库名称。示例: 'mydb'",
                            "collection", "string", "Collection name. Example: 'users' | 集合名称。示例: 'users'",
                            "filter", "string",
                            "JSON filter. Example: '{\"status\":\"inactive\"}' | JSON过滤器。示例: '{\"status\":\"inactive\"}'"))
                    .build();
        }

        /**
         * 执行删除文档操作
         *
         * @param args 参数 Map：
         *             <ul>
         *             <li>database (必填) - 数据库名称</li>
         *             <li>collection (必填) - 集合名称</li>
         *             <li>filter (可选) - 删除过滤器（JSON 格式），默认 {} 表示删除所有</li>
         *             </ul>
         * @return 工具执行结果，包含删除的文档数量
         */
        @Override
        public ToolResult execute(Map<String, Object> args) {
            String database = (String) args.get("database");
            String collection = (String) args.get("collection");
            String filter = (String) args.getOrDefault("filter", "{}");

            try {
                // 获取数据库和集合
                MongoDatabase db = mongoClient.getDatabase(database);
                MongoCollection<Document> coll = db.getCollection(collection);
                // 执行删除操作
                var result = coll.deleteMany(Document.parse(filter));
                return ToolResult.text("Deleted " + result.getDeletedCount() + " document(s)");
            } catch (Exception e) {
                return ToolResult.error("Error: " + e.getMessage());
            }
        }
    }

    /**
     * 构建空的输入参数 Schema
     *
     * @return 空的 schema 对象
     */
    private static Map<String, Object> emptySchema() {
        Map<String, Object> s = new HashMap<>();
        s.put("type", "object");
        s.put("properties", new HashMap<>());
        return s;
    }

    /**
     * 构建输入参数 Schema
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
