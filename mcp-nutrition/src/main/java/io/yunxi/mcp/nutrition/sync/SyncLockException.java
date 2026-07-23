package io.yunxi.mcp.nutrition.sync;

/**
 * 同步锁异常
 * <p>
 * 当同步正在进行时，再次触发同步会抛出此异常，防止并发冲突。
 * </p>
 *
 * @author yunxi-agent-platform
 * @version 2.0.0
 */
public class SyncLockException extends RuntimeException {

    public SyncLockException(String message) {
        super(message);
    }

    public SyncLockException(String message, Throwable cause) {
        super(message, cause);
    }
}
