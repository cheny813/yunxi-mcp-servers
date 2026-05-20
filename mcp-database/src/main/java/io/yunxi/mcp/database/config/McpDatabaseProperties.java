package io.yunxi.mcp.database.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * MCP 多数据库配置
 *
 * <p>
 * 从 YAML 读取多数据库连接信息，启动时自动注册到 {@link DatabaseConfigService}。
 * 不再依赖业务库中的 district_db 表。
 * </p>
 *
 * 配置示例：
 * <pre>
 * mcp-databases:
 *   nutrition:
 *     display-name: 营养数据库
 *     jdbc:
 *       url: jdbc:mysql://192.168.10.153:3306/nutrition_zhaoxian
 *       username: root
 *       password: root
 *   finance:
 *     display-name: 经费数据库
 *     jdbc:
 *       url: jdbc:mysql://192.168.10.154:3306/finance_db
 *       username: root
 *       password: root
 * </pre>
 *
 * @author yunxi-agent-platform
 * @version 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "mcp-databases")
public class McpDatabaseProperties {

    /**
     * 数据库配置映射
     * key: 数据库标识符（如 nutrition, finance）
     * value: 数据库配置
     */
    private Map<String, DatabaseInfo> databases = new HashMap<>();

    @Data
    public static class DatabaseInfo {
        /**
         * 显示名称
         */
        private String displayName;

        /**
         * JDBC 连接配置
         */
        private JdbcConfig jdbc;
    }

    @Data
    public static class JdbcConfig {
        /**
         * JDBC URL
         */
        private String url;

        /**
         * 用户名
         */
        private String username;

        /**
         * 密码
         */
        private String password;

        /**
         * 驱动类名
         */
        private String driverClassName = "com.mysql.cj.jdbc.Driver";
    }
}