-- V6: 为 match_record 添加 session_id 字段，支持同一次匹配的多条记录归组
ALTER TABLE match_record
    ADD COLUMN session_id VARCHAR(64) NULL COMMENT '匹配会话ID（同一次匹配请求共享）' AFTER user_id,
    ADD COLUMN source_patent_title VARCHAR(500) NULL COMMENT '源专利名称（专利匹配时冗余存储）' AFTER source_patent_id,
    ADD COLUMN target_patent_title VARCHAR(500) NULL COMMENT '目标专利名称（冗余存储，便于历史查询）' AFTER target_patent_id,
    ADD COLUMN top_k INT NULL COMMENT '本次匹配要求返回的数量' AFTER target_patent_title;

-- 添加索引
ALTER TABLE match_record
    ADD INDEX idx_session_id (session_id),
    ADD INDEX idx_user_session (user_id, session_id);
