package io.yunxi.mcp.text2sql.model;

import java.util.List;

/**
 * 数据库 schema 快照（内省结果），可被 Jackson 直接序列化。
 */
public record SchemaSnapshot(List<TableInfo> tables) {

    public record TableInfo(String name, List<ColumnInfo> columns) {
    }

    public record ColumnInfo(String name, String type, boolean nullable) {
    }
}
