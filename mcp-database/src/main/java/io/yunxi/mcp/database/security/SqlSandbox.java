package io.yunxi.mcp.database.security;

import io.yunxi.mcp.database.config.DatabaseConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL 沙箱安全校验器
 * <p>
 * 提供多层次的 SQL 安全校验：
 * <ul>
 *   <li>SQL 类型白名单（仅允许 SELECT/SHOW/DESCRIBE/EXPLAIN）</li>
 *   <li>表名访问控制（允许列表/禁止列表）</li>
 *   <li>系统表访问控制</li>
 *   <li>SQL 注入检测</li>
 * </ul>
 * </p>
 *
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public class SqlSandbox {

    /**
     * 允许的 SQL 类型（大写）
     */
    private static final Set<String> ALLOWED_SQL_TYPES = Set.of(
            "SELECT", "SHOW", "DESCRIBE", "DESC", "EXPLAIN"
    );

    /**
     * 危险的 SQL 关键字（用于注入检测）
     */
    private static final Set<String> DANGEROUS_KEYWORDS = Set.of(
            "INSERT", "UPDATE", "DELETE", "DROP", "TRUNCATE", "CREATE",
            "ALTER", "GRANT", "REVOKE", "EXEC", "EXECUTE", "UNION",
            "INTO", "LOAD_FILE", "OUTFILE", "DUMPFILE"
    );

    /**
     * 系统表前缀（MySQL）
     */
    private static final Set<String> SYSTEM_TABLE_PREFIXES = Set.of(
            "mysql.", "information_schema.", "performance_schema.", "sys."
    );

    /**
     * 从 SQL 中提取表名的正则表达式
     */
    private static final Pattern TABLE_PATTERN = Pattern.compile(
            "FROM\\s+(`?)(\\w+)\\1",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern JOIN_PATTERN = Pattern.compile(
            "JOIN\\s+(`?)(\\w+)\\1",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 校验 SQL 是否安全
     *
     * @param sql           SQL 语句
     * @param databaseConfig 数据库配置
     * @return 校验结果
     */
    public static ValidationResult validate(String sql, DatabaseConfig databaseConfig) {
        if (sql == null || sql.isBlank()) {
            return ValidationResult.failure("SQL 语句不能为空");
        }

        // 1. 校验 SQL 类型
        ValidationResult typeResult = validateSqlType(sql);
        if (!typeResult.isValid()) {
            return typeResult;
        }

        // 2. 检测 SQL 注入
        ValidationResult injectionResult = checkSqlInjection(sql);
        if (!injectionResult.isValid()) {
            return injectionResult;
        }

        // 3. 校验表名访问权限
        ValidationResult tableResult = validateTableAccess(sql, databaseConfig);
        if (!tableResult.isValid()) {
            return tableResult;
        }

        return ValidationResult.success();
    }

    /**
     * 校验 SQL 类型是否在白名单中
     */
    private static ValidationResult validateSqlType(String sql) {
        String trimmedSql = sql.trim().toUpperCase();

        // 提取第一个单词作为 SQL 类型
        String sqlType = trimmedSql.split("\\s+")[0];

        if (!ALLOWED_SQL_TYPES.contains(sqlType)) {
            return ValidationResult.failure(
                    "不支持的 SQL 类型: " + sqlType + "。仅允许: " + ALLOWED_SQL_TYPES
            );
        }

        return ValidationResult.success();
    }

    /**
     * 检测 SQL 注入攻击
     */
    private static ValidationResult checkSqlInjection(String sql) {
        String upperSql = sql.toUpperCase();

        // 检测多语句（分号）
        if (sql.contains(";")) {
            // 检查分号是否在字符串中
            if (!isSemicolonInString(sql)) {
                return ValidationResult.failure("SQL 包含多个语句，可能存在注入风险");
            }
        }

        // 检测注释（可能的注入绕过）
        if (upperSql.contains("--") || upperSql.contains("/*") || upperSql.contains("*/")) {
            return ValidationResult.failure("SQL 包含注释，可能存在注入风险");
        }

        // 检测危险关键字
        for (String keyword : DANGEROUS_KEYWORDS) {
            // 使用单词边界匹配
            String pattern = "\\b" + keyword + "\\b";
            if (Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(upperSql).find()) {
                return ValidationResult.failure("SQL 包含危险关键字: " + keyword);
            }
        }

        return ValidationResult.success();
    }

    /**
     * 检查分号是否在字符串中
     */
    private static boolean isSemicolonInString(String sql) {
        boolean inString = false;
        char stringChar = 0;

        for (char c : sql.toCharArray()) {
            if (!inString && (c == '\'' || c == '"')) {
                inString = true;
                stringChar = c;
            } else if (inString && c == stringChar) {
                inString = false;
            } else if (!inString && c == ';') {
                return false;
            }
        }

        return true;
    }

    /**
     * 校验表名访问权限
     */
    private static ValidationResult validateTableAccess(String sql, DatabaseConfig config) {
        Set<String> tableNames = extractTableNames(sql);

        for (String tableName : tableNames) {
            // 检查系统表
            if (isSystemTable(tableName)) {
                if (!config.isAllowSystemTables()) {
                    return ValidationResult.failure("禁止访问系统表: " + tableName);
                }
                continue;
            }

            // 检查禁止列表
            List<String> deniedTables = config.getDeniedTables();
            if (deniedTables != null && !deniedTables.isEmpty()) {
                if (deniedTables.stream()
                        .anyMatch(t -> t.equalsIgnoreCase(tableName))) {
                    return ValidationResult.failure("表 '" + tableName + "' 在禁止访问列表中");
                }
            }

            // 检查允许列表（如果配置了）
            List<String> allowedTables = config.getAllowedTables();
            if (allowedTables != null && !allowedTables.isEmpty()) {
                boolean allowed = allowedTables.stream()
                        .anyMatch(t -> t.equalsIgnoreCase(tableName));
                if (!allowed) {
                    return ValidationResult.failure(
                            "表 '" + tableName + "' 不在允许访问列表中。允许访问的表: " + allowedTables
                    );
                }
            }
        }

        return ValidationResult.success();
    }

    /**
     * 从 SQL 中提取表名
     */
    private static Set<String> extractTableNames(String sql) {
        Set<String> tables = new HashSet<>();

        // 提取 FROM 子句中的表名
        Matcher fromMatcher = TABLE_PATTERN.matcher(sql);
        while (fromMatcher.find()) {
            tables.add(fromMatcher.group(2).toLowerCase());
        }

        // 提取 JOIN 子句中的表名
        Matcher joinMatcher = JOIN_PATTERN.matcher(sql);
        while (joinMatcher.find()) {
            tables.add(joinMatcher.group(2).toLowerCase());
        }

        return tables;
    }

    /**
     * 判断是否为系统表
     */
    private static boolean isSystemTable(String tableName) {
        String lowerTable = tableName.toLowerCase();
        return SYSTEM_TABLE_PREFIXES.stream()
                .anyMatch(lowerTable::startsWith);
    }

    /**
     * 校验结果
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        private ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult failure(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }
}
