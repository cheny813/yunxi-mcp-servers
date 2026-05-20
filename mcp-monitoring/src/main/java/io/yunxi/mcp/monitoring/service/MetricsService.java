package io.yunxi.mcp.monitoring.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.lang.management.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 指标采集服务
 *
 * <p>
 * 采集操作系统级和 JVM 级的运行指标，支持 CPU、内存、磁盘、网络等维度的监控数据。
 * </p>
 *
 * @author yunxi-mcp-servers
 * @version 1.0.0
 */
@Slf4j
@Service
public class MetricsService {

    /** 操作系统 MXBean */
    private final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

    /** 运行时 MXBean */
    private final RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();

    /** 内存 MXBean */
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

    /** 告警检查间隔（秒） */
    @Value("${monitoring.alert.check-interval-seconds:60}")
    private int checkIntervalSeconds;

    /**
     * 获取系统指标
     *
     * @param metricNames 指标名称列表（null 返回全部）
     * @return 指标数据
     */
    public Map<String, Object> getSystemMetrics(List<String> metricNames) {
        Map<String, Object> metrics = new LinkedHashMap<>();

        // CPU 指标
        if (shouldInclude(metricNames, "cpu")) {
            metrics.put("cpu", getCpuMetrics());
        }

        // 内存指标
        if (shouldInclude(metricNames, "memory")) {
            metrics.put("memory", getMemoryMetrics());
        }

        // 磁盘指标
        if (shouldInclude(metricNames, "disk")) {
            metrics.put("disk", getDiskMetrics());
        }

        // 网络指标
        if (shouldInclude(metricNames, "network")) {
            metrics.put("network", getNetworkMetrics());
        }

        // 操作系统信息
        if (shouldInclude(metricNames, "os")) {
            metrics.put("os", getOsInfo());
        }

        // 如果未指定指标名，返回全部
        if (metricNames == null || metricNames.isEmpty()) {
            metrics.put("cpu", getCpuMetrics());
            metrics.put("memory", getMemoryMetrics());
            metrics.put("disk", getDiskMetrics());
            metrics.put("network", getNetworkMetrics());
            metrics.put("os", getOsInfo());
        }

        metrics.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return metrics;
    }

    /**
     * 获取 JVM 指标
     *
     * @return JVM 指标数据
     */
    public Map<String, Object> getJvmMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();

        // 堆内存
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        Map<String, Object> heap = new LinkedHashMap<>();
        heap.put("used", formatBytes(heapUsage.getUsed()));
        heap.put("committed", formatBytes(heapUsage.getCommitted()));
        heap.put("max", formatBytes(heapUsage.getMax()));
        heap.put("usedPercent", String.format("%.1f%%", (double) heapUsage.getUsed() / heapUsage.getMax() * 100));
        metrics.put("heap", heap);

        // 非堆内存
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        Map<String, Object> nonHeap = new LinkedHashMap<>();
        nonHeap.put("used", formatBytes(nonHeapUsage.getUsed()));
        nonHeap.put("committed", formatBytes(nonHeapUsage.getCommitted()));
        metrics.put("nonHeap", nonHeap);

        // 线程
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        Map<String, Object> threads = new LinkedHashMap<>();
        threads.put("count", threadBean.getThreadCount());
        threads.put("peakCount", threadBean.getPeakThreadCount());
        threads.put("daemonCount", threadBean.getDaemonThreadCount());
        threads.put("totalStarted", threadBean.getTotalStartedThreadCount());
        metrics.put("threads", threads);

