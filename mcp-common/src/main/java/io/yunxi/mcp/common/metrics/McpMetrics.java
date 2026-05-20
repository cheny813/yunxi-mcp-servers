package io.yunxi.mcp.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * MCP 工具调用指标收集器
 * <p>
 * 提供 MCP 服务的监控指标：
 * <ul>
 *   <li>mcp.tool.calls - 工具调用总数</li>
 *   <li>mcp.tool.success.count - 成功调用数</li>
 *   <li>mcp.tool.error.count - 失败调用数</li>
 *   <li>mcp.tool.latency - 调用延迟</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Slf4j
@Component
public class McpMetrics {

    @Autowired
    private MeterRegistry meterRegistry;

    private Counter toolCallCounter;
    private Counter toolSuccessCounter;
    private Counter toolErrorCounter;
    private Timer toolLatencyTimer;

    @PostConstruct
    public void init() {
        // 工具调用计数器
        toolCallCounter = Counter.builder("mcp.tool.calls")
                .description("Total number of MCP tool calls")
                .register(meterRegistry);

        // 成功调用计数器
        toolSuccessCounter = Counter.builder("mcp.tool.success.count")
                .description("Total number of successful MCP tool calls")
                .register(meterRegistry);

        // 失败调用计数器
        toolErrorCounter = Counter.builder("mcp.tool.error.count")
                .description("Total number of failed MCP tool calls")
                .register(meterRegistry);

        // 调用延迟计时器
        toolLatencyTimer = Timer.builder("mcp.tool.latency")
                .description("MCP tool call latency")
                .register(meterRegistry);

        log.info("McpMetrics 初始化完成");
    }

    /**
     * 记录工具调用
     */
    public void recordToolCall() {
        toolCallCounter.increment();
    }

    /**
     * 记录成功调用
     */
    public void recordToolSuccess() {
        toolSuccessCounter.increment();
    }

    /**
     * 记录失败调用
     */
    public void recordToolError() {
        toolErrorCounter.increment();
    }

    /**
     * 记录调用延迟
     */
    public void recordLatency(long millis) {
        toolLatencyTimer.record(millis, TimeUnit.MILLISECONDS);
    }

    /**
     * 记录带计时的工具调用
     */
    public void recordToolExecution(boolean success, long millis) {
        recordToolCall();
        recordLatency(millis);
        if (success) {
            recordToolSuccess();
        } else {
            recordToolError();
        }
    }
}
