package io.yunxi.mcp.monitoring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 系统监控 MCP 服务器应用程序入口
 *
 * <p>
 * 提供系统监控指标的采集、查询和告警能力的 MCP 服务器。
 * 支持操作系统级指标（CPU、内存、磁盘、网络）和 JVM 指标监控，
 * 以及自定义健康检查和告警规则管理。
 * </p>
 *
 * <h3>功能特性</h3>
 * <ul>
 * <li>系统指标采集 - CPU/内存/磁盘/网络等操作系统级指标</li>
 * <li>JVM 指标监控 - 堆内存/线程/GC 等 JVM 运行时指标</li>
 * <li>健康检查 - 自定义 HTTP/TCP/进程健康检查</li>
 * <li>告警管理 - 指标阈值告警规则配置与查询</li>
 * </ul>
 *
 * <h3>使用方式</h3>
 *
 * <pre>
 * java -jar mcp-monitoring-1.0.0.jar --server.port=40106
 * </pre>
 *
 * <h3>提供工具</h3>
 * <ul>
 * <li>get_system_metrics - 获取系统指标</li>
 * <li>get_jvm_metrics - 获取 JVM 指标</li>
 * <li>check_health - 执行健康检查</li>
 * <li>list_alerts - 列出告警</li>
 * <li>set_alert_rule - 设置告警规则</li>
 * </ul>
 *
 * @author yunxi-mcp-servers
 * @version 1.0.0
 * @since 2026-04-25
 */
@SpringBootApplication
public class MonitoringMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonitoringMcpApplication.class, args);
    }
}
