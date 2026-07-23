package io.yunxi.mcp.nutrition.shared.repository;

import io.yunxi.mcp.nutrition.shared.entity.SyncCursorEntity;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 同步增量游标仓储（JdbcTemplate 实现）
 * <p>
 * 替代原 agent-core 的 MyBatis SyncCursorMapper，直接基于营养库中的 sync_cursor 表，
 * 提供断点续传所需的游标读写。表由 {@link #initTable()} 在容器启动时自动创建，
 * 无需运维手工 DDL。
 * </p>
 *
 * @author yunxi-agent-platform
 * @version 2.0.0
 */
@Repository
public class SyncCursorRepository {

    private static final Logger log = LoggerFactory.getLogger(SyncCursorRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public SyncCursorRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 容器启动时自动建表（幂等），消解对业务库预置表的依赖。
     */
    @PostConstruct
    public void initTable() {
        try {
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS sync_cursor ("
                    + "pipeline_name VARCHAR(128) NOT NULL, "
                    + "cursor_value VARCHAR(255), "
                    + "synced_rows BIGINT DEFAULT 0, "
                    + "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
                    + "PRIMARY KEY (pipeline_name)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
            log.info("sync_cursor 表已就绪（自动建表）");
        } catch (Exception e) {
            log.warn("sync_cursor 表初始化失败（可能已存在，无害）: {}", e.getMessage());
        }
    }

    private static final RowMapper<SyncCursorEntity> ROW_MAPPER = (rs, i) -> {
        SyncCursorEntity e = new SyncCursorEntity();
        e.setPipelineName(rs.getString("pipeline_name"));
        e.setCursorValue(rs.getString("cursor_value"));
        long sr = rs.getLong("synced_rows");
        e.setSyncedRows(rs.wasNull() ? null : sr);
        Timestamp ts = rs.getTimestamp("updated_at");
        e.setUpdatedAt(ts != null ? ts.toLocalDateTime() : null);
        return e;
    };

    /**
     * 按管道名称查询游标。
     */
    public SyncCursorEntity findByPipelineName(String pipelineName) {
        List<SyncCursorEntity> list = jdbcTemplate.query(
                "SELECT pipeline_name, cursor_value, synced_rows, updated_at FROM sync_cursor WHERE pipeline_name = ?",
                ROW_MAPPER, pipelineName);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 按 id 查询（元数据兼容，未使用主键列时返回 null）。
     */
    public SyncCursorEntity findById(Long id) {
        return null;
    }

    /**
     * 插入或更新游标（按 pipeline_name 唯一键）。
     */
    public void upsert(SyncCursorEntity entity) {
        LocalDateTime now = entity.getUpdatedAt() != null ? entity.getUpdatedAt() : LocalDateTime.now();
        jdbcTemplate.update(
                "INSERT INTO sync_cursor (pipeline_name, cursor_value, synced_rows, updated_at) VALUES (?, ?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE cursor_value = VALUES(cursor_value), "
                        + "synced_rows = VALUES(synced_rows), updated_at = VALUES(updated_at)",
                entity.getPipelineName(), entity.getCursorValue(), entity.getSyncedRows(),
                Timestamp.valueOf(now));
    }

    /**
     * 按 id 删除（元数据兼容，未使用主键列时为空操作）。
     */
    public void deleteById(Long id) {
        // 增量同步仅依赖 findByPipelineName / upsert，此处保持兼容空实现
    }
}
