-- 多区县配置中心表结构
-- 数据库: config_center

-- 区县信息表
CREATE TABLE IF NOT EXISTS district_info (
    id VARCHAR(32) PRIMARY KEY COMMENT '区县ID',
    name VARCHAR(100) NOT NULL COMMENT '区县名称',
    province VARCHAR(50) COMMENT '省份',
    contact VARCHAR(100) COMMENT '联系人',
    status VARCHAR(20) DEFAULT 'active' COMMENT '状态:active/inactive',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='区县信息表';

-- 数据库配置表
CREATE TABLE IF NOT EXISTS district_db (
    id VARCHAR(32) PRIMARY KEY COMMENT 'ID',
    district_id VARCHAR(32) COMMENT '区县ID',
    db_type VARCHAR(50) COMMENT '数据库类型:nutrition/safety/budget/collection/warning/strategy',
    host VARCHAR(100) COMMENT '数据库地址',
    port INT DEFAULT 3306 COMMENT '端口',
    database_name VARCHAR(100) COMMENT '库名',
    username VARCHAR(100) COMMENT '用户名',
    password VARCHAR(255) COMMENT '密码(加密)',
    description VARCHAR(255) COMMENT '描述',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (district_id) REFERENCES district_info(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据库配置表';

-- 代码版本表
CREATE TABLE IF NOT EXISTS district_code (
    id VARCHAR(32) PRIMARY KEY COMMENT 'ID',
    district_id VARCHAR(32) COMMENT '区县ID',
    git_branch VARCHAR(100) COMMENT '分支',
    git_commit VARCHAR(100) COMMENT 'commit hash',
    git_version VARCHAR(100) COMMENT '版本号',
    code_path VARCHAR(255) COMMENT '代码目录',
    is_default BOOLEAN DEFAULT FALSE COMMENT '是否默认版本',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (district_id) REFERENCES district_info(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码版本表';

-- 知识库表
CREATE TABLE IF NOT EXISTS knowledge_base (
    id VARCHAR(32) PRIMARY KEY COMMENT 'ID',
    district_id VARCHAR(32) COMMENT '区县ID(可空=通用)',
    title VARCHAR(255) COMMENT '标题',
    content TEXT COMMENT '内容',
    type VARCHAR(50) COMMENT '类型:architecture/business/issue/git_log',
    tags VARCHAR(255) COMMENT '标签,逗号分隔',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库表';

-- 初始化示例数据
INSERT INTO district_info (id, name, province, contact, status) VALUES
('district_001', '北京市朝阳区', '北京', '张三', 'active'),
('district_002', '上海市浦东新区', '上海', '李四', 'active'),
('district_003', '深圳市南山区', '广东', '王五', 'active');

INSERT INTO district_db (id, district_id, db_type, host, port, database_name, username, password, description) VALUES
('db_001', 'district_001', 'nutrition', '192.168.10.153', 3306, 'nutrition_zhaoxian', 'root', ' encrypted ', '营养数据库'),
('db_002', 'district_001', 'safety', '192.168.10.154', 3306, 'safety_db', 'root', ' encrypted ', '食安数据库'),
('db_003', 'district_002', 'nutrition', '192.168.20.153', 3306, 'nutrition_pudong', 'root', ' encrypted ', '营养数据库浦东'),
('db_004', 'district_003', 'nutrition', '192.168.30.153', 3306, 'nutrition_nanshan', 'root', ' encrypted ', '营养数据库南山');

INSERT INTO district_code (id, district_id, git_branch, git_commit, git_version, code_path, is_default) VALUES
('code_001', 'district_001', 'main', 'abc123', 'v1.0.0', '/data/code/nutrition', TRUE),
('code_002', 'district_002', 'main', 'def456', 'v1.0.1', '/data/code/nutrition', TRUE),
('code_003', 'district_003', 'develop', 'ghi789', 'v1.1.0', '/data/code/nutrition', TRUE);

INSERT INTO knowledge_base (id, district_id, title, content, type, tags) VALUES
('kb_001', NULL, '代码架构说明', '本系统采用微服务架构，包含以下模块:agent-core(核心框架),agent-desktop(桌面客户端),mcp-database(数据库服务)...', 'architecture', '架构,微服务'),
('kb_002', NULL, '数据库表命名规范', '表名采用小写字母加下划线的方式,如:district_info,district_db。字段名统一采用驼峰命名...', 'business', '规范,数据库'),
('kb_003', 'district_001', '食谱上传失败处理', '如果食谱上传失败，请检查:1.网络连接 2.数据库是否正常 3.文件大小是否超限', 'issue', '食谱,上传失败');
