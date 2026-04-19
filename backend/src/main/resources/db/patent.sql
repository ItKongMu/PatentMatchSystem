/*
 Navicat Premium Data Transfer

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 80043 (8.0.43)
 Source Host           : localhost:3306
 Source Schema         : patent_match_system

 Target Server Type    : MySQL
 Target Server Version : 80043 (8.0.43)
 File Encoding         : 65001

 Date: 18/04/2026 20:46:03
*/

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS patent_match_system
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_unicode_ci;

USE patent_match_system;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '加密密码',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '昵称',
  `role` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'user' COMMENT '角色：admin/user',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '系统用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for patent
-- ----------------------------
DROP TABLE IF EXISTS `patent`;
CREATE TABLE `patent`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '专利ID',
  `publication_no` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '公开号/专利号',
  `title` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '专利名称',
  `applicant` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '申请人',
  `publication_date` date NULL DEFAULT NULL COMMENT '公开日期',
  `abstract` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '专利摘要',
  `file_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'MinIO文件路径',
  `source_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'FILE' COMMENT '来源类型：FILE-PDF上传/TEXT-文本录入/CSV-批量导入',
  `parse_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'PENDING' COMMENT '解析状态：PENDING/PARSING/EXTRACTING/VECTORIZING/SUCCESS/FAILED',
  `parse_error` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '解析错误信息',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建用户ID',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_publication_no`(`publication_no` ASC) USING BTREE,
  INDEX `idx_parse_status`(`parse_status` ASC) USING BTREE,
  INDEX `idx_created_by`(`created_by` ASC) USING BTREE,
  INDEX `idx_applicant`(`applicant` ASC) USING BTREE,
  CONSTRAINT `fk_patent_user` FOREIGN KEY (`created_by`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 229 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '专利基础信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for match_record
-- ----------------------------
DROP TABLE IF EXISTS `match_record`;
CREATE TABLE `match_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '匹配记录ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `session_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '匹配会话ID（同一次匹配请求共享）',
  `match_mode` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '匹配模式：PATENT-专利匹配/TEXT-文本查询',
  `source_patent_id` bigint NULL DEFAULT NULL COMMENT '源专利ID（专利匹配时使用）',
  `source_patent_title` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '源专利名称（专利匹配时冗余存储）',
  `query_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '查询文本（文本查询时使用）',
  `query_entities` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '查询提取的实体JSON',
  `query_domain` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '查询的技术领域',
  `target_patent_id` bigint NOT NULL COMMENT '目标专利ID',
  `target_patent_title` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '目标专利名称（冗余存储，便于历史查询）',
  `top_k` int NULL DEFAULT NULL COMMENT '本次匹配要求返回的数量',
  `similarity_score` decimal(5, 4) NULL DEFAULT NULL COMMENT '相似度评分（0.0000-1.0000）',
  `match_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '匹配类型：VECTOR/KEYWORD/HYBRID',
  `entity_match_count` int NULL DEFAULT NULL COMMENT '实体匹配数量',
  `domain_match` tinyint NULL DEFAULT NULL COMMENT '领域是否匹配：1-是/0-否',
  `match_reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '匹配原因（LLM生成）',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_match_mode`(`match_mode` ASC) USING BTREE,
  INDEX `idx_source_patent`(`source_patent_id` ASC) USING BTREE,
  INDEX `idx_target_patent`(`target_patent_id` ASC) USING BTREE,
  INDEX `idx_similarity`(`similarity_score` ASC) USING BTREE,
  INDEX `idx_created_at`(`created_at` ASC) USING BTREE,
  INDEX `idx_session_id`(`session_id` ASC) USING BTREE,
  INDEX `idx_user_session`(`user_id` ASC, `session_id` ASC) USING BTREE,
  CONSTRAINT `fk_match_source` FOREIGN KEY (`source_patent_id`) REFERENCES `patent` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_match_target` FOREIGN KEY (`target_patent_id`) REFERENCES `patent` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_match_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 90 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '匹配记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for patent_domain
-- ----------------------------
DROP TABLE IF EXISTS `patent_domain`;
CREATE TABLE `patent_domain`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '领域ID',
  `patent_id` bigint NOT NULL COMMENT '专利ID',
  `domain_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '领域代码（如G06F16/30）',
  `domain_level` int NOT NULL COMMENT '领域层级：1-部/2-大类/3-小类/4-主组/5-分组',
  `domain_desc` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '领域描述',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_patent_id`(`patent_id` ASC) USING BTREE,
  INDEX `idx_domain_code`(`domain_code` ASC) USING BTREE,
  INDEX `idx_domain_level`(`domain_level` ASC) USING BTREE,
  CONSTRAINT `fk_domain_patent` FOREIGN KEY (`patent_id`) REFERENCES `patent` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 2448 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '专利技术领域表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for patent_entity
-- ----------------------------
DROP TABLE IF EXISTS `patent_entity`;
CREATE TABLE `patent_entity`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '实体ID',
  `patent_id` bigint NOT NULL COMMENT '专利ID',
  `entity_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '实体名称',
  `entity_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '实体类型：PRODUCT/METHOD/MATERIAL/COMPONENT/EFFECT/APPLICATION',
  `importance` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'medium' COMMENT '重要性：high/medium/low',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_patent_id`(`patent_id` ASC) USING BTREE,
  INDEX `idx_entity_name`(`entity_name` ASC) USING BTREE,
  INDEX `idx_entity_type`(`entity_type` ASC) USING BTREE,
  CONSTRAINT `fk_entity_patent` FOREIGN KEY (`patent_id`) REFERENCES `patent` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 7867 CHARACTER SET = utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT = '专利实体表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for patent_favorite
-- ----------------------------
DROP TABLE IF EXISTS `patent_favorite`;
CREATE TABLE `patent_favorite`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `patent_id` bigint NOT NULL COMMENT '专利ID',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '收藏备注',
  `group_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '收藏夹分组',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_patent`(`user_id` ASC, `patent_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_patent_id`(`patent_id` ASC) USING BTREE,
  INDEX `idx_group_name`(`group_name` ASC) USING BTREE,
  CONSTRAINT `fk_favorite_patent` FOREIGN KEY (`patent_id`) REFERENCES `patent` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_favorite_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '专利收藏表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for patent_vector
-- ----------------------------
DROP TABLE IF EXISTS `patent_vector`;
CREATE TABLE `patent_vector`  (
  `patent_id` bigint NOT NULL COMMENT '专利ID',
  `vector_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Qdrant向量ID（UUID）',
  `embedding_model` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '向量模型：text-embedding-v3/nomic-embed-text',
  `vector_dim` int NULL DEFAULT NULL COMMENT '向量维度',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`patent_id`) USING BTREE,
  CONSTRAINT `fk_vector_patent` FOREIGN KEY (`patent_id`) REFERENCES `patent` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '专利向量映射表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_llm_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_llm_config`;
CREATE TABLE `sys_llm_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL DEFAULT 0 COMMENT '用户ID（0=系统默认，>0=用户自定义）',
  `config_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置名称（如：通义千问、DeepSeek官方）',
  `llm_mode` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'LLM模式：online/offline',
  `base_url` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '自定义 API BaseURL（在线模式）',
  `api_key` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '加密存储的 API Key（在线模式）',
  `chat_model` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '对话模型名称（ChatService使用）',
  `llm_model` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '分析模型名称（LlmService使用）',
  `ollama_url` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'Ollama 服务地址（离线模式）',
  `is_active` tinyint NOT NULL DEFAULT 0 COMMENT '是否当前启用（0=禁用，1=启用）',
  `remark` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注说明',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除（0=正常，1=已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_llm_mode`(`llm_mode` ASC) USING BTREE,
  INDEX `idx_is_active`(`is_active` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'LLM配置管理表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_llm_selection
-- ----------------------------
DROP TABLE IF EXISTS `user_llm_selection`;
CREATE TABLE `user_llm_selection`  (
  `user_id` bigint NOT NULL COMMENT '用户ID（>0）',
  `config_id` bigint NOT NULL COMMENT '当前选择的配置ID（sys_llm_config.id）',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_id`) USING BTREE,
  INDEX `idx_config_id`(`config_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户LLM配置选择表（每用户一条，记录其激活的配置）' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;