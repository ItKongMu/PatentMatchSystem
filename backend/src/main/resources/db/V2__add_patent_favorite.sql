-- ============================================================
-- 专利技术匹配系统 - 增量更新脚本 V2
-- 功能: 添加专利收藏表
-- ============================================================

USE patent_match_system;

-- 创建专利收藏表
CREATE TABLE IF NOT EXISTS patent_favorite (
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

-- 更新专利表source_type枚举，增加CSV来源类型
-- (注：如果已有数据，此操作会自动兼容)
ALTER TABLE patent MODIFY COLUMN source_type VARCHAR(32) DEFAULT 'FILE' COMMENT '来源类型：FILE-PDF上传/TEXT-文本录入/CSV-批量导入';

SELECT 'V2迁移完成：添加专利收藏表' AS message;
