package com.patent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 专利系统配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "patent")
public class PatentConfig {

    /**
     * LLM模式：online-通义千问 / offline-Ollama
     */
    private String llmMode = "online";

    /**
     * 匹配结果数量
     */
    private Integer matchTopK = 10;

    /**
     * 相似度阈值
     */
    private Double similarityThreshold = 0.6;

    /**
     * 向量维度
     * text-embedding-v3 (通义千问) 默认1024
     * nomic-embed-text (Ollama) 默认768
     */
    private Integer vectorDimension = 1024;
}
