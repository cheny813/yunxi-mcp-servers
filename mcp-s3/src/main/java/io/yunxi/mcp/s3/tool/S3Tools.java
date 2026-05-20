package io.yunxi.mcp.s3.tool;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.core.ResponseInputStream;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * S3 工具集合
 * <p>
 * 提供 AWS S3 存储操作的 MCP 工具实现，包括存储桶管理和对象操作。
 * 使用 AWS SDK for Java v2 进行 S3 操作。
 * </p>
 *
 * <h3>提供的工具</h3>
 * <ul>
 *   <li>{@link ListBucketsTool} - 列出所有存储桶</li>
 *   <li>{@link ListObjectsTool} - 列出存储桶中的对象</li>
 *   <li>{@link PutObjectTool} - 上传对象到存储桶</li>
 *   <li>{@link GetObjectTool} - 下载存储桶中的对象</li>
 *   <li>{@link DeleteObjectTool} - 删除存储桶中的对象</li>
 * </ul>
 */
public class S3Tools {

    /**
     * S3 客户端
     * <p>
     * 用于与 AWS S3 或 S3 兼容存储服务进行交互。
     * </p>
     */
    private final S3Client s3Client;

    /**
     * 构造函数
     *
     * @param s3Client S3 客户端实例
     */
    public S3Tools(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    /**
     * 列出所有存储桶
     * <p>
     * 工具名称: {@code s3_list_buckets}
     * </p>
     * <p>
     * 返回当前 AWS 账户下所有 S3 存储桶的列表，包括存储桶名称和创建时间。
     * </p>
     */
    public static class ListBucketsTool implements ToolHandler {
        /**
         * S3 客户端
         */
        private final S3Client s3Client;

        /**
         * 构造函数
         *
         * @param s3Client S3 客户端实例
         */
        public ListBucketsTool(S3Client s3Client) {
            this.s3Client = s3Client;
        }

        /**
         * 获取工具定义
         *
         * @return 工具定义对象
         */
        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("list_buckets")
                    .description("List all S3 buckets")
                    .inputSchema(emptySchema())
                    .build();
        }

        /**
         * 执行列出存储桶操作
         *
         * @param args 参数 Map（此工具无需参数）
         * @return 工具执行结果，包含存储桶列表
         */
        @Override
        public ToolResult execute(Map<String, Object> args) {
            try {
                // 调用 S3 API 列出所有存储桶
                ListBucketsResponse response = s3Client.listBuckets();
                StringBuilder sb = new StringBuilder();
                sb.append("Buckets (").append(response.buckets().size()).append("):\n\n");
                // 遍历并格式化输出每个存储桶的信息
                for (Bucket bucket : response.buckets()) {
                    sb.append("- ").append(bucket.name());
                    if (bucket.creationDate() != null) {
                        sb.append(" (created: ").append(bucket.creationDate()).append(")");
                    }
                    sb.append("\n");
                }
                return ToolResult.text(sb.toString());
            } catch (Exception e) {
                return ToolResult.error("Error: " + e.getMessage());
            }
        }
    }

    /**
     * 列出存储桶中的对象
     * <p>
     * 工具名称: {@code s3_list_objects}
     * </p>
     * <p>
     * 返回指定存储桶中的对象列表，支持通过 prefix 进行过滤。
     * </p>
     */
    public static class ListObjectsTool implements ToolHandler {
        /**
         * S3 客户端
         */
        private final S3Client s3Client;

        /**
         * 构造函数
         *
         * @param s3Client S3 客户端实例
         */
        public ListObjectsTool(S3Client s3Client) {
            this.s3Client = s3Client;
        }

        /**
         * 获取工具定义
         *
         * @return 工具定义对象
         */
        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("list_objects")
                    .description("List objects in an S3 bucket")
                    .inputSchema(schema("bucket", "string", "Bucket name",
                            "prefix", "string", "Prefix to filter objects"))
                    .build();
        }

