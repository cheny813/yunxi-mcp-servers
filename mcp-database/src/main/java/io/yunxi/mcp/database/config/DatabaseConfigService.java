package io.yunxi.mcp.database.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多数据库配置服务
 * <p>
 * 管理多个数据库连接配置，支持动态添加、删除和切换数据库。
 * </p>
 *
 * @author yunxi
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
public class DatabaseConfigService {

    private final Map<String, DatabaseConfig> configs = new ConcurrentHashMap<>();
    private final Map<String, DataSource> dataSources = new ConcurrentHashMap<>();

    /**
     * 添加数据库配置
     */
    public DatabaseConfig addDatabase(DatabaseConfig config) {
        String id = config.getId();
        if (id == null || id.isEmpty()) {
            id = "db_" + System.currentTimeMillis();
            config.setId(id);
        }

        configs.put(id, config);

        // 创建数据源
        DataSource ds = createDataSource(config);
        dataSources.put(id, ds);

        log.info("添加数据库: {} -> {}", id, config.getName());
        return config;
    }

    /**
     * 获取所有数据库配置
     */
    public Map<String, DatabaseConfig> getAllDatabases() {
        return configs;
    }

    /**
     * 获取数据源
     */
    public DataSource getDataSource(String dbId) {
        return dataSources.get(dbId);
    }

    /**
     * 获取数据库配置
     */
    public DatabaseConfig getConfig(String dbId) {
        return configs.get(dbId);
    }

    /**
     * 删除数据库
     */
    public boolean removeDatabase(String dbId) {
        DataSource ds = dataSources.remove(dbId);
        configs.remove(dbId);

        if (ds instanceof HikariDataSource) {
            ((HikariDataSource) ds).close();
        }

        log.info("删除数据库: {}", dbId);
        return ds != null;
    }

    /**
     * 测试数据库连接
     */
    public boolean testConnection(DatabaseConfig config) {
        try {
            DataSource ds = createDataSource(config);
            ds.getConnection().close();
            ((HikariDataSource) ds).close();
            return true;
        } catch (Exception e) {
            log.error("数据库连接测试失败: {}", e.getMessage());
            return false;
        }
    }

    private DataSource createDataSource(DatabaseConfig config) {
        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl(config.getJdbcUrl());
        hc.setUsername(config.getUsername());
        hc.setPassword(config.getPassword());
        hc.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hc.setMaximumPoolSize(5);
        hc.setMinimumIdle(1);
        hc.setConnectionTimeout(30000);
        hc.setIdleTimeout(600000);
        hc.setMaxLifetime(1800000);
        return new HikariDataSource(hc);
    }
}