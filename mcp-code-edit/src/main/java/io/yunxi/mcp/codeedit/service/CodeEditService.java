package io.yunxi.mcp.codeedit.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Code editing service
 * <p>
 * Provides code file editing capability via string replacement.
 * Used for AI code fixes, batch refactoring, etc.
 * </p>
 *
 * <h3>Features</h3>
 * <ul>
 * <li>Edit - Replace code content</li>
 * <li>Backup - Automatic file backup</li>
 * <li>Restore - Restore from backup</li>
 * <li>UTF-8 - UTF-8 encoding</li>
 * <li>Verify - Verify before modifying</li>
 * </ul>
 *
 * <h3>Configuration</h3>
 * <ul>
 * <li>code.edit.root-path - Code root path, default: ./code</li>
 * <li>code.edit.backup-enabled - Enable backup, default: true</li>
 * <li>code.edit.backup-path - Backup path, default: ./backups</li>
 * </ul>
 *
 * @author yunxi-mcp-servers
 * @see EditResult
 * @see BackupMetadata
 */
@Service
public class CodeEditService {

    private static final Logger log = LoggerFactory.getLogger(CodeEditService.class);

    private final String rootPath;
    private final boolean backupEnabled;
    private final String backupPath;
    private final AtomicInteger editCount = new AtomicInteger(0);

    // Backup metadata (in-memory)
    private final Map<String, BackupMetadata> backupMetadata = new ConcurrentHashMap<>();

    /**
     * Constructor
     *
     * @param rootPath      Code root path
     * @param backupEnabled Enable backup
     * @param backupPath    Backup path
     */
    public CodeEditService(
            @Value("${code.edit.root-path:./code}") String rootPath,
            @Value("${code.edit.backup-enabled:true}") boolean backupEnabled,
            @Value("${code.edit.backup-path:./backups}") String backupPath) {
        this.rootPath = rootPath;
        this.backupEnabled = backupEnabled;
        this.backupPath = backupPath;

        // Create backup directory
        if (backupEnabled) {
            try {
                Path backupDir = Paths.get(backupPath);
                if (!Files.exists(backupDir)) {
                    Files.createDirectories(backupDir);
                    log.info("Backup directory: {}", backupDir.toAbsolutePath());
                }
            } catch (IOException e) {
                log.warn("Create backup dir: {}", backupPath, e);
            }
        }
    }

    /**
     * Edit file
     * <p>
     * Replace old content with new content
     * </p>
     * <ol>
     * <li>Read file</li>
     * <li>Verify old content exists</li>
     * <li>Create backup</li>
     * <li>Apply modification</li>
     * <li>Save file</li>
     * <li>Return result</li>
     * </ol>
     *
     * @param relativePath Relative file path
     * @param oldContent   Old content to replace
     * @param newContent   New content
     * @return Edit result with ID
     * @throws IOException if error occurs
     */
    public EditResult editFile(String relativePath, String oldContent, String newContent) throws IOException {
        Path filePath = Paths.get(rootPath, relativePath);

        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("File not found: " + relativePath);
        }

        // Read content
        String currentContent = Files.readString(filePath, StandardCharsets.UTF_8);

        // Verify old content exists
        if (!currentContent.contains(oldContent)) {
            return new EditResult(false, "Old content not found in file", null);
        }

        // Create backup
        String backupFile = null;
        String editId = null;
        if (backupEnabled) {
            editId = "edit_" + editCount.incrementAndGet();
            backupFile = createBackup(filePath, currentContent, editId);
            // Save metadata
            backupMetadata.put(editId, new BackupMetadata(editId, relativePath, backupFile, LocalDateTime.now()));
        }

        // Apply modification
        String updatedContent = currentContent.replace(oldContent, newContent);

        // Save UTF-8
        Files.writeString(filePath, updatedContent, StandardCharsets.UTF_8);

        log.info("Edit file: {} ({})", relativePath, editId);

        return new EditResult(true,
                "Edit success" + (backupFile != null ? ", backup: " + backupFile : ""),
                editId);
    }

    /**
     * Create backup
     *
     * @param filePath Original file path
     * @param content  File content
     * @param editId   Edit ID
     * @return Backup file path
     * @throws IOException if error occurs
     */
    private String createBackup(Path filePath, String content, String editId) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = filePath.getFileName().toString();
        String backupFileName = editId + "_" + fileName + ".bak";

        Path backupFilePath = Paths.get(backupPath, backupFileName);
        Files.writeString(backupFilePath, content, StandardCharsets.UTF_8);

        log.debug("Backup: {}", backupFilePath);
        return backupFilePath.toString();
    }

    /**
     * Restore from backup
     * <p>
     * Restore file using edit ID
     * </p>
     *
     * @param editId Edit ID
     * @return Success status
     */
    public boolean restoreBackup(String editId) {
        BackupMetadata metadata = backupMetadata.get(editId);
        if (metadata == null) {
            log.warn("Edit ID not found: {}", editId);
            return false;
        }

        try {
            Path backupFile = Paths.get(metadata.backupPath());
            if (!Files.exists(backupFile)) {
                log.error("Backup file not found: {}", metadata.backupPath());
                return false;
            }

            // Read backup
            String backupContent = Files.readString(backupFile, StandardCharsets.UTF_8);

            // Restore
            Path originalFile = Paths.get(rootPath, metadata.relativePath());
            Files.writeString(originalFile, backupContent, StandardCharsets.UTF_8);

            log.info("Restore: {} -> {}", editId, metadata.relativePath());
            return true;

        } catch (IOException e) {
            log.error("Restore failed: {}", editId, e);
            return false;
        }
    }

    /**
     * List backups
     *
     * @return Backup list
     */
    public List<BackupMetadata> listBackups() {
        return new ArrayList<>(backupMetadata.values());
    }

    /**
     * Get backup metadata
     *
     * @param editId Edit ID
     * @return Backup metadata or null
     */
    public BackupMetadata getBackupMetadata(String editId) {
        return backupMetadata.get(editId);
    }

    /**
     * Add auto comment
     * <p>
     * Add modification reason as comment before content
     * </p>
     *
     * @param content File content
     * @param reason  Modification reason
     * @return Content with comment
     */
    public String addAutoComment(String content, String reason) {
        String[] lines = content.split("\n");
        StringBuilder sb = new StringBuilder();

        String commentPrefix = "// ";
        sb.append(commentPrefix).append("=====================================================\n");
        sb.append(commentPrefix).append("MCP Edit - ").append(LocalDateTime.now()).append("\n");
        sb.append(commentPrefix).append("Reason: ").append(reason).append("\n");
        sb.append(commentPrefix).append("=====================================================\n");

        for (String line : lines) {
            sb.append(line).append("\n");
        }

        return sb.toString();
    }

    /**
     * Backup metadata record
     *
     * @param editId       Edit ID
     * @param relativePath Original relative path
     * @param backupPath   Backup file path
     * @param createTime   Create time
     */
    public record BackupMetadata(String editId, String relativePath, String backupPath, LocalDateTime createTime) {
    }

    /**
     * Edit result
     *
     * @param success Success status
     * @param message Message
     * @param editId  Edit ID (for restore)
     */
    public record EditResult(boolean success, String message, String editId) {
    }
}