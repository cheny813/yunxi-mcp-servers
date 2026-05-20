package io.yunxi.mcp.monitoring.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 告警与健康检查服务
 *
 * <p>
 * 提供 HTTP/TCP 健康检查、进程检测、告警规则管理和告警历史查询。
 * 告警规则支持按指标名称和阈值自动触发告警。
 * </p>
 *
 * @author yunxi-mcp-servers
 * @version 1.0.0
 */
@Slf4j
@Service
public class AlertService {

    /** 告警规则: ruleId -> AlertRule */
    private final Map<String, AlertRule> alertRules = new ConcurrentHashMap<>();

    /** 告警历史: alertId -> AlertRecord */
    private final Map<String, AlertRecord> alertHistory = new ConcurrentHashMap<>();

    /** 最大告警历史数 */
    @Value("${monitoring.alert.max-alerts-history:1000}")
    private int maxAlertsHistory;

    // ==================== 健康检查 ====================

    /**
     * 执行 HTTP 健康检查
     *
     * @param url      目标 URL
     * @param timeoutMs 超时毫秒
     * @return 健康检查结果
     */
    public HealthCheckResult checkHttp(String url, int timeoutMs) {
        HealthCheckResult result = new HealthCheckResult();
        result.setType("HTTP");
        result.setTarget(url);
        result.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        long start = System.currentTimeMillis();
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);

            int code = conn.getResponseCode();
            long latency = System.currentTimeMillis() - start;

