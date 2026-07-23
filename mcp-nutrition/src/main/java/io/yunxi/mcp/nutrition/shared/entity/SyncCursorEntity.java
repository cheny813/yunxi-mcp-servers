package io.yunxi.mcp.nutrition.shared.entity;

import java.time.LocalDateTime;

/**
 * 同步增量游标实体
 * <p>
 * 用于记录每个增量同步管道上次同步到的游标位置（如最大更新时间），
 * 实现断点续传。对应营养库中的 sync_cursor 表。
 * </p>
 *
 * @author yunxi-agent-platform
 * @version 2.0.0
 */
public class SyncCursorEntity {

    /** 管道名称（即集合名） */
    private String pipelineName;

    /** 上次游标值（如最大更新时间字符串） */
    private String cursorValue;

    /** 上次同步行数 */
    private Long syncedRows;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    public String getPipelineName() { return pipelineName; }
    public void setPipelineName(String pipelineName) { this.pipelineName = pipelineName; }
    public String getCursorValue() { return cursorValue; }
    public void setCursorValue(String cursorValue) { this.cursorValue = cursorValue; }
    public Long getSyncedRows() { return syncedRows; }
    public void setSyncedRows(Long syncedRows) { this.syncedRows = syncedRows; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
