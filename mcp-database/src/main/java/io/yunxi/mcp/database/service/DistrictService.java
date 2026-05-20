package io.yunxi.mcp.database.service;

import io.yunxi.mcp.database.entity.DistrictInfo;
import io.yunxi.mcp.database.entity.DistrictDb;
import io.yunxi.mcp.database.entity.DistrictCode;
import io.yunxi.mcp.database.entity.KnowledgeBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 区县配置中心服务
 * <p>
 * 提供区县信息、数据库配置、代码版本和知识库的管理功能。
 * 支持增删改查操作，并维护内存缓存以提高查询性能。
 * </p>
 *
 * @author yunxi
 * @version 1.0.0
 * @since 1.0.0
 */
@Service
public class DistrictService {

    private static final Logger log = LoggerFactory.getLogger(DistrictService.class);

    /**
     * 数据源
     */
    private final DataSource dataSource;

    // 内存缓存
    /**
     * 区县信息缓存
     */
    private final Map<String, List<DistrictInfo>> cache = new ConcurrentHashMap<>();

    public DistrictService(DataSource dataSource) {
        this.dataSource = dataSource;
        initTables();
    }

    private void initTables() {
        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement()) {

            // 区县信息表
            stmt.execute("CREATE TABLE IF NOT EXISTS district_info (" +
                    "id VARCHAR(32) PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "province VARCHAR(50), " +
                    "contact VARCHAR(100), " +
                    "status VARCHAR(20) DEFAULT 'active', " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            // 数据库配置表
            stmt.execute("CREATE TABLE IF NOT EXISTS district_db (" +
                    "id VARCHAR(32) PRIMARY KEY, " +
                    "district_id VARCHAR(32), " +
                    "db_type VARCHAR(50), " +
                    "host VARCHAR(100), " +
                    "port INT DEFAULT 3306, " +
                    "database_name VARCHAR(100), " +
                    "username VARCHAR(100), " +
                    "password VARCHAR(255), " +
                    "description VARCHAR(255), " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            // 代码版本表
            stmt.execute("CREATE TABLE IF NOT EXISTS district_code (" +
                    "id VARCHAR(32) PRIMARY KEY, " +
                    "district_id VARCHAR(32), " +
                    "git_branch VARCHAR(100), " +
                    "git_commit VARCHAR(100), " +
                    "git_version VARCHAR(100), " +
                    "code_path VARCHAR(255), " +
                    "is_default BOOLEAN DEFAULT FALSE, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            // 知识库表
            stmt.execute("CREATE TABLE IF NOT EXISTS knowledge_base (" +
                    "id VARCHAR(32) PRIMARY KEY, " +
                    "district_id VARCHAR(32), " +
                    "title VARCHAR(255), " +
                    "content TEXT, " +
                    "type VARCHAR(50), " +
                    "tags VARCHAR(255), " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            log.info("初始化配置中心表结构完成");

            // 插入示例数据
            initSampleData(conn);

        } catch (Exception e) {
            log.warn("初始化表失败: {}", e.getMessage());
        }
    }

    private void initSampleData(Connection conn) throws SQLException {
        // 检查是否已有数据
        ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM district_info");
        rs.next();
        if (rs.getInt(1) > 0) {
            return;
        }

        // 插入示例区县
        conn.createStatement().execute("INSERT INTO district_info VALUES " +
                "('district_001', '北京市朝阳区', '北京', '张三', 'active', NOW()), " +
                "('district_002', '上海市浦东新区', '上海', '李四', 'active', NOW()), " +
                "('district_003', '深圳市南山区', '广东', '王五', 'active', NOW())");

        // 插入示例数据库
        conn.createStatement().execute("INSERT INTO district_db VALUES " +
                "('db_001', 'district_001', 'nutrition', '192.168.10.153', 3306, 'nutrition_zhaoxian', 'root', '', '营养数据库', NOW()), "
                +
                "('db_002', 'district_001', 'safety', '192.168.10.154', 3306, 'safety_db', 'root', '', '食安数据库', NOW()), "
                +
                "('db_003', 'district_002', 'nutrition', '192.168.20.153', 3306, 'nutrition_pudong', 'root', '', '营养数据库浦东', NOW())");

        // 插入示例代码版本
        conn.createStatement().execute("INSERT INTO district_code VALUES " +
                "('code_001', 'district_001', 'main', 'abc123def', 'v1.0.0', '/data/code', TRUE, NOW()), " +
                "('code_002', 'district_002', 'main', 'def456ghi', 'v1.0.1', '/data/code', TRUE, NOW())");

        // 插入示例知识库
        conn.createStatement().execute("INSERT INTO knowledge_base VALUES " +
                "('kb_001', NULL, '代码架构说明', '本系统采用微服务架构，包含以下模块:agent-core(核心框架),agent-desktop(桌面客户端),mcp-database(数据库服务)', 'architecture', '架构,微服务', NOW()), "
                +
                "('kb_002', NULL, '数据库表命名规范', '表名采用小写字母加下划线的方式,如:district_info,district_db。字段名统一采用驼峰命名。', 'business', '规范,数据库', NOW()), "
                +
                "('kb_003', 'district_001', '食谱上传失败处理', '如果食谱上传失败，请检查:1.网络连接 2.数据库是否正常 3.文件大小是否超限', 'issue', '食谱,上传失败', NOW())");

        log.info("插入示例数据完成");
    }

    // ===== 区县管理 =====

    public List<DistrictInfo> getAllDistricts() {
        List<DistrictInfo> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
                ResultSet rs = conn.createStatement().executeQuery(
                        "SELECT * FROM district_info WHERE status = 'active' ORDER BY name")) {
            while (rs.next()) {
                DistrictInfo d = new DistrictInfo();
                d.setId(rs.getString("id"));
                d.setName(rs.getString("name"));
                d.setProvince(rs.getString("province"));
                d.setContact(rs.getString("contact"));
                d.setStatus(rs.getString("status"));
                d.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                list.add(d);
            }
        } catch (Exception e) {
            log.error("获取区县列表失败: {}", e.getMessage());
        }
        return list;
    }

    public DistrictInfo getDistrict(String id) {
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT * FROM district_info WHERE id = ?")) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                DistrictInfo d = new DistrictInfo();
                d.setId(rs.getString("id"));
                d.setName(rs.getString("name"));
                d.setProvince(rs.getString("province"));
                d.setContact(rs.getString("contact"));
                d.setStatus(rs.getString("status"));
                d.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                return d;
            }
        } catch (Exception e) {
            log.error("获取区县失败: {}", e.getMessage());
        }
        return null;
    }

