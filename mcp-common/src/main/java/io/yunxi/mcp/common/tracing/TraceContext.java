package io.yunxi.mcp.common.tracing;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * MCP 请求链路追踪上下文
 * <p>
 * 管理跨模块的 TraceId 传递，支持：
 * - 自动生成 TraceId
 * - 从请求中提取 TraceId
 * - 将 TraceId 注入 MDC 用于日志输出
 * - 清理 TraceId 防止内存泄漏
 * </p>
 */
public class TraceContext {

    /**
     * MDC 中 TraceId 的键名
     */
    public static final String TRACE_ID_KEY = "traceId";

    /**
     * HTTP 请求头中 TraceId 的键名
     */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    /**
     * 生成新的 TraceId
     *
     * @return 新的 TraceId（UUID 格式，去除横线）
     */
    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 获取当前线程的 TraceId
     *
     * @return TraceId，如果不存在返回 null
     */
    public static String getTraceId() {
        return MDC.get(TRACE_ID_KEY);
    }

    /**
     * 设置当前线程的 TraceId
     *
     * @param traceId TraceId
     */
    public static void setTraceId(String traceId) {
        if (traceId != null && !traceId.isEmpty()) {
            MDC.put(TRACE_ID_KEY, traceId);
        }
    }

    /**
     * 设置 TraceId，如果不存在则生成新的
     *
     * @param traceId TraceId（可为 null）
     * @return 最终使用的 TraceId
     */
    public static String setTraceIdOrGenerate(String traceId) {
        if (traceId == null || traceId.isEmpty()) {
            traceId = generateTraceId();
        }
        MDC.put(TRACE_ID_KEY, traceId);
        return traceId;
    }

    /**
     * 清除当前线程的 TraceId
     */
    public static void clear() {
        MDC.remove(TRACE_ID_KEY);
    }

    /**
     * 使用指定的 TraceId 执行 Runnable
     *
     * @param traceId  TraceId
     * @param runnable 要执行的任务
     */
    public static void runWithTraceId(String traceId, Runnable runnable) {
        String previousTraceId = getTraceId();
        try {
            setTraceId(traceId);
            runnable.run();
        } finally {
            if (previousTraceId != null) {
                MDC.put(TRACE_ID_KEY, previousTraceId);
            } else {
                clear();
            }
        }
    }

    /**
     * 包装 Runnable，使其在执行时携带当前 TraceId
     *
     * @param runnable 原始 Runnable
     * @return 包装后的 Runnable
     */
    public static Runnable wrap(Runnable runnable) {
        String traceId = getTraceId();
        return () -> runWithTraceId(traceId, runnable);
    }
}