            result.setHealthy(code >= 200 && code < 400);
            result.setStatusCode(code);
            result.setLatencyMs(latency);
            result.setMessage(code >= 200 && code < 400 ? "OK" : "HTTP " + code);
        } catch (Exception e) {
            result.setHealthy(false);
            result.setLatencyMs(System.currentTimeMillis() - start);
            result.setMessage("连接失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 执行 TCP 健康检查
     *
     * @param host     目标主机
     * @param port     目标端口
     * @param timeoutMs 超时毫秒
     * @return 健康检查结果
     */
    public HealthCheckResult checkTcp(String host, int port, int timeoutMs) {
        HealthCheckResult result = new HealthCheckResult();
        result.setType("TCP");
        result.setTarget(host + ":" + port);
        result.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        long start = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            result.setHealthy(true);
            result.setLatencyMs(System.currentTimeMillis() - start);
            result.setMessage("连接成功");
        } catch (Exception e) {
            result.setHealthy(false);
            result.setLatencyMs(System.currentTimeMillis() - start);
            result.setMessage("连接失败: " + e.getMessage());
        }

        return result;
    }

    // ==================== 告警规则管理 ====================

    /**
     * 设置告警规则
     *
     * @param name        规则名称
     * @param metricName  指标名称
     * @param threshold   阈值
     * @param operator    比较运算符 (gt/lt/gte/lte/eq)
     * @param durationSec 持续时间（秒）
     * @param description 描述
     * @return 规则 ID
     */
    public String setAlertRule(String name, String metricName, double threshold,
                               String operator, int durationSec, String description) {
        String ruleId = "RULE-" + System.currentTimeMillis();
        AlertRule rule = new AlertRule();
        rule.setRuleId(ruleId);
        rule.setName(name);
        rule.setMetricName(metricName);
        rule.setThreshold(threshold);
        rule.setOperator(operator != null ? operator : "gt");
        rule.setDurationSec(durationSec > 0 ? durationSec : 60);
        rule.setDescription(description);
        rule.setEnabled(true);
        rule.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        alertRules.put(ruleId, rule);
        log.info("[Alert] 告警规则已创建: ruleId={}, name={}, metric={}, threshold={}, op={}",
                ruleId, name, metricName, threshold, operator);

        return ruleId;
    }

    /**
     * 删除告警规则
     *
     * @param ruleId 规则 ID
     * @return 是否删除成功
     */
    public boolean deleteAlertRule(String ruleId) {
        AlertRule removed = alertRules.remove(ruleId);
        if (removed != null) {
            log.info("[Alert] 告警规则已删除: ruleId={}", ruleId);
            return true;
        }
        return false;
    }

    /**
     * 获取所有告警规则
     *
     * @return 告警规则列表
     */
    public List<AlertRule> getAlertRules() {
        return new ArrayList<>(alertRules.values());
    }

    /**
     * 启用/禁用告警规则
     *
     * @param ruleId  规则 ID
     * @param enabled 是否启用
     */
    public void toggleAlertRule(String ruleId, boolean enabled) {
        AlertRule rule = alertRules.get(ruleId);
        if (rule != null) {
            rule.setEnabled(enabled);
            log.info("[Alert] 告警规则{}: ruleId={}", enabled ? "已启用" : "已禁用", ruleId);
        }
    }

    // ==================== 告警历史 ====================

    /**
     * 记录告警
     *
     * @param ruleName  规则名称
     * @param metricName 指标名称
     * @param value     当前值
     * @param threshold 阈值
     * @param level     告警级别
     * @param message   告警消息
     */
    public void recordAlert(String ruleName, String metricName, double value,
                            double threshold, String level, String message) {
        // 超过历史上限时清理最早的记录
        if (alertHistory.size() >= maxAlertsHistory) {
            String oldestKey = alertHistory.entrySet().stream()
                    .min(Comparator.comparingLong(e -> Long.parseLong(e.getKey().split("-")[1])))
                    .map(Map.Entry::getKey)
                    .orElse(null);
            if (oldestKey != null) {
                alertHistory.remove(oldestKey);
            }
        }

        String alertId = "ALERT-" + System.currentTimeMillis();
        AlertRecord record = new AlertRecord();
        record.setAlertId(alertId);
        record.setRuleName(ruleName);
        record.setMetricName(metricName);
        record.setCurrentValue(value);
        record.setThreshold(threshold);
        record.setLevel(level);
        record.setMessage(message);
        record.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        record.setAcknowledged(false);

        alertHistory.put(alertId, record);
        log.warn("[Alert] 告警触发: {} - {} (当前值={}, 阈值={})", ruleName, message, value, threshold);
    }

    /**
     * 查询告警历史
     *
     * @param level     告警级别过滤（可选）
     * @param metricName 指标名称过滤（可选）
     * @param limit     最大返回数
     * @return 告警列表
     */
    public List<AlertRecord> getAlerts(String level, String metricName, int limit) {
        return alertHistory.values().stream()
                .filter(r -> level == null || level.equals(r.getLevel()))
                .filter(r -> metricName == null || metricName.equals(r.getMetricName()))
                .sorted(Comparator.comparing(AlertRecord::getTimestamp).reversed())
                .limit(limit > 0 ? limit : 50)
                .toList();
    }

    /**
     * 确认告警
     *
     * @param alertId 告警 ID
     */
    public void acknowledgeAlert(String alertId) {
        AlertRecord record = alertHistory.get(alertId);
        if (record != null) {
            record.setAcknowledged(true);
            log.info("[Alert] 告警已确认: alertId={}", alertId);
        }
    }

    // ==================== 内部类 ====================

    @Data
    public static class AlertRule {
        private String ruleId;
        private String name;
        private String metricName;
        private double threshold;
        private String operator;
        private int durationSec;
        private String description;
        private boolean enabled;
        private String createdAt;
    }

    @Data
    public static class AlertRecord {
        private String alertId;
        private String ruleName;
        private String metricName;
        private double currentValue;
        private double threshold;
        private String level;
        private String message;
        private String timestamp;
        private boolean acknowledged;
    }

    @Data
    public static class HealthCheckResult {
        private String type;
        private String target;
        private boolean healthy;
        private int statusCode;
        private long latencyMs;
        private String message;
        private String timestamp;
    }
}
