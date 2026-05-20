package io.yunxi.mcp.common.security;

/**
 * Token 验证器接口
 *
 * @since 1.0.0
 */
public interface TokenValidator {

    /**
     * 验证 Token 是否有效
     *
     * @param token 待验证的 Token
     * @return true 如果 Token 有效，否则 false
     */
    boolean validate(String token);
}
