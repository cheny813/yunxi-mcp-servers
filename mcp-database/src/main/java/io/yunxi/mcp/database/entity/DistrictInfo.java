package io.yunxi.mcp.database.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 区县信息实体
 * <p>
 * 存储区县的基本信息，包括名称、省份、联系人等。
 * </p>
 *
 * @author yunxi
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class DistrictInfo {
    /**
     * 区县唯一标识
     */
    private String id;
    /**
     * 区县名称
     */
    private String name;
    /**
     * 所属省份
     */
    private String province;
    /**
     * 联系人
     */
    private String contact;
    /**
     * 状态（active/inactive）
     */
    private String status;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