    public DistrictInfo addDistrict(DistrictInfo district) {
        if (district.getId() == null || district.getId().isEmpty()) {
            district.setId("district_" + System.currentTimeMillis());
        }
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO district_info (id, name, province, contact, status) VALUES (?, ?, ?, ?, ?)")) {
            ps.setString(1, district.getId());
            ps.setString(2, district.getName());
            ps.setString(3, district.getProvince());
            ps.setString(4, district.getContact());
            ps.setString(5, district.getStatus() != null ? district.getStatus() : "active");
            ps.executeUpdate();
            return district;
        } catch (Exception e) {
            log.error("添加区县失败: {}", e.getMessage());
            return null;
        }
    }

    public boolean deleteDistrict(String id) {
        try (Connection conn = dataSource.getConnection()) {
            conn.createStatement().execute("DELETE FROM district_db WHERE district_id = '" + id + "'");
            conn.createStatement().execute("DELETE FROM district_code WHERE district_id = '" + id + "'");
            conn.createStatement().execute("DELETE FROM knowledge_base WHERE district_id = '" + id + "'");
            conn.createStatement().execute("DELETE FROM district_info WHERE id = '" + id + "'");
            return true;
        } catch (Exception e) {
            log.error("删除区县失败: {}", e.getMessage());
            return false;
        }
    }

    // ===== 数据库配置 =====

    public List<DistrictDb> getDatabases(String districtId) {
        List<DistrictDb> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT * FROM district_db WHERE district_id = ?")) {
            ps.setString(1, districtId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                DistrictDb db = new DistrictDb();
                db.setId(rs.getString("id"));
                db.setDistrictId(rs.getString("district_id"));
                db.setDbType(rs.getString("db_type"));
                db.setHost(rs.getString("host"));
                db.setPort(rs.getInt("port"));
                db.setDatabaseName(rs.getString("database_name"));
                db.setUsername(rs.getString("username"));
                db.setPassword(rs.getString("password"));
                db.setDescription(rs.getString("description"));
                db.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                list.add(db);
            }
        } catch (Exception e) {
            log.error("获取数据库列表失败: {}", e.getMessage());
        }
        return list;
    }

    public DistrictDb addDatabase(DistrictDb db) {
        if (db.getId() == null || db.getId().isEmpty()) {
            db.setId("db_" + System.currentTimeMillis());
        }
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO district_db (id, district_id, db_type, host, port, database_name, username, password, description) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, db.getId());
            ps.setString(2, db.getDistrictId());
            ps.setString(3, db.getDbType());
            ps.setString(4, db.getHost());
            ps.setInt(5, db.getPort() != null ? db.getPort() : 3306);
            ps.setString(6, db.getDatabaseName());
            ps.setString(7, db.getUsername());
            ps.setString(8, db.getPassword());
            ps.setString(9, db.getDescription());
            ps.executeUpdate();
            return db;
        } catch (Exception e) {
            log.error("添加数据库失败: {}", e.getMessage());
            return null;
        }
    }

    public boolean deleteDatabase(String id) {
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement("DELETE FROM district_db WHERE id = ?")) {
            ps.setString(1, id);
            ps.executeUpdate();
            return true;
        } catch (Exception e) {
            log.error("删除数据库失败: {}", e.getMessage());
            return false;
        }
    }

    // ===== 代码版本 =====

    public DistrictCode getCodeVersion(String districtId) {
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT * FROM district_code WHERE district_id = ? AND is_default = TRUE")) {
            ps.setString(1, districtId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                DistrictCode code = new DistrictCode();
                code.setId(rs.getString("id"));
                code.setDistrictId(rs.getString("district_id"));
                code.setGitBranch(rs.getString("git_branch"));
                code.setGitCommit(rs.getString("git_commit"));
                code.setGitVersion(rs.getString("git_version"));
                code.setCodePath(rs.getString("code_path"));
                code.setIsDefault(rs.getBoolean("is_default"));
                code.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                return code;
            }
        } catch (Exception e) {
            log.error("获取代码版本失败: {}", e.getMessage());
        }
        return null;
    }

    public DistrictCode addCodeVersion(DistrictCode code) {
        if (code.getId() == null || code.getId().isEmpty()) {
            code.setId("code_" + System.currentTimeMillis());
        }
        // 如果设为默认，先取消其他的默认
        if (Boolean.TRUE.equals(code.getIsDefault())) {
            try (Connection conn = dataSource.getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                            "UPDATE district_code SET is_default = FALSE WHERE district_id = ?")) {
                ps.setString(1, code.getDistrictId());
                ps.executeUpdate();
            } catch (Exception e) {
                log.warn("取消默认版本失败: {}", e.getMessage());
            }
        }
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO district_code (id, district_id, git_branch, git_commit, git_version, code_path, is_default) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, code.getId());
            ps.setString(2, code.getDistrictId());
            ps.setString(3, code.getGitBranch());
            ps.setString(4, code.getGitCommit());
            ps.setString(5, code.getGitVersion());
            ps.setString(6, code.getCodePath());
            ps.setBoolean(7, code.getIsDefault() != null ? code.getIsDefault() : false);
            ps.executeUpdate();
            return code;
        } catch (Exception e) {
            log.error("添加代码版本失败: {}", e.getMessage());
            return null;
        }
    }

    // ===== 知识库 =====

    public List<KnowledgeBase> searchKnowledge(String districtId, String type, String keyword) {
        List<KnowledgeBase> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM knowledge_base WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (districtId != null && !districtId.isEmpty()) {
            sql.append(" AND (district_id = ? OR district_id IS NULL)");
            params.add(districtId);
        }
        if (type != null && !type.isEmpty()) {
            sql.append(" AND type = ?");
            params.add(type);
        }
        if (keyword != null && !keyword.isEmpty()) {
            sql.append(" AND (title LIKE ? OR content LIKE ?)");
            params.add("%" + keyword + "%");
            params.add("%" + keyword + "%");
        }
        sql.append(" ORDER BY created_at DESC LIMIT 50");

        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                KnowledgeBase kb = new KnowledgeBase();
                kb.setId(rs.getString("id"));
                kb.setDistrictId(rs.getString("district_id"));
                kb.setTitle(rs.getString("title"));
                kb.setContent(rs.getString("content"));
                kb.setType(rs.getString("type"));
                kb.setTags(rs.getString("tags"));
                kb.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                list.add(kb);
            }
        } catch (Exception e) {
            log.error("搜索知识库失败: {}", e.getMessage());
        }
        return list;
    }

    public KnowledgeBase addKnowledge(KnowledgeBase kb) {
        if (kb.getId() == null || kb.getId().isEmpty()) {
            kb.setId("kb_" + System.currentTimeMillis());
        }
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO knowledge_base (id, district_id, title, content, type, tags) VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, kb.getId());
            ps.setString(2, kb.getDistrictId());
            ps.setString(3, kb.getTitle());
            ps.setString(4, kb.getContent());
            ps.setString(5, kb.getType());
            ps.setString(6, kb.getTags());
            ps.executeUpdate();
            return kb;
        } catch (Exception e) {
            log.error("添加知识失败: {}", e.getMessage());
            return null;
        }
    }
}
