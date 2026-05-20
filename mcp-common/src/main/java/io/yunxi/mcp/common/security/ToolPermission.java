package io.yunxi.mcp.common.security;

import lombok.Data;

import java.util.List;

/**
 * 工具权限定义
 * <p>
 * 定义工具的访问权限控制
 *
 * @since 1.0.0
 */
@Data
public class ToolPermission {

    /**
     * 工具名称
     */
    private String toolName;

    /**
     * 允许访问的角色列表
     */
    private List<String> allowedRoles;

    /**
     * 允许访问的用户列表
     */
    private List<String> allowedUsers;

    /**
     * 是否需要认证
     */
    private boolean requireAuth = true;

    /**
     * 检查用户是否有权限访问工具
     */
    public boolean hasPermission(String userId, List<String> userRoles) {
        // 如果不需要认证，允许访问
        if (!requireAuth) {
            return true;
        }

        // 检查用户白名单
        if (allowedUsers != null && allowedUsers.contains(userId)) {
            return true;
        }

        // 检查角色
        if (allowedRoles != null && userRoles != null) {
            return userRoles.stream().anyMatch(allowedRoles::contains);
        }

        return false;
    }
}
