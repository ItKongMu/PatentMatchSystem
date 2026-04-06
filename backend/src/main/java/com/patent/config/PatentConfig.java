package com.patent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 专利系统核心配置类
 * <p>
 * LLM 对话/分析模型配置（base-url、api-key、chat-model、llm-model）
 * 全部由用户在前端配置并存入数据库，DynamicLlmFactory 按激活配置动态构建。
 * 向量维度由 spring.ai.openai.embedding.options.dimensions 统一配置。
 * 此处仅保留系统级公共配置。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "patent")
public class PatentConfig {

    /**
     * LLM模式：online-在线API / offline-Ollama本地
     * 主要用于系统状态展示，实际 LLM 实例由 DynamicLlmFactory 从数据库激活配置构建
     */
    private String llmMode = "offline";

    /**
     * 匹配结果数量
     */
    private Integer matchTopK = 10;

    /**
     * 相似度阈值（向量检索最低分）
     */
    private Double similarityThreshold = 0.6;
}
