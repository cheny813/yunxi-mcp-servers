package io.yunxi.mcp.nutrition.sync.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 同步数据源注册表 —— 管理 MySQL→Milvus 同步管道对应的源端数据库连接。
 *
 * <h3>数据源解析优先级</h3>
 * <ol>
 *   <li>{@code dsId} 为 {@code "default"} 或 {@code null}：
 *       复用 Spring Boot 的 {@code spring.datasource}（HikariCP 连接池）。</li>
 *   <li>{@code dsId} 命中注册表 → 按需建池并缓存，返回对应的 {@link JdbcTemplate} / {@link DataSource}。</li>
 * </ol>
 *
 * <h3>推荐 API</h3>
 * <ul>
 *   <li>{@link #getJdbcTemplate(String)} — 获取数据源的 JdbcTemplate（default 或外部）</li>
 *   <li>{@link #getDataSource(String)} — 获取数据源（default 或外部）</li>
 *   <li>{@link #isResolvable(String)} — 检查数据源是否可用</li>
 *   <li>{@link #getJdbcTemplate()} — 速记方法，返回 default 数据源的 JdbcTemplate</li>
 *   <li>{@link #getDataSource()} — 速记方法，返回 default 数据源</li>
 * </ul>
 */
@Component
public class SyncDatasourceRegistry {

    private static final Logger log = LoggerFactory.getLogger(SyncDatasourceRegistry.class);

    public static final String DEFAULT_ID = "default";

    /** 外部同步数据源元数据 */
    public record SyncDataSource(String url, String username, String password, String driverClassName) {}

    private final DataSource springDataSource;
    private final JdbcTemplate springJdbcTemplate;
    private final Map<String, SyncDataSource> dataSources = new ConcurrentHashMap<>();
    private final Map<String, JdbcTemplate> externalJdbcTemplates = new ConcurrentHashMap<>();

    public SyncDatasourceRegistry(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.springDataSource = dataSource;
        this.springJdbcTemplate = jdbcTemplate;
    }

    // ─── 核心 API（推荐）────────────────────

    /** 获取数据源的 JdbcTemplate。dsId 为 null 或 "default" 时返回 Spring 默认。 */
    public JdbcTemplate getJdbcTemplate(String dsId) {
        if (dsId == null || DEFAULT_ID.equals(dsId)) {
            return springJdbcTemplate;
        }
        return externalJdbcTemplates.computeIfAbsent(dsId, id -> {
            SyncDataSource spec = dataSources.get(id);
            if (spec == null) {
                throw new IllegalArgumentException("未注册的同步数据源: " + id + "，请在 sync.sources 中配置");
            }
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(spec.url());
            config.setUsername(spec.username());
            config.setPassword(spec.password());
            config.setDriverClassName(spec.driverClassName() != null ? spec.driverClassName() : "com.mysql.cj.jdbc.Driver");
            config.setMaximumPoolSize(2);
            config.setMinimumIdle(0);
            config.setConnectionTimeout(10000);
            config.setInitializationFailTimeout(0); // 数据库不可达时不抛异常，由调用方探测后降级
            config.setPoolName("sync-ds-" + id);
            HikariDataSource ds = new HikariDataSource(config);
            return new JdbcTemplate(ds);
        });
    }

    /** 获取数据源。dsId 为 null 或 "default" 时返回 Spring 默认。 */
    public DataSource getDataSource(String dsId) {
        if (dsId == null || DEFAULT_ID.equals(dsId)) {
            return springDataSource;
        }
        return getJdbcTemplate(dsId).getDataSource();
    }

    /** 检查数据源是否可解析。 */
    public boolean isResolvable(String dsId) {
        if (dsId == null || DEFAULT_ID.equals(dsId)) {
            return true;
        }
        return dataSources.containsKey(dsId);
    }

    /**
     * 探测数据源是否能建立连接（用于启动期/调用期优雅降级判断）。
     * 连接失败仅记录 WARN 并返回 false，不向上抛出，避免阻断启动或调用。
     *
     * @param dsId 数据源标识（null / "default" 表示 Spring 默认）
     * @return 是否可连通
     */
    public boolean testConnection(String dsId) {
        try {
            DataSource ds = getDataSource(dsId);
            try (Connection c = ds.getConnection()) {
                return c.isValid(2);
            }
        } catch (Exception e) {
            log.warn("数据源 {} 连接探测失败，将降级处理: {}", dsId, e.getMessage());
            return false;
        }
    }

    // ─── 速记方法（Spring default）────────

    public JdbcTemplate getJdbcTemplate() {
        return springJdbcTemplate;
    }

    public DataSource getDataSource() {
        return springDataSource;
    }

    // ─── 注册管理 ─────────────────────────

    /** 注册外部数据源元数据。id 不可为 "default"。 */
    public void register(String id, SyncDataSource spec) {
        if (DEFAULT_ID.equals(id)) {
            throw new IllegalArgumentException("'default' 为 Spring 内置数据源，不可覆盖注册");
        }
        dataSources.put(id, spec);
        log.info("注册同步数据源: {} → {}", id, spec.url());
    }

    /** 获取已注册的外部数据源元数据（不含 default）。 */
    public Map<String, SyncDataSource> getRegisteredSources() {
        return Collections.unmodifiableMap(dataSources);
    }

    // ─── 废弃 API ──────────────────────────

    /**
     * @deprecated 返回元数据对象 {@link SyncDataSource}，仅支持外部数据源。
     *     请迁移到 {@link #getJdbcTemplate(String)} / {@link #getDataSource(String)}。
     */
    @Deprecated(since = "1.0", forRemoval = true)
    public SyncDataSource resolve(String id) {
        if (id == null || DEFAULT_ID.equals(id)) {
            log.warn("resolve() 不支持 default 数据源，请改用 getJdbcTemplate()/getDataSource()");
            return null;
        }
        SyncDataSource spec = dataSources.get(id);
        if (spec == null) {
            log.warn("未找到数据源: {}", id);
        }
        return spec;
    }
}
