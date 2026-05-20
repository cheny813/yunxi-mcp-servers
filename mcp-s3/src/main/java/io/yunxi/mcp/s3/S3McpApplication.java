package io.yunxi.mcp.s3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MCP S3 应用入口
 * <p>
 * 基于 Spring Boot 的 MCP S3 服务器，提供 HTTP 和 SSE 两种调用模式，
 * 用于操作 AWS S3 存储桶和对象。
 * </p>
 *
 * <h3>使用说明</h3>
 * <pre>
 * # 启动服务（使用默认凭证）
 * java -jar mcp-s3-1.0.0.jar
 *
 * # 或指定 AWS 凭证和区域
 * java -jar mcp-s3-1.0.0.jar \
 *   --aws.access-key-id=AKIAIOSFODNN7EXAMPLE \
 *   --aws.secret-access-key=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY \
 *   --aws.region=us-east-1
 *
 * # 或指定 S3 兼容的 endpoint（用于 MinIO、阿里云 OSS 等）
 * java -jar mcp-s3-1.0.0.jar \
 *   --aws.endpoint=http://localhost:9000 \
 *   --aws.region=us-east-1
 * </pre>
 *
 * <h3>提供的工具</h3>
 * <ul>
 *   <li>s3_list_buckets - 列出所有存储桶</li>
 *   <li>s3_list_objects - 列出存储桶中的对象</li>
 *   <li>s3_put_object - 上传对象到存储桶</li>
 *   <li>s3_get_object - 下载存储桶中的对象</li>
 *   <li>s3_delete_object - 删除存储桶中的对象</li>
 * </ul>
 */
@SpringBootApplication
public class S3McpApplication {
    public static void main(String[] args) {
        SpringApplication.run(S3McpApplication.class, args);
    }
}
