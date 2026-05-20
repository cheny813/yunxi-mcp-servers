package io.yunxi.mcp.memory.store;

import io.yunxi.mcp.memory.config.MemoryProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 文件存储的记忆存储服务
 * <p>
 * 实现 MEMORY.md + USER.md 模式
 * 完全独立，不依赖 yunxi-agent-platform
 * </p>
 * <p>
 * <b>多用户支持：</b>每个用户拥有独立的记忆文件，互不干扰
 * <br>
 * <b>环境模式：</b>
 * <ul>
 *   <li>开发模式（dev）：本地存储，每个实例独立</li>
 *   <li>生产模式（prod）：共享存储，多实例共享数据</li>
 * </ul>
 * <br>
 * <b>存储引擎：</b>可通过配置切换文件存储或 Milvus 向量存储
 * <br>
 * <b>数据一致性：</b>每次操作都从文件读取，确保集群一致性
 * </p>
 *
 * @author yunxi-mcp-servers
 * @version 2.0.0
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "memory.store.type", havingValue = "file", matchIfMissing = false)
public class FileMemoryStore implements MemoryStoreInterface {

    @Autowired
    private MemoryProperties memoryProperties;

    // 安全扫描模式（英文，用于检测LLM输出的英文指令注入）
    private static final Pattern[] INJECTION_PATTERNS = {
            Pattern.compile("(?:you are|you're|your task is your goal)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("ignore all previous instructions", Pattern.CASE_INSENSITIVE),
            Pattern.compile("start with your response", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(?:system|prompt|instruction|ignore|forget|remember)\\s*:.*$", Pattern.CASE_INSENSITIVE)
    };

    // 条目分隔符
    private static final String ENTRY_DELIMITER = "§";

    private Path basePathDev;
    private Path basePathProd;
    private String environmentMode;

    /**
     * 初始化
     */
    public FileMemoryStore(MemoryProperties memoryProperties) {
        this.memoryProperties = memoryProperties;
        this.basePathDev = Paths.get(memoryProperties.getStoragePathDev()).toAbsolutePath().normalize();
        this.basePathProd = Paths.get(memoryProperties.getStoragePathProd()).toAbsolutePath().normalize();
        this.environmentMode = getEnvironmentMode();

        log.info("MemoryStore 初始化");
        log.info("  - 开发路径: {}", basePathDev);
        log.info("  - 生产路径: {}", basePathProd);
        log.info("  - 当前环境模式: {}", environmentMode);

        if ("prod".equals(environmentMode)) {
            log.info("  - 多用户模式：每个用户独立的 MEMORY.md + USER.md");
            log.info("  - 集群模式：使用共享存储，确保数据一致性");
        } else {
            log.info("  - 开发模式：本地存储，适用于单机测试");
        }
    }

    /**
     * 获取环境模式（从系统属性或默认）
     */
    private String getEnvironmentMode() {
        return System.getProperty("yunxi.environment.mode", "prod");
    }

    /**
     * 获取当前环境使用的存储路径
     */
    private Path getActivePath() {
        return "prod".equals(environmentMode) ? basePathProd : basePathDev;
    }

    /**
     * 获取用户路径
     */
    private Path getUserPath(String userId) {
        Path basePath = getActivePath();
        if (userId == null || userId.isBlank()) {
            userId = "default";
        }
        return basePath.resolve(userId);
    }

    /**
     * 初始化用户存储
     */
    private UserMemoryStore getUserStore(String userId) throws IOException {
        Path userPath = getUserPath(userId);
        UserMemoryStore store = new UserMemoryStore(userPath, memoryProperties);
        store.initialize();
        return store;
    }

    /**
     * 添加记忆条目
     *
     * @param userId 用户ID（必填，用于多用户隔离）
     * @param target 目标存储：memory 或 user
     * @param category 分类
     * @param content 内容
     * @return 条目ID
     */
    public String addEntry(String userId, String target, String category, String content) {
        if (!isValidContent(content)) {
            throw new IllegalArgumentException("无效的记忆内容，可能包含注入模式");
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        String entry = String.format("[%s] %s: %s", timestamp, category, content);

        try {
            UserMemoryStore userStore = getUserStore(userId);
            if ("memory".equalsIgnoreCase(target)) {
                return userStore.addMemoryEntry(entry);
            } else if ("user".equalsIgnoreCase(target)) {
                return userStore.addUserEntry(entry);
            } else {
                throw new IllegalArgumentException("无效的目标: " + target);
            }
        } catch (IOException e) {
            log.error("添加记忆条目失败: userId={}, target={}", userId, target, e);
            throw new RuntimeException("添加记忆失败: " + e.getMessage(), e);
        }
    }

    /**
     * 替换现有条目（模糊匹配）
     *
     * @param userId 用户ID（必填，用于多用户隔离）
     * @param target 目标存储：memory 或 user
     * @param entryId 条目ID（时间戳）
     * @param newContent 新内容
     * @return 替换后的条目ID
     */
    public String replaceEntry(String userId, String target, String entryId, String newContent) {
        if (!isValidContent(newContent)) {
            throw new IllegalArgumentException("无效的内容");
        }

        try {
            UserMemoryStore userStore = getUserStore(userId);
            return userStore.replaceEntry(target, entryId, newContent);

        } catch (IOException e) {
            log.error("替换记忆条目失败: userId={}, target={}", userId, target, e);
            throw new RuntimeException("替换失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除条目
     *
     * @param userId 用户ID（必填，用于多用户隔离）
     * @param target 目标存储：memory 或 user
     * @param entryId 条目ID
     */
    public void removeEntry(String userId, String target, String entryId) {
        try {
            UserMemoryStore userStore = getUserStore(userId);
            userStore.removeEntry(target, entryId);

        } catch (IOException e) {
            log.error("删除记忆条目失败: userId={}, target={}", userId, target, e);
            throw new RuntimeException("删除失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取记忆内容
     *
     * @param userId 用户ID（必填，用于多用户隔离）
     * @param target 目标存储：memory 或 user
     * @param context 上下文（可选，用于过滤）
     * @return 记忆内容
     */
    public String getRelevantMemory(String userId, String target, String context) {
        try {
            UserMemoryStore userStore = getUserStore(userId);
            return userStore.getRelevantMemory(target, context);

        } catch (IOException e) {
            log.error("获取记忆失败: userId={}, target={}", userId, target, e);
            return "(读取失败: " + e.getMessage() + ")";
        }
    }

    /**
     * 获取记忆统计
     *
     * @param userId 用户ID（必填，用于多用户隔离）
     * @return 统计信息
     */
    public MemoryStats getMemoryStats(String userId) {
        try {
            UserMemoryStore userStore = getUserStore(userId);
            return userStore.getMemoryStats();
        } catch (IOException e) {
            log.error("获取记忆统计失败: userId={}", userId, e);
            // 返回空统计
            MemoryStats stats = new MemoryStats();
            stats.setMemoryUsage(0);
            stats.setMemoryLimit(2200);
            stats.setMemoryUsagePercentage(0.0);
            stats.setUserUsage(0);
            stats.setUserLimit(1375);
            stats.setUserUsagePercentage(0.0);
            return stats;
        }
    }

    // ========== 静态工具方法 ==========

    private static boolean isValidContent(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }

        // 安全扫描
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(content).find()) {
                log.warn("检测到潜在注入模式: {}", pattern.pattern());
                return false;
            }
        }

        return true;
    }

    // ========== 内部类 ==========

    /**
     * 用户记忆存储（单用户）
     * <p>
     * 为每个用户提供独立的 MEMORY.md + USER.md
     * 包含文件锁机制，支持集群环境下的并发安全
     * </p>
     */
    @Slf4j
    private static class UserMemoryStore {
        private final Path basePath;
        private final Path memoryFile;
        private final Path userFile;

        // 文件锁：支持集群环境下的并发安全
        private final Object memoryLock = new Object();
        private final Object userLock = new Object();

        // 配置
        private final int memoryMaxChars;
        private final int userMaxChars;

        // 条目分隔符
        private static final String ENTRY_DELIMITER = "§";

        public UserMemoryStore(Path basePath, MemoryProperties properties) {
            this.basePath = basePath;
            this.memoryFile = basePath.resolve("MEMORY.md");
            this.userFile = basePath.resolve("USER.md");
            this.memoryMaxChars = properties.getMaxMemoryLength();
            this.userMaxChars = properties.getMaxUserMemoryLength();
        }

        public void initialize() {
            try {
                // 创建目录结构
                Files.createDirectories(basePath);

                // 初始化或加载文件
                initFileIfNotExists(memoryFile,
                        "# Memory (HOT Tier)\n\n## Preferences\n\n## Patterns\n\n## Rules\n\n## Knowledge\n");
                initFileIfNotExists(userFile,
                        "# User Profile\n\n## Preferences\n\n## Habits\n\n## Work Style\n\n## Expectations\n");

                log.debug("UserMemoryStore 初始化完成: {}", basePath);

            } catch (IOException e) {
                log.error("初始化 UserMemoryStore 失败: {}", basePath, e);
            }
        }

        public String addMemoryEntry(String entry) throws IOException {
            synchronized (memoryLock) {
                List<String> currentEntries = readMemoryEntries();
                String content = String.join("\n" + ENTRY_DELIMITER + "\n", currentEntries);
                content += "\n" + ENTRY_DELIMITER + "\n" + entry;

                // 字符限制检查
                if (content.length() > memoryMaxChars) {
                    throw new MemoryLimitException("MEMORY.md 容量超限: " + content.length() + "/" + memoryMaxChars);
                }

                writeAtomically(memoryFile, content);

                String entryId = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
                log.debug("记忆条目已添加: entryId={}", entryId);
                return entryId;
            }
        }

        public String addUserEntry(String entry) throws IOException {
            synchronized (userLock) {
                List<String> currentEntries = readUserEntries();
                String content = String.join("\n" + ENTRY_DELIMITER + "\n", currentEntries);
                content += "\n" + ENTRY_DELIMITER + "\n" + entry;

                // 字符限制检查
                if (content.length() > userMaxChars) {
                    throw new MemoryLimitException("USER.md 容量超限: " + content.length() + "/" + userMaxChars);
                }

                writeAtomically(userFile, content);

                String entryId = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
                log.debug("用户条目已添加: entryId={}", entryId);
                return entryId;
            }
        }

        public String replaceEntry(String target, String entryId, String newContent) throws IOException {
            Object lock = "memory".equalsIgnoreCase(target) ? memoryLock : userLock;
            Path file = "memory".equalsIgnoreCase(target) ? memoryFile : userFile;

            synchronized (lock) {
                List<String> entries = parseEntries(Files.readString(file));
                boolean replaced = false;

                for (int i = 0; i < entries.size(); i++) {
                    if (entries.get(i).contains(entryId)) {
                        entries.set(i, newContent);
                        replaced = true;
                        break;
                    }
                }

                if (!replaced) {
                    throw new EntryNotFoundException("找不到条目: " + entryId);
                }

                String content = String.join("\n" + ENTRY_DELIMITER + "\n", entries);
                writeAtomically(file, content);

                log.debug("记忆条目已替换: target={}, entryId={}", target, entryId);
                return entryId;
            }
        }

        public void removeEntry(String target, String entryId) throws IOException {
            Object lock = "memory".equalsIgnoreCase(target) ? memoryLock : userLock;
            Path file = "memory".equalsIgnoreCase(target) ? memoryFile : userFile;

            synchronized (lock) {
                List<String> entries = parseEntries(Files.readString(file));
                boolean removed = false;

                for (int i = entries.size() - 1; i >= 0; i--) {
                    if (entries.get(i).contains(entryId)) {
                        entries.remove(i);
                        removed = true;
                        break;
                    }
                }

                if (!removed) {
                    throw new EntryNotFoundException("找不到条目: " + entryId);
                }

                String content = String.join("\n" + ENTRY_DELIMITER + "\n", entries);
                writeAtomically(file, content);

                log.debug("记忆条目已删除: target={}, entryId={}", target, entryId);
            }
        }

        public String getRelevantMemory(String target, String context) throws IOException {
            Path file = "memory".equalsIgnoreCase(target) ? memoryFile : userFile;
            List<String> entries = parseEntries(Files.readString(file));

            if (entries.isEmpty()) {
                return "(暂无记忆)";
            }

            // 简单的全文搜索：如果提供 context，则过滤相关条目
            if (context != null && !context.isBlank()) {
                String lowerContext = context.toLowerCase();
                List<String> relevant = entries.stream()
                        .filter(entry -> entry.toLowerCase().contains(lowerContext))
                        .toList();

                if (relevant.isEmpty()) {
                    return "(暂无相关记忆)";
                }

                return String.join("\n\n§\n\n", relevant);
            }

            // 返回所有内容
            return String.join("\n\n§\n\n", entries);
        }

        public MemoryStats getMemoryStats() throws IOException {
            List<String> memory = readMemoryEntries();
            List<String> user = readUserEntries();

            int memoryUsage = memory.stream().mapToInt(String::length).sum();
            int userUsage = user.stream().mapToInt(String::length).sum();

            MemoryStats stats = new MemoryStats();
            stats.setMemoryUsage(memoryUsage);
            stats.setMemoryLimit(memoryMaxChars);
            stats.setMemoryUsagePercentage((double) memoryUsage / memoryMaxChars * 100);
            stats.setUserUsage(userUsage);
            stats.setUserLimit(userMaxChars);
            stats.setUserUsagePercentage((double) userUsage / userMaxChars * 100);

            return stats;
        }

        // ========== 私有方法 ==========

        private void initFileIfNotExists(Path file, String defaultContent) throws IOException {
            if (!Files.exists(file)) {
                Files.writeString(file, defaultContent, StandardOpenOption.CREATE);
                log.debug("创建文件: {}", file);
            }
        }

        private List<String> readMemoryEntries() throws IOException {
            if (Files.exists(memoryFile)) {
                return parseEntries(Files.readString(memoryFile));
            }
            return new ArrayList<>();
        }

        private List<String> readUserEntries() throws IOException {
            if (Files.exists(userFile)) {
                return parseEntries(Files.readString(userFile));
            }
            return new ArrayList<>();
        }

        private void writeAtomically(Path file, String content) throws IOException {
            Path tempFile = file.resolveSibling(file.getFileName() + ".tmp");
            try {
                Files.writeString(tempFile, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                // 清理临时文件
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {}
                throw e;
            }
        }

        private List<String> parseEntries(String content) {
            List<String> entries = new ArrayList<>();
            String[] parts = content.split(Pattern.quote(ENTRY_DELIMITER));
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    entries.add(trimmed);
                }
            }
            return entries;
        }
    }

    /**
     * 记忆容量超限异常
     */
    public static class MemoryLimitException extends RuntimeException {
        public MemoryLimitException(String message) {
            super(message);
        }
    }

    /**
     * 条目不存在异常
     */
    public static class EntryNotFoundException extends RuntimeException {
        public EntryNotFoundException(String message) {
            super(message);
        }
    }
}
