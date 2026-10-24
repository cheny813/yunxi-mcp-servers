package io.yunxi.mcp.text2sql.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据源注册表：根据配置 {@code text2sql.datasources} 懒加载 Hikari 连接池。
 */
@Service
public class DatasourceRegistry {

    private final Text2SqlProperties properties;
    private final Map<String, HikariDataSource> cache = new ConcurrentHashMap<>();

    public DatasourceRegistry(Text2SqlProperties properties) {
        this.properties = properties;
    }

    public DataSource getDataSource(String dbId) {
        HikariDataSource existing = cache.get(dbId);
        if (existing != null) {
            return existing;
        }
        Text2SqlProperties.DatasourceConfig cfg = properties.getDatasources().get(dbId);
        if (cfg == null) {
            throw new RuntimeException("未配置的数据源: " + dbId
                    + "。已配置: " + properties.getDatasources().keySet());
        }
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(cfg.getUrl());
        ds.setUsername(cfg.getUsername());
        ds.setPassword(cfg.getPassword());
        ds.setMaximumPoolSize(3);
        ds.setPoolName("text2sql-" + dbId);
        HikariDataSource racing = cache.putIfAbsent(dbId, ds);
        return racing != null ? racing : ds;
    }

    public boolean contains(String dbId) {
        return properties.getDatasources().containsKey(dbId);
    }
}
