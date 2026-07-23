package io.yunxi.mcp.nutrition.sync.dto;

import java.util.List;
import java.util.Map;

/**
 * 同步状态 DTO
 * <p>
 * 用于封装同步操作的返回结果，包含同步的数据行、总数、已同步数、状态等信息。
 * </p>
 *
 * @author yunxi-agent-platform
 * @version 2.0.0
 */
public class SyncStateDTO {

    /** 同步返回的数据行 */
    private List<Map<String, Object>> rows;

    /** 总记录数 */
    private long total;

    /** 已同步记录数 */
    private long synced;

    /** 同步状态：success / failed / skipped */
    private String status;

    /** 状态消息 */
    private String message;

    public List<Map<String, Object>> getRows() {
        return rows;
    }

    public void setRows(List<Map<String, Object>> rows) {
        this.rows = rows;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getSynced() {
        return synced;
    }

    public void setSynced(long synced) {
        this.synced = synced;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
