package io.yunxi.mcp.knowledge.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 变更监控服务
 * 监控 Git 提交和数据库 Schema 变化，触发知识库自动更新
 *
 * @author yunxi-mcp-servers
 */
@Service
public class ChangeMonitorService {

    private static final Logger log = LoggerFactory.getLogger(ChangeMonitorService.class);

    private final KnowledgeExtractionService extractionService;
    private final KnowledgeVectorService knowledgeVectorService;

    // 存储仓库的最后提交哈希
    private final Map<String, String> lastCommitHash = new ConcurrentHashMap<>();

    // 存储数据库的最后检查时间
    private final Map<String, LocalDateTime> lastDbCheckTime = new ConcurrentHashMap<>();

    // 监控配置
    private final Map<String, MonitorConfig> monitorConfigs = new ConcurrentHashMap<>();

    public ChangeMonitorService(
            KnowledgeExtractionService extractionService,
            KnowledgeVectorService knowledgeVectorService) {
        this.extractionService = extractionService;
        this.knowledgeVectorService = knowledgeVectorService;
    }

    /**
     * 添加监控配置
     *
     * @param repoId     仓库ID
     * @param repoPath   仓库路径
     * @param districtId 区县ID
     */
    public void addMonitorConfig(String repoId, String repoPath, String districtId) {
        MonitorConfig config = new MonitorConfig(repoId, repoPath, districtId);
        monitorConfigs.put(repoId, config);

        // 初始化检查
        checkGitChanges(repoId);
        log.info("已添加仓库监控: {} -> {}", repoPath, districtId);
    }

    /**
     * 定时检查变更 (每5分钟)
     */
    @Scheduled(fixedRate = 300000)
    public void checkAllChanges() {
        log.debug("开始检查所有仓库变更...");

        for (String repoId : monitorConfigs.keySet()) {
            try {
                checkGitChanges(repoId);
            } catch (Exception e) {
                log.error("检查仓库变更失败: {}", repoId, e);
            }
        }
    }

    /**
     * 检查 Git 变更
     */
    private void checkGitChanges(String repoId) {
        MonitorConfig config = monitorConfigs.get(repoId);
        if (config == null)
            return;

        try {
            Path repoPath = Paths.get(config.repoPath());
            if (!Files.exists(repoPath)) {
                log.warn("仓库路径不存在: {}", config.repoPath());
                return;
            }

            // 获取当前 HEAD 提交哈希
            String currentHash = getCurrentGitHash(repoPath);

            if (currentHash == null) {
                log.warn("无法获取 Git 提交哈希: {}", repoPath);
                return;
            }

            // 比较变更
            String lastHash = lastCommitHash.get(repoId);

            if (lastHash == null) {
                // 首次检查，初始化
                lastCommitHash.put(repoId, currentHash);
                log.info("初始化仓库监控: {} = {}", repoId, currentHash);
                return;
            }

            if (!currentHash.equals(lastHash)) {
                log.info("检测到仓库变更: {} ({} -> {})", repoId, lastHash, currentHash);

                // 触发知识更新
                triggerKnowledgeUpdate(repoId, lastHash, currentHash);

                // 更新哈希
                lastCommitHash.put(repoId, currentHash);
            }

        } catch (Exception e) {
            log.error("检查 Git 变更失败: {}", repoId, e);
        }
    }

    /**
     * 触发知识更新
     */
    private void triggerKnowledgeUpdate(String repoId, String oldHash, String newHash) {
        MonitorConfig config = monitorConfigs.get(repoId);
        if (config == null)
            return;

        try {
            // 获取变更的文件列表
            List<String> changedFiles = getChangedFiles(config.repoPath(), oldHash, newHash);

            log.info("变更文件数量: {}", changedFiles.size());

            // 判断变更类型
            boolean hasSchemaChange = changedFiles.stream()
                    .anyMatch(f -> f.endsWith(".sql") || f.contains("schema"));

            boolean hasCodeChange = changedFiles.stream()
                    .anyMatch(f -> f.endsWith(".java") || f.endsWith(".xml"));

            // 提取变更知识
            if (hasSchemaChange || hasCodeChange) {
                var result = extractionService.extractFromCode(config.repoPath(), config.districtId());

                log.info("知识更新结果: {}", result.message());

                // 记录变更历史
                recordChangeHistory(repoId, oldHash, newHash, changedFiles);
            }

        } catch (Exception e) {
            log.error("触发知识更新失败: {}", repoId, e);
        }
    }

    /**
     * 获取变更文件列表
     */
    private List<String> getChangedFiles(String repoPath, String oldHash, String newHash) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("git", "diff", "--name-only", oldHash, newHash);
        pb.directory(new File(repoPath));
        pb.redirectErrorStream(true);

        Process process = pb.start();

        List<String> files = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    files.add(line.trim());
                }
            }
        }

        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            log.warn("git diff 命令超时");
        }
        return files;
    }

    /**
     * 获取当前 Git 提交哈希
     */
    private String getCurrentGitHash(Path repoPath) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("git", "rev-parse", "HEAD");
        pb.directory(repoPath.toFile());
        pb.redirectErrorStream(true);

        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String hash = reader.readLine();
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return null;
            }
            return hash != null ? hash.trim() : null;
        }
    }

    /**
     * 记录变更历史
     */
    private void recordChangeHistory(String repoId, String oldHash, String newHash, List<String> files) {
        log.info("=== 知识库变更记录 ===");
        log.info("仓库: {}", repoId);
        log.info("变更: {} -> {}", oldHash.substring(0, 7), newHash.substring(0, 7));
        log.info("文件数: {}", files.size());
        files.stream().limit(5).forEach(f -> log.info("  - {}", f));
    }

    /**
     * 手动触发更新
     */
    public Map<String, Object> manualSync(String repoId) {
        MonitorConfig config = monitorConfigs.get(repoId);
        if (config == null) {
            return Map.of("success", false, "error", "未找到监控配置: " + repoId);
        }

        try {
            // 强制重新提取
            var result = extractionService.extractFromCode(config.repoPath(), config.districtId());

            return Map.of(
                    "success", result.success(),
                    "message", result.message(),
                    "extractedCount", result.extractedCount());
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * 获取监控状态
     */
    public Map<String, Object> getMonitorStatus() {
        Map<String, Object> status = new HashMap<>();

        status.put("totalRepos", monitorConfigs.size());
        status.put("monitors", monitorConfigs.keySet().stream()
                .map(id -> {
                    Map<String, Object> info = new HashMap<>();
                    info.put("repoId", id);
                    info.put("lastCommit", lastCommitHash.getOrDefault(id, "N/A"));
                    return info;
                })
                .toList());

        return status;
    }

    /**
     * 监控配置
     */
    private record MonitorConfig(String repoId, String repoPath, String districtId) {
    }
}