package io.yunxi.mcp.text2sql.util;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * 只读 SQL 安全护栏（参照 mcp-database 的 SqlSandbox 思路，独立内置以保证本模块自包含）。
 * <p>仅允许 SELECT/SHOW/DESCRIBE/EXPLAIN；拦截写操作关键字、多语句与注释，降低注入风险。</p>
 */
public final class SqlGuard {

    private static final Set<String> ALLOWED = Set.of("SELECT", "SHOW", "DESCRIBE", "DESC", "EXPLAIN");
    private static final Set<String> DANGEROUS = Set.of(
            "INSERT", "UPDATE", "DELETE", "DROP", "TRUNCATE", "CREATE", "ALTER",
            "GRANT", "REVOKE", "EXEC", "EXECUTE", "MERGE", "REPLACE");

    private SqlGuard() {
    }

    public static Result validate(String sql) {
        if (sql == null || sql.isBlank()) {
            return Result.fail("SQL 不能为空");
        }
        String trimmed = sql.trim();
        String upperType = trimmed.split("\\s+")[0].toUpperCase();
        if (!ALLOWED.contains(upperType)) {
            return Result.fail("仅允许只读语句: " + ALLOWED);
        }
        String upper = sql.toUpperCase();
        if (upper.contains("--") || upper.contains("/*") || upper.contains("*/")) {
            return Result.fail("SQL 包含注释，疑似注入");
        }
        if (hasMultipleStatements(sql)) {
            return Result.fail("SQL 包含多条语句，疑似注入");
        }
        for (String kw : DANGEROUS) {
            if (Pattern.compile("\\b" + kw + "\\b", Pattern.CASE_INSENSITIVE).matcher(upper).find()) {
                return Result.fail("SQL 含危险关键字: " + kw);
            }
        }
        return Result.ok();
    }

    private static boolean hasMultipleStatements(String sql) {
        boolean inStr = false;
        char quote = 0;
        StringBuilder buf = new StringBuilder();
        for (char c : sql.toCharArray()) {
            if (!inStr && (c == '\'' || c == '"')) {
                inStr = true;
                quote = c;
                buf.append(c);
            } else if (inStr && c == quote) {
                inStr = false;
                buf.append(c);
            } else if (!inStr && c == ';') {
                buf.append('\n');
            } else {
                buf.append(c);
            }
        }
        int nonBlank = 0;
        for (String part : buf.toString().split("\n")) {
            if (!part.trim().isEmpty()) {
                nonBlank++;
            }
        }
        return nonBlank > 1;
    }

    public static class Result {
        private final boolean valid;
        private final String message;

        private Result(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public static Result ok() {
            return new Result(true, null);
        }

        public static Result fail(String message) {
            return new Result(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }
}
