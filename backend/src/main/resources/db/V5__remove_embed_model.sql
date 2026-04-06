-- ============================================================
-- V5: 从 sys_llm_config 表中移除 embed_model 列
-- 向量嵌入模型统一由 application.yml 中 patent.ollama.embed-model
-- 和 patent.online.embed-model 配置，不再存储到数据库。
-- 这确保了向量数据库（Qdrant）写入与检索的维度一致性。
-- ============================================================

-- 移除历史数据中的 embed_model 列（已迁移到 yaml 配置）
ALTER TABLE sys_llm_config DROP COLUMN IF EXISTS embed_model;
