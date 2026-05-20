package io.yunxi.mcp.database.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 知识库实体
 * <p>
 * 存储知识库条目的信息，包括标题、内容、类型、标签等。
 * </p>
 *
 * @author yunxi
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class KnowledgeBase {
    /**
     * 知识库条目唯一标识
     */
    private String id;
    /**
     * 所属区县ID（为空表示全局知识）
     */
    private String districtId;
    /**
     * 知识库标题
     */
    private String title;
    /**
     * 知识库内容
     */
    private String content;
    /**
     * 知识库类型（如 architecture、business、issue 等）
     */
    private String type;
    /**
     * 标签（逗号分隔）
     */
    private String tags;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
