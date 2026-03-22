-- ============================================================
-- V4: 用户 LLM 配置选择表
-- 解耦"系统配置定义"与"用户的激活选择"
-- 每个用户只有一条记录，记录其当前选择的配置ID
-- ============================================================

CREATE TABLE IF NOT EXISTS user_llm_selection
(
    user_id   BIGINT   NOT NULL COMMENT '用户ID（>0）',
    config_id BIGINT   NOT NULL COMMENT '当前选择的配置ID（sys_llm_config.id）',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (user_id),
    INDEX idx_config_id (config_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '用户LLM配置选择表（每用户一条，记录其激活的配置）';

-- 将 sys_llm_config 中用户自定义配置的激活状态迁移到新表
-- （系统配置 user_id=0 的 is_active 不再有多用户语义，保留作系统级默认标记）
INSERT INTO user_llm_selection (user_id, config_id)
SELECT user_id, id
FROM sys_llm_config
WHERE user_id > 0
  AND is_active = 1
  AND deleted = 0
ON DUPLICATE KEY UPDATE config_id = VALUES(config_id);