        /**
         * 执行列出对象操作
         *
         * @param args 参数 Map：
         *             <ul>
         *               <li>bucket (必填) - 存储桶名称</li>
         *               <li>prefix (可选) - 对象键前缀过滤</li>
         *             </ul>
         * @return 工具执行结果，包含对象列表
         */
        @Override
        public ToolResult execute(Map<String, Object> args) {
            // 获取必填参数：存储桶名称
            String bucket = (String) args.get("bucket");
            // 获取可选参数：前缀过滤
            String prefix = (String) args.getOrDefault("prefix", "");

            try {
                // 构建列出对象请求
                ListObjectsV2Request request = ListObjectsV2Request.builder()
                        .bucket(bucket)
                        .prefix(prefix)
                        .build();

                // 调用 S3 API 列出对象
                ListObjectsV2Response response = s3Client.listObjectsV2(request);
                StringBuilder sb = new StringBuilder();
                sb.append("Objects in ").append(bucket).append("/").append(prefix)
                  .append(" (").append(response.contents().size()).append("):\n\n");

                // 遍历并格式化输出每个对象的信息
                for (S3Object obj : response.contents()) {
                    sb.append("- ").append(obj.key())
                      .append(" (").append(obj.size()).append(" bytes)\n");
                }
                return ToolResult.text(sb.toString());
            } catch (Exception e) {
                return ToolResult.error("Error: " + e.getMessage());
            }
        }
    }

    /**
     * 上传对象到存储桶
     * <p>
     * 工具名称: {@code s3_put_object}
     * </p>
     * <p>
     * 将字符串内容上传到指定的 S3 存储桶。
     * </p>
     */
    public static class PutObjectTool implements ToolHandler {
        /**
         * S3 客户端
         */
        private final S3Client s3Client;

        /**
         * 构造函数
         *
         * @param s3Client S3 客户端实例
         */
        public PutObjectTool(S3Client s3Client) {
            this.s3Client = s3Client;
        }

        /**
         * 获取工具定义
         *
         * @return 工具定义对象
         */
        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("put_object")
                    .description("Upload an object to S3")
                    .inputSchema(schema("bucket", "string", "Bucket name",
                            "key", "string", "Object key",
                            "content", "string", "Content to upload"))
                    .build();
        }

        /**
         * 执行上传对象操作
         *
         * @param args 参数 Map：
         *             <ul>
         *               <li>bucket (必填) - 存储桶名称</li>
         *               <li>key (必填) - 对象键</li>
         *               <li>content (必填) - 上传的内容</li>
         *             </ul>
         * @return 工具执行结果，包含上传状态和 ETag
         */
        @Override
        public ToolResult execute(Map<String, Object> args) {
            String bucket = (String) args.get("bucket");
            String key = (String) args.get("key");
            String content = (String) args.get("content");

            try {
                // 上传对象到 S3
                PutObjectResponse response = s3Client.putObject(
                        PutObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .build(),
                        RequestBody.fromString(content)
                );
                return ToolResult.text("Uploaded: s3://" + bucket + "/" + key + "\nETag: " + response.eTag());
            } catch (Exception e) {
                return ToolResult.error("Error: " + e.getMessage());
            }
        }
    }

    /**
     * 下载存储桶中的对象
     * <p>
     * 工具名称: {@code s3_get_object}
     * </p>
     * <p>
     * 从 S3 存储桶下载对象内容。
     * </p>
     */
    public static class GetObjectTool implements ToolHandler {
        /**
         * S3 客户端
         */
        private final S3Client s3Client;

        /**
         * 构造函数
         *
         * @param s3Client S3 客户端实例
         */
        public GetObjectTool(S3Client s3Client) {
            this.s3Client = s3Client;
        }

        /**
         * 获取工具定义
         *
         * @return 工具定义对象
         */
        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("get_object")
                    .description("Get object content from S3")
                    .inputSchema(schema("bucket", "string", "Bucket name",
                            "key", "string", "Object key"))
                    .build();
        }

        /**
         * 执行下载对象操作
         *
         * @param args 参数 Map：
         *             <ul>
         *               <li>bucket (必填) - 存储桶名称</li>
         *               <li>key (必填) - 对象键</li>
         *             </ul>
         * @return 工具执行结果，包含对象内容
         */
        @Override
        public ToolResult execute(Map<String, Object> args) {
            String bucket = (String) args.get("bucket");
            String key = (String) args.get("key");

            try (ResponseInputStream<GetObjectResponse> response = s3Client.getObject(
                    GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .build())) {
                // 读取并返回对象内容
                String content = new BufferedReader(new InputStreamReader(response))
                        .lines().collect(Collectors.joining("\n"));
                return ToolResult.text("s3://" + bucket + "/" + key + "\n\n" + content);
            } catch (Exception e) {
                return ToolResult.error("Error: " + e.getMessage());
            }
        }
    }

    /**
     * 删除存储桶中的对象
     * <p>
     * 工具名称: {@code s3_delete_object}
     * </p>
     * <p>
     * 从 S3 存储桶删除指定的对象。
     * </p>
     */
    public static class DeleteObjectTool implements ToolHandler {
        /**
         * S3 客户端
         */
        private final S3Client s3Client;

        /**
         * 构造函数
         *
         * @param s3Client S3 客户端实例
         */
        public DeleteObjectTool(S3Client s3Client) {
            this.s3Client = s3Client;
        }

        /**
         * 获取工具定义
         *
         * @return 工具定义对象
         */
        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("delete_object")
                    .description("Delete an object from S3")
                    .inputSchema(schema("bucket", "string", "Bucket name",
                            "key", "string", "Object key"))
                    .build();
        }

        /**
         * 执行删除对象操作
         *
         * @param args 参数 Map：
         *             <ul>
         *               <li>bucket (必填) - 存储桶名称</li>
         *               <li>key (必填) - 对象键</li>
         *             </ul>
         * @return 工具执行结果，包含删除状态
         */
        @Override
        public ToolResult execute(Map<String, Object> args) {
            String bucket = (String) args.get("bucket");
            String key = (String) args.get("key");

            try {
                // 删除 S3 对象
                s3Client.deleteObject(
                        DeleteObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .build()
                );
                return ToolResult.text("Deleted: s3://" + bucket + "/" + key);
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
