package io.yunxi.mcp.database.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 区县数据库配置实体
 * <p>
 * 存储区县所属的数据库连接信息。
 * </p>
 *
 * @author yunxi
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class DistrictDb {
    /**
     * 数据库配置唯一标识
     */
    private String id;
    /**
     * 所属区县ID
     */
    private String districtId;
    /**
     * 数据库类型（如 nutrition、safety 等）
     */
    private String dbType;
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
    private String databaseName;
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
     * 创建时间
     */
    private LocalDateTime createdAt;
}