        // GC
        Map<String, Object> gc = new LinkedHashMap<>();
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            Map<String, Object> gcInfo = new LinkedHashMap<>();
            gcInfo.put("count", gcBean.getCollectionCount());
            gcInfo.put("timeMs", gcBean.getCollectionTime());
            gc.put(gcBean.getName(), gcInfo);
        }
        metrics.put("gc", gc);

        // 运行时
        Map<String, Object> runtime = new LinkedHashMap<>();
        runtime.put("uptime", formatDuration(runtimeBean.getUptime()));
        runtime.put("startTime", new Date(runtimeBean.getStartTime()).toString());
        runtime.put("vmName", runtimeBean.getVmName());
        runtime.put("vmVersion", runtimeBean.getVmVersion());
        metrics.put("runtime", runtime);

        metrics.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return metrics;
    }

    // ==================== 私有方法 ====================

    private Map<String, Object> getCpuMetrics() {
        Map<String, Object> cpu = new LinkedHashMap<>();
        cpu.put("processors", osBean.getAvailableProcessors());
        cpu.put("systemLoadAverage", osBean.getSystemLoadAverage());

        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
            cpu.put("processCpuLoad", String.format("%.1f%%", sunOsBean.getProcessCpuLoad() * 100));
            cpu.put("systemCpuLoad", String.format("%.1f%%", sunOsBean.getCpuLoad() * 100));
        }
        return cpu;
    }

    private Map<String, Object> getMemoryMetrics() {
        Map<String, Object> memory = new LinkedHashMap<>();

        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
            memory.put("totalPhysical", formatBytes(sunOsBean.getTotalMemorySize()));
            memory.put("freePhysical", formatBytes(sunOsBean.getFreeMemorySize()));
            memory.put("usedPhysical", formatBytes(sunOsBean.getTotalMemorySize() - sunOsBean.getFreeMemorySize()));
            long totalSwap = sunOsBean.getTotalSwapSpaceSize();
            long freeSwap = sunOsBean.getFreeSwapSpaceSize();
            memory.put("totalSwap", formatBytes(totalSwap));
            memory.put("freeSwap", formatBytes(freeSwap));
            memory.put("usedSwap", formatBytes(totalSwap - freeSwap));
            memory.put("physicalUsedPercent",
                    String.format("%.1f%%", (double) (sunOsBean.getTotalMemorySize() - sunOsBean.getFreeMemorySize())
                            / sunOsBean.getTotalMemorySize() * 100));
        } else {
            Runtime runtime = Runtime.getRuntime();
            memory.put("jvmTotalMemory", formatBytes(runtime.totalMemory()));
            memory.put("jvmFreeMemory", formatBytes(runtime.freeMemory()));
            memory.put("jvmMaxMemory", formatBytes(runtime.maxMemory()));
        }
        return memory;
    }

    private Map<String, Object> getDiskMetrics() {
        Map<String, Object> disk = new LinkedHashMap<>();
        java.io.File[] roots = java.io.File.listRoots();
        List<Map<String, Object>> partitions = new ArrayList<>();

        for (java.io.File root : roots) {
            Map<String, Object> partition = new LinkedHashMap<>();
            partition.put("path", root.getAbsolutePath());
            partition.put("total", formatBytes(root.getTotalSpace()));
            partition.put("free", formatBytes(root.getFreeSpace()));
            partition.put("usable", formatBytes(root.getUsableSpace()));
            partition.put("usedPercent",
                    root.getTotalSpace() > 0
                            ? String.format("%.1f%%", (double) (root.getTotalSpace() - root.getFreeSpace()) / root.getTotalSpace() * 100)
                            : "N/A");
            partitions.add(partition);
        }
        disk.put("partitions", partitions);
        return disk;
    }

    private Map<String, Object> getNetworkMetrics() {
        Map<String, Object> network = new LinkedHashMap<>();
        try {
            java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();
            List<Map<String, Object>> nicList = new ArrayList<>();

            while (interfaces != null && interfaces.hasMoreElements()) {
                java.net.NetworkInterface nic = interfaces.nextElement();
                if (nic.isLoopback() || !nic.isUp()) {
                    continue;
                }
                Map<String, Object> nicInfo = new LinkedHashMap<>();
                nicInfo.put("name", nic.getName());
                nicInfo.put("displayName", nic.getDisplayName());

                List<String> addresses = new ArrayList<>();
                nic.getInterfaceAddresses().forEach(addr ->
                        addresses.add(addr.getAddress().getHostAddress() + "/" + addr.getNetworkPrefixLength()));
                nicInfo.put("addresses", addresses);
                nicList.add(nicInfo);
            }
            network.put("interfaces", nicList);
        } catch (Exception e) {
            network.put("error", "获取网络接口失败: " + e.getMessage());
        }
        return network;
    }

    private Map<String, Object> getOsInfo() {
        Map<String, Object> os = new LinkedHashMap<>();
        os.put("name", osBean.getName());
        os.put("version", osBean.getVersion());
        os.put("arch", osBean.getArch());
        return os;
    }

    private boolean shouldInclude(List<String> metricNames, String name) {
        return metricNames != null && metricNames.contains(name);
    }

    private String formatBytes(long bytes) {
        if (bytes < 0) return "N/A";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private String formatDuration(long uptimeMs) {
        long seconds = uptimeMs / 1000;
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%dd %dh %dm %ds", days, hours, minutes, secs);
    }
}
