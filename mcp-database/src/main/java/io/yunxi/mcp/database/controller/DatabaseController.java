package io.yunxi.mcp.database.controller;

import io.yunxi.mcp.database.config.DatabaseConfig;
import io.yunxi.mcp.database.config.DatabaseConfigService;
import io.yunxi.mcp.database.config.McpDatabaseProperties;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据库配置管理 API
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class DatabaseController {

    private final DatabaseConfigService configService;
    private final McpDatabaseProperties mcpDatabaseProperties;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    public DatabaseController(DatabaseConfigService configService,
                              McpDatabaseProperties mcpDatabaseProperties) {
        this.configService = configService;
        this.mcpDatabaseProperties = mcpDatabaseProperties;
    }

    /**
     * Spring 初始化后执行
     */
    @PostConstruct
    public void init() {
        // 1. 注册默认数据库（从 spring.datasource 读取）
        addDefaultDatabase();
        // 2. 注册多数据库（从 mcp-databases YAML 配置读取）
        registerDatabasesFromConfig();
    }

    private void addDefaultDatabase() {
        try {
            log.info("开始添加默认数据库，dbUrl: {}", dbUrl);
            if (dbUrl != null && !dbUrl.isBlank()) {
                DatabaseConfig config = new DatabaseConfig();
                config.setId("default");
                config.setName("默认数据库");

                String[] urlParts = dbUrl.replace("jdbc:mysql://", "").split("/");
                if (urlParts.length >= 2) {
                    String hostPort = urlParts[0];
                    String[] hostPortParts = hostPort.split(":");
                    if (hostPortParts.length >= 2) {
                        config.setHost(hostPortParts[0]);
                        config.setPort(Integer.parseInt(hostPortParts[1]));
                    }
                    config.setDatabase(urlParts[1]);
                }

                config.setUsername(username);
                config.setPassword(password);
                configService.addDatabase(config);
                log.info("添加默认数据库: {} -> {}", config.getId(), config.getName());
            }
        } catch (Exception e) {
            log.error("添加默认数据库失败", e);
        }
    }

    /**
     * 从 YAML 配置注册多数据库
     */
    private void registerDatabasesFromConfig() {
        Map<String, McpDatabaseProperties.DatabaseInfo> databases = mcpDatabaseProperties.getDatabases();
        if (databases == null || databases.isEmpty()) {
            log.info("未配置多数据库（mcp-databases），跳过");
            return;
        }

        for (Map.Entry<String, McpDatabaseProperties.DatabaseInfo> entry : databases.entrySet()) {
            String dbKey = entry.getKey();
            McpDatabaseProperties.DatabaseInfo dbInfo = entry.getValue();
            McpDatabaseProperties.JdbcConfig jdbc = dbInfo.getJdbc();

            if (jdbc == null || jdbc.getUrl() == null || jdbc.getUrl().isBlank()) {
                log.warn("数据库 {} 未配置 JDBC 连接信息，跳过", dbKey);
                continue;
            }

            try {
                DatabaseConfig config = new DatabaseConfig();
                config.setId(dbKey);
                config.setName(dbInfo.getDisplayName() != null ? dbInfo.getDisplayName() : dbKey);

                // 解析 JDBC URL: jdbc:mysql://host:port/database
                String jdbcUrl = jdbc.getUrl();
                String[] urlParts = jdbcUrl.replace("jdbc:mysql://", "").split("/");
                if (urlParts.length >= 2) {
                    String hostPort = urlParts[0];
                    String[] hostPortParts = hostPort.split(":");
                    if (hostPortParts.length >= 2) {
                        config.setHost(hostPortParts[0]);
                        config.setPort(Integer.parseInt(hostPortParts[1]));
                    }
                    config.setDatabase(urlParts[1]);
                }

                config.setUsername(jdbc.getUsername());
                config.setPassword(jdbc.getPassword());
                configService.addDatabase(config);
                log.info("注册数据库: {} -> {} ({})", dbKey, dbInfo.getDisplayName(), jdbc.getUrl());
            } catch (Exception e) {
                log.error("注册数据库 {} 失败: {}", dbKey, e.getMessage());
            }
        }
    }

    /**
     * 添加数据库
     */
    @PostMapping("/databases")
    public Map<String, Object> addDatabase(@RequestBody DatabaseConfig config) {
        try {
            DatabaseConfig saved = configService.addDatabase(config);
            return Map.of(
                    "status", "success",
                    "id", saved.getId(),
                    "name", saved.getName());
        } catch (Exception e) {
            log.error("添加数据库失败: {}", e.getMessage());
            return Map.of("status", "error", "message", e.getMessage());
        }
    }

    /**
     * 获取所有数据库
     */
    @GetMapping("/databases")
    public Map<String, Object> getDatabases() {
        Map<String, DatabaseConfig> configs = configService.getAllDatabases();
        List<Map<String, Object>> list = configs.entrySet().stream()
                .map(e -> {
                    Map<String, Object> db = new java.util.LinkedHashMap<>();
                    db.put("id", e.getKey());
                    db.put("name", e.getValue().getName());
                    db.put("host", e.getValue().getHost());
                    db.put("port", e.getValue().getPort());
                    db.put("database", e.getValue().getDatabase());
                    db.put("enabled", e.getValue().isEnabled());
                    return db;
                })
                .collect(Collectors.toList());
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("databases", list);
        return result;
    }

    /**
     * 删除数据库
     */
    @DeleteMapping("/databases/{id}")
    public Map<String, Object> deleteDatabase(@PathVariable String id) {
        boolean success = configService.removeDatabase(id);
        return Map.of("status", success ? "success" : "error");
    }

    /**
     * 测试数据库连接
     */
    @PostMapping("/databases/test")
    public Map<String, Object> testDatabase(@RequestBody DatabaseConfig config) {
        boolean success = configService.testConnection(config);
        return Map.of("status", success ? "success" : "error");
    }

    /**
     * 查询（指定数据库）
     */
    @PostMapping("/query")
    public Map<String, Object> query(@RequestBody QueryRequest request) {
        String dbId = request.getDbId();
        String sql = request.getSql();
        Integer limit = request.getLimit() != null ? request.getLimit() : 100;

        if (dbId == null || dbId.isEmpty()) {
            dbId = "default";
        }

        try {
            var dataSource = configService.getDataSource(dbId);
            if (dataSource == null) {
                return Map.of("status", "error", "message", "数据库不存在: " + dbId);
            }

            var conn = dataSource.getConnection();
            var stmt = conn.createStatement();
            stmt.setMaxRows(limit);
            var rs = stmt.executeQuery(sql);

            var rows = new java.util.ArrayList<Map<String, Object>>();
            var meta = rs.getMetaData();
            int colCount = meta.getColumnCount();

            while (rs.next()) {
                var row = new java.util.LinkedHashMap<String, Object>();
                for (int i = 1; i <= colCount; i++) {
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                }
                rows.add(row);
            }

            rs.close();
            conn.close();

            return Map.of(
                    "status", "success",
                    "dbId", dbId,
                    "rowCount", rows.size(),
                    "columns", getColumnNames(meta),
                    "rows", rows);
        } catch (Exception e) {
            log.error("查询失败: {}", e.getMessage());
            return Map.of("status", "error", "message", e.getMessage());
        }
    }

    private List<String> getColumnNames(java.sql.ResultSetMetaData meta) throws java.sql.SQLException {
        var names = new java.util.ArrayList<String>();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            names.add(meta.getColumnLabel(i));
        }
        return names;
    }

    @Data
    public static class QueryRequest {
        private String dbId;
        private String sql;
        private Integer limit;
    }
}