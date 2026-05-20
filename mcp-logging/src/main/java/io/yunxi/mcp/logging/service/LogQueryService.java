package io.yunxi.mcp.logging.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 日志查询服务
 * <p>
 * 提供日志文件的查询、搜索和列表功能。
 * 支持按关键词、日志级别、时间范围进行过滤。
 * </p>
 *
 * <h3>功能特性</h3>
 * <ul>
 * <li>关键词搜索 - 支持不区分大小写的关键词匹配</li>
 * <li>级别过滤 - 支持 ERROR、WARN、INFO、DEBUG 等级别过滤</li>
 * <li>时间范围 - 支持按时间范围过滤日志</li>
 * <li>多格式支持 - 支持多种时间戳格式解析</li>
 * </ul>
 *
 * <h3>支持的时间格式</h3>
 * <ul>
 * <li>yyyy-MM-dd HH:mm:ss</li>
 * <li>yyyy-MM-dd HH:mm:ss.SSS</li>
 * <li>yyyy/MM/dd HH:mm:ss</li>
 * <li>dd/MMM/yyyy:HH:mm:ss</li>
 * </ul>
 *
 * @author yunxi-mcp-servers
 * @since 1.0.0
 */
@Service
public class LogQueryService {

    private static final Logger log = LoggerFactory.getLogger(LogQueryService.class);

    /**
     * 支持的日志时间戳格式列表
     */
    private static final List<DateTimeFormatter> TIME_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss"));

    /**
     * 日志文件基础路径
     */
    private final String logBasePath;

    /**
     * 构造日志查询服务
     *
     * @param logBasePath 日志文件基础路径，默认为 ./logs
     */
    public LogQueryService(@Value("${logging.file.path:./logs}") String logBasePath) {
        this.logBasePath = logBasePath;
    }

    /**
     * 查询日志
     * <p>
     * 根据关键词、日志级别、时间范围搜索日志内容。
     * 支持相对时间表达式（如 "1小时前", "30分钟前"）。
     * </p>
     *
     * @param keyword   搜索关键词，可选
     * @param level     日志级别过滤（ERROR/WARN/INFO/DEBUG），可选
     * @param startTime 开始时间，可选，支持格式：yyyy-MM-dd HH:mm:ss 或相对时间
     * @param endTime   结束时间，可选，支持格式同上
     * @param maxLines  最大返回行数
     * @return 匹配的日志行列表，格式：文件名 | 日志内容
     */
    public List<String> queryLogs(String keyword, String level, String startTime, String endTime, int maxLines) {
        List<File> logFiles = findLogFiles();

        List<String> results = new ArrayList<>();
        int count = 0;

        // 解析时间范围
        LocalDateTime start = parseTime(startTime);
        LocalDateTime end = parseTime(endTime);

        for (File file : logFiles) {
            if (count >= maxLines)
                break;

            try {
                List<String> matched = searchInFile(file, keyword, level, start, end);
                for (String line : matched) {
                    results.add(file.getName() + " | " + line);
                    count++;
                    if (count >= maxLines)
                        break;
                }
            } catch (IOException e) {
                log.warn("读取日志文件失败: {}", file.getName(), e);
            }
        }

        return results;
    }

    /**
     * 在单个文件中搜索日志
     *
     * @param file      日志文件
     * @param keyword   关键词
     * @param level     日志级别
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 匹配的日志行列表
     * @throws IOException 读取文件失败时抛出
     */
    private List<String> searchInFile(File file, String keyword, String level, LocalDateTime startTime,
            LocalDateTime endTime) throws IOException {
        List<String> results = new ArrayList<>();

        Pattern levelPattern = level != null
                ? Pattern.compile(".*(" + level.toUpperCase() + ").*", Pattern.CASE_INSENSITIVE)
                : null;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // 级别过滤
                if (levelPattern != null && !levelPattern.matcher(line).matches()) {
                    continue;
                }

                // 关键词过滤
                if (keyword != null && !keyword.isEmpty() && !line.toLowerCase().contains(keyword.toLowerCase())) {
                    continue;
                }

                // 时间范围过滤
                if (startTime != null || endTime != null) {
                    LocalDateTime lineTime = extractTimestamp(line);
                    if (lineTime != null) {
                        if (startTime != null && lineTime.isBefore(startTime)) {
                            continue;
                        }
                        if (endTime != null && lineTime.isAfter(endTime)) {
                            continue;
                        }
                    }
                }

                results.add(line);
            }
        }

        return results;
    }

    /**
     * 从日志行中提取时间戳
     *
     * @param line 日志行
     * @return 解析的时间，如果无法解析则返回 null
     */
    private LocalDateTime extractTimestamp(String line) {
        // 尝试匹配常见日志格式的时间戳
        // 格式: 2024-01-01 12:00:00
        Pattern pattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2})");
        Matcher matcher = pattern.matcher(line);

        if (matcher.find()) {
            String timestamp = matcher.group(1);
            for (DateTimeFormatter formatter : TIME_FORMATTERS) {
                try {
                    return LocalDateTime.parse(timestamp, formatter);
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    /**
     * 解析时间字符串
     * <p>
     * 支持标准时间格式和相对时间表达式。
     * </p>
     *
     * @param timeStr 时间字符串
     * @return 解析的 LocalDateTime，如果无法解析则返回 null
     */
    private LocalDateTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            return null;
        }

        // 尝试标准格式
        for (DateTimeFormatter formatter : TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(timeStr, formatter);
            } catch (Exception ignored) {
            }
        }

        // 尝试相对时间 (如 "1小时前", "30分钟前")
        try {
            if (timeStr.contains("小时")) {
                int hours = Integer.parseInt(timeStr.replaceAll("[^0-9]", ""));
                return LocalDateTime.now().minusHours(hours);
            } else if (timeStr.contains("分钟")) {
                int minutes = Integer.parseInt(timeStr.replaceAll("[^0-9]", ""));
                return LocalDateTime.now().minusMinutes(minutes);
            } else if (timeStr.contains("天")) {
                int days = Integer.parseInt(timeStr.replaceAll("[^0-9]", ""));
                return LocalDateTime.now().minusDays(days);
            }
        } catch (Exception ignored) {
        }

        log.warn("无法解析时间字符串: {}", timeStr);
        return null;
    }

    /**
     * 查找日志目录中的所有日志文件
     *
     * @return 日志文件列表，按修改时间倒序排列
     */
    private List<File> findLogFiles() {
        File logDir = new File(logBasePath);
        if (!logDir.exists() || !logDir.isDirectory()) {
            log.warn("日志目录不存在: {}", logBasePath);
            return Collections.emptyList();
        }

        List<File> logFiles = new ArrayList<>();
        File[] files = logDir.listFiles((dir, name) -> name.endsWith(".log"));

        if (files != null) {
            logFiles.addAll(Arrays.asList(files));
        }

        // 按修改时间排序，最新的在前
        logFiles.sort(Comparator.comparing(File::lastModified).reversed());

        return logFiles;
    }

    /**
     * 获取日志文件列表
     *
     * @return 日志文件信息列表，格式：文件名 (大小 KB)
     */
    public List<String> listLogFiles() {
        return findLogFiles().stream()
                .map(f -> f.getName() + " (" + (f.length() / 1024) + " KB)")
                .collect(Collectors.toList());
    }
}
