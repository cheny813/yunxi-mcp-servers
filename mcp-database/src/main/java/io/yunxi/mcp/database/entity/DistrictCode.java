package io.yunxi.mcp.database.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 代码版本实体
 * <p>
 * 存储区县代码版本信息，包括 Git 分支、提交哈希、版本号等。
 * </p>
 *
 * @author yunxi
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class DistrictCode {
    /**
     * 代码版本唯一标识
     */
    private String id;
    /**
     * 所属区县ID
     */
    private String districtId;
    /**
     * Git 分支名称
     */
    private String gitBranch;
    /**
     * Git 提交哈希值
     */
    private String gitCommit;
    /**
     * 版本号（如 v1.0.0）
     */
    private String gitVersion;
    /**
     * 代码存放路径
     */
    private String codePath;
    /**
     * 是否为默认版本
     */
    private Boolean isDefault;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
