package com.patent.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 匹配会话视图对象（历史列表按会话聚合）
 */
@Data
public class MatchSessionVO {

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 匹配模式：PATENT/TEXT
     */
    private String matchMode;

    /**
     * 查询来源（文本查询时为查询文本；专利匹配时为源专利名称）
     */
    private String querySource;

    /**
     * 源专利ID（专利匹配时使用）
     */
    private Long sourcePatentId;

    /**
     * 要求返回数量（topK）
     */
    private Integer topK;

    /**
     * 实际匹配到的专利数量
     */
    private Integer matchCount;

    /**
     * 会话创建时间（最早记录时间）
     */
    private LocalDateTime createdAt;

    /**
     * 匹配专利列表摘要（前3条用于预览）
     */
    private List<MatchItemSummary> topMatches;

    /**
     * 匹配专利摘要（扩展详情字段）
     */
    @Data
    public static class MatchItemSummary {
        /** 目标专利ID */
        private Long targetPatentId;
        /** 目标专利标题 */
        private String targetPatentTitle;
        /** 相似度评分 */
        private BigDecimal similarityScore;

        // ========== 详情扩展字段 ==========
        /** 公开号 */
        private String publicationNo;
        /** 申请人 */
        private String applicant;
        /** 专利摘要 */
        private String patentAbstract;
        /** 领域代码列表 */
        private List<String> domainCodes;
        /** 实体匹配数量 */
        private Integer entityMatchCount;
        /** 领域是否匹配 */
        private Boolean domainMatch;
        /** 匹配原因（LLM生成） */
        private String matchReason;
    }

    /**
     * 查询实体项（用于历史详情展示）
     */
    @Data
    public static class QueryEntityItem {
        private String name;
        private String type;
    }

    /**
     * Session 详情响应（包含查询实体 + 匹配列表，供历史详情抽屉使用）
     */
    @Data
    public static class SessionDetailVO {
        /** 从记录中解析出的查询实体列表 */
        private List<QueryEntityItem> queryEntities;
        /** 匹配专利列表（含详细字段） */
        private List<MatchItemSummary> matches;
    }
}
