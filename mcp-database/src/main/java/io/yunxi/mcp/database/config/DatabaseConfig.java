package io.yunxi.mcp.database.config;

import lombok.Data;

import java.util.List;

/**
 * 数据库配置实体
 * <p>
 * 存储数据库连接信息，包括主机、端口、数据库名、用户名和密码等配置。
 * </p>
 *
 * @author yunxi
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class DatabaseConfig {

    /**
     * 数据库唯一标识符
     */
    private String id;

    /**
     * 数据库名称（用于显示）
     */
    private String name;

    /**
     * 数据库主机地址
     */
    private String host;

    /**
     * 数据库端口号，默认 3306
     */
    private Integer port = 3306;

    /**
     * 数据库名称
     */
    private String database;

    /**
     * 数据库用户名
     */
    private String username;

    /**
     * 数据库密码
     */
    private String password;

    /**
     * 数据库描述信息
     */
    private String description;

    /**
     * 是否启用该数据库配置
     */
    private boolean enabled = true;

    /**
     * 允许访问的表名列表（为空表示允许所有）
     * <p>
     * 用于细粒度的数据访问控制，限制工具只能访问指定的表。
     * 例如：["users", "orders"] 表示只允许访问这两个表
     * </p>
     */
    private List<String> allowedTables;

    /**
     * 禁止访问的表名列表
     * <p>
     * 用于数据安全保护，明确禁止访问敏感表。
     * 例如：["passwords", "secrets"] 表示禁止访问这些表
     * </p>
     */
    private List<String> deniedTables;

    /**
     * 是否允许访问系统表
     * <p>
     * 系统表通常包含数据库元数据，默认为 false 以提高安全性。
     * </p>
     */
    private boolean allowSystemTables = false;

    /**
     * 最大返回行数限制
     * <p>
     * 防止大数据量查询导致内存溢出，默认为 1000。
     * </p>
     */
    private int maxRowsLimit = 1000;

    /**
     * 查询超时时间（秒）
     * <p>
     * 防止慢查询占用资源，默认为 30 秒。
     * </p>
     */
    private int queryTimeoutSeconds = 30;

    /**
     * 获取 JDBC URL
     * <p>
     * 根据配置信息生成标准的 MySQL JDBC 连接 URL。
     * 格式：jdbc:mysql://host:port/database?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
     * </p>
     *
     * @return JDBC 连接 URL 字符串
     */
    public String getJdbcUrl() {
        return String.format(
                "jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true",
                host, port, database);
    }
}