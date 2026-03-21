-- ============================================================
-- V3: LLM 配置管理表
-- 支持用户自定义 API Key / BaseURL / 模型名称（两级配置机制）
-- ============================================================

CREATE TABLE IF NOT EXISTS sys_llm_config
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id     BIGINT       NOT NULL DEFAULT 0 COMMENT '用户ID（0=系统默认，>0=用户自定义）',
    config_name VARCHAR(64)  NOT NULL COMMENT '配置名称（如：通义千问、DeepSeek官方）',
    llm_mode    VARCHAR(16)  NOT NULL COMMENT 'LLM模式：online/offline',
    base_url    VARCHAR(256)          COMMENT '自定义 API BaseURL（在线模式）',
    api_key     VARCHAR(512)          COMMENT '加密存储的 API Key（在线模式）',
    chat_model  VARCHAR(128)          COMMENT '对话模型名称（ChatService使用）',
    llm_model   VARCHAR(128)          COMMENT '分析模型名称（LlmService使用）',
    embed_model VARCHAR(128)          COMMENT '向量嵌入模型名称（VectorService使用）',
    ollama_url  VARCHAR(256)          COMMENT 'Ollama 服务地址（离线模式）',
    is_active   TINYINT      NOT NULL DEFAULT 0 COMMENT '是否当前启用（0=禁用，1=启用）',
    remark      VARCHAR(256)          COMMENT '备注说明',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除（0=正常，1=已删除）',

    UNIQUE KEY uk_user_active (user_id, is_active, deleted),
    INDEX idx_user_id (user_id),
    INDEX idx_llm_mode (llm_mode)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = 'LLM配置管理表';

-- 插入系统默认离线配置（user_id=0 表示系统级配置）
INSERT INTO sys_llm_config (user_id, config_name, llm_mode, chat_model, llm_model, embed_model,
                             ollama_url, is_active, remark)
VALUES (0, 'Ollama 默认配置', 'offline', 'deepseek-r1:7b', 'qwen2.5:7b', 'bge-m3',
        'http://localhost:11434', 1, '系统默认离线配置，使用 Ollama 三模型分工');

-- 插入系统默认在线配置示例（未启用）
INSERT INTO sys_llm_config (user_id, config_name, llm_mode, base_url, api_key, chat_model,
                             llm_model, embed_model, is_active, remark)
VALUES (0, '通义千问（系统预设）', 'online',
        'https://dashscope.aliyuncs.com/compatible-mode', '', 'qwen-max', 'qwen-plus',
        'text-embedding-v3', 0, '系统预设在线配置，需填写 ONLINE_API_KEY');
