-- ============================================================
-- 专利技术匹配系统 - 数据库初始化脚本
-- 数据库: MySQL 8.0+
-- ============================================================

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS patent_match_system
DEFAULT CHARACTER SET utf8mb4 
DEFAULT COLLATE utf8mb4_unicode_ci;

USE patent_match_system;

-- ============================================================
-- 1. 系统用户表
-- ============================================================
DROP TABLE IF EXISTS match_record;
DROP TABLE IF EXISTS patent_favorite;
DROP TABLE IF EXISTS patent_vector;
DROP TABLE IF EXISTS patent_domain;
DROP TABLE IF EXISTS patent_entity;
DROP TABLE IF EXISTS patent;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '加密密码',
    nickname VARCHAR(50) COMMENT '昵称',
    role VARCHAR(32) NOT NULL DEFAULT 'user' COMMENT '角色：admin/user',
    status TINYINT DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- 创建管理员账号（密码: admin123，BCrypt加密）
INSERT INTO sys_user (username, password, nickname, role, status) VALUES 
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt3GWoa', '管理员', 'admin', 1);

-- ============================================================
-- 2. 专利基础信息表
-- ============================================================
CREATE TABLE patent (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '专利ID',
    publication_no VARCHAR(100) COMMENT '公开号/专利号',
    title VARCHAR(500) COMMENT '专利名称',
    applicant VARCHAR(255) COMMENT '申请人',
    publication_date DATE COMMENT '公开日期',
    `abstract` TEXT COMMENT '专利摘要',
    file_path VARCHAR(255) COMMENT 'MinIO文件路径',
    source_type VARCHAR(32) DEFAULT 'FILE' COMMENT '来源类型：FILE-PDF上传/TEXT-文本录入',
    parse_status VARCHAR(32) DEFAULT 'PENDING' COMMENT '解析状态：PENDING/PARSING/EXTRACTING/VECTORIZING/SUCCESS/FAILED',
    parse_error TEXT COMMENT '解析错误信息',
    created_by BIGINT COMMENT '创建用户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_publication_no (publication_no),
    INDEX idx_parse_status (parse_status),
    INDEX idx_created_by (created_by),
    INDEX idx_applicant (applicant),
    CONSTRAINT fk_patent_user FOREIGN KEY (created_by) REFERENCES sys_user(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='专利基础信息表';

-- ============================================================
-- 3. 专利实体表（LLM提取的技术实体）
-- ============================================================
CREATE TABLE patent_entity (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '实体ID',
    patent_id BIGINT NOT NULL COMMENT '专利ID',
    entity_name VARCHAR(255) NOT NULL COMMENT '实体名称',
    entity_type VARCHAR(64) NOT NULL COMMENT '实体类型：PRODUCT/METHOD/MATERIAL/COMPONENT/EFFECT/APPLICATION',
    importance VARCHAR(20) DEFAULT 'medium' COMMENT '重要性：high/medium/low',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_patent_id (patent_id),
    INDEX idx_entity_name (entity_name),
    INDEX idx_entity_type (entity_type),
    CONSTRAINT fk_entity_patent FOREIGN KEY (patent_id) REFERENCES patent(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='专利实体表';

-- ============================================================
-- 4. 专利技术领域表（IPC/CPC分类，支持层次化）
-- ============================================================
CREATE TABLE patent_domain (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '领域ID',
    patent_id BIGINT NOT NULL COMMENT '专利ID',
    domain_code VARCHAR(100) NOT NULL COMMENT '领域代码（如G06F16/30）',
    domain_level INT NOT NULL COMMENT '领域层级：1-部/2-大类/3-小类/4-主组/5-分组',
    domain_desc VARCHAR(500) COMMENT '领域描述',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_patent_id (patent_id),
    INDEX idx_domain_code (domain_code),
    INDEX idx_domain_level (domain_level),
    CONSTRAINT fk_domain_patent FOREIGN KEY (patent_id) REFERENCES patent(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='专利技术领域表';

-- ============================================================
-- 5. 专利向量映射表（与Qdrant的关联）
-- ============================================================
CREATE TABLE patent_vector (
    patent_id BIGINT PRIMARY KEY COMMENT '专利ID',
    vector_id VARCHAR(128) NOT NULL COMMENT 'Qdrant向量ID（UUID）',
    embedding_model VARCHAR(64) NOT NULL COMMENT '向量模型：text-embedding-v3/nomic-embed-text',
    vector_dim INT COMMENT '向量维度',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    CONSTRAINT fk_vector_patent FOREIGN KEY (patent_id) REFERENCES patent(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='专利向量映射表';

-- ============================================================
-- 6. 专利收藏表
-- ============================================================
CREATE TABLE patent_favorite (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '收藏ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    patent_id BIGINT NOT NULL COMMENT '专利ID',
    remark VARCHAR(500) COMMENT '收藏备注',
    group_name VARCHAR(100) COMMENT '收藏夹分组',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_patent (user_id, patent_id),
    INDEX idx_user_id (user_id),
    INDEX idx_patent_id (patent_id),
    INDEX idx_group_name (group_name),
    CONSTRAINT fk_favorite_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_favorite_patent FOREIGN KEY (patent_id) REFERENCES patent(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='专利收藏表';

-- ============================================================
-- 7. 匹配记录表（统一保存专利匹配和文本查询）
-- ============================================================
CREATE TABLE match_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '匹配记录ID',
    user_id BIGINT COMMENT '用户ID',
    
    -- 查询来源（二选一）
    match_mode VARCHAR(16) NOT NULL COMMENT '匹配模式：PATENT-专利匹配/TEXT-文本查询',
    source_patent_id BIGINT COMMENT '源专利ID（专利匹配时使用）',
    query_text TEXT COMMENT '查询文本（文本查询时使用）',
    
    -- 查询分析结果
    query_entities TEXT COMMENT '查询提取的实体JSON',
    query_domain VARCHAR(32) COMMENT '查询的技术领域',
    
    -- 匹配目标
    target_patent_id BIGINT NOT NULL COMMENT '目标专利ID',
    
    -- 匹配结果
    similarity_score DECIMAL(5,4) COMMENT '相似度评分（0.0000-1.0000）',
    match_type VARCHAR(32) COMMENT '匹配类型：VECTOR/KEYWORD/HYBRID',
    entity_match_count INT COMMENT '实体匹配数量',
    domain_match TINYINT COMMENT '领域是否匹配：1-是/0-否',
    match_reason TEXT COMMENT '匹配原因（LLM生成）',
    
    -- 时间戳
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    -- 索引
    INDEX idx_user_id (user_id),
    INDEX idx_match_mode (match_mode),
    INDEX idx_source_patent (source_patent_id),
    INDEX idx_target_patent (target_patent_id),
    INDEX idx_similarity (similarity_score),
    INDEX idx_created_at (created_at),
    
    -- 外键
    CONSTRAINT fk_match_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE SET NULL,
    CONSTRAINT fk_match_source FOREIGN KEY (source_patent_id) REFERENCES patent(id) ON DELETE CASCADE,
    CONSTRAINT fk_match_target FOREIGN KEY (target_patent_id) REFERENCES patent(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='匹配记录表';

-- ============================================================
-- 完成
-- ============================================================
SELECT '数据库初始化完成！' AS message;
