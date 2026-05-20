package io.yunxi.mcp.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.yunxi.mcp.common.server.AbstractMcpServer;
import io.yunxi.mcp.database.tool.QueryTool;
import io.yunxi.mcp.database.tool.ListTablesTool;
import io.yunxi.mcp.database.tool.DescribeTableTool;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 数据库 MCP 服务器
 * <p>
 * 提供 MySQL/PostgreSQL 数据库操作能力
 * </p>
 *
 * <h3>使用方式</h3>
 * <pre>
 * java -jar mcp-database.jar --db.url=${DATABASE_URL:jdbc:mysql://localhost:3306/test} --db.username=${DATABASE_USERNAME:root} --db.password=${DATABASE_PASSWORD:root}
 * </pre>
 */
public class DatabaseMcpServer extends AbstractMcpServer {

    private final DataSource dataSource;

    public DatabaseMcpServer(String name, String version, DataSource dataSource) {
        super(name, version);
        this.dataSource = dataSource;

        // 注册工具
        registerTool(new QueryTool(this));
        registerTool(new ListTablesTool(this));
        registerTool(new DescribeTableTool(this));
    }

    /**
     * 获取数据库连接
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * 获取数据源
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * 主入口
     */
    public static void main(String[] args) {
        // 解析数据库配置
        String dbUrl = getConfig("db.url", ""); // 建议通过环境变量DATABASE_URL配置
        String dbUsername = getConfig("db.username", "root");
        String dbPassword = getConfig("db.password", "root");
        String dbDriver = getConfig("db.driver", "com.mysql.cj.jdbc.Driver");

        // 创建数据源
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUsername);
        config.setPassword(dbPassword);
        config.setDriverClassName(dbDriver);
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        HikariDataSource dataSource = new HikariDataSource(config);

        // 启动 MCP 服务器
        DatabaseMcpServer server = new DatabaseMcpServer(
                "yunxi-mcp-database",
                "1.0.0",
                dataSource
        );

        server.start();
    }

    /**
     * 获取配置（优先环境变量，其次系统属性）
     */
    private static String getConfig(String key, String defaultValue) {
        String envKey = key.replace(".", "_").toUpperCase();
        String value = System.getenv(envKey);
        if (value != null) {
            return value;
        }
        value = System.getProperty(key);
        if (value != null) {
            return value;
        }
        return defaultValue;
    }
}
