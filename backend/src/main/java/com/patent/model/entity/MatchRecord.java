package com.patent.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 匹配记录表（统一保存专利匹配和文本查询）
 */
@Data
@TableName("match_record")
public class MatchRecord {

    /**
     * 匹配记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 匹配会话ID（同一次匹配请求共享，用于历史聚合）
     */
    private String sessionId;

    /**
     * 匹配模式：PATENT-专利匹配/TEXT-文本查询
     */
    private String matchMode;

    /**
     * 源专利ID（专利匹配时使用）
     */
    private Long sourcePatentId;

    /**
     * 源专利名称（冗余存储）
     */
    private String sourcePatentTitle;

    /**
     * 查询文本（文本查询时使用）
     */
    private String queryText;

    /**
     * 查询提取的实体JSON
     */
    private String queryEntities;

    /**
     * 查询的技术领域
     */
    private String queryDomain;

    /**
     * 目标专利ID
     */
    private Long targetPatentId;

    /**
     * 目标专利名称（冗余存储，便于历史查询）
     */
    private String targetPatentTitle;

    /**
     * 本次匹配要求返回的数量
     */
    private Integer topK;

    /**
     * 相似度评分（0.0000-1.0000）
     */
    private BigDecimal similarityScore;

    /**
     * 匹配类型：VECTOR/KEYWORD/HYBRID
     */
    private String matchType;

    /**
     * 实体匹配数量
     */
    private Integer entityMatchCount;

    /**
     * 领域是否匹配：1-是/0-否
     */
    private Integer domainMatch;

    /**
     * 匹配原因（LLM生成）
     */
    private String matchReason;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
