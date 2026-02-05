package com.patent.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 匹配结果VO
 */
@Data
@Schema(description = "匹配结果")
public class MatchResultVO {

    @Schema(description = "查询文本")
    private String query;

    @Schema(description = "查询提取的实体")
    private List<QueryEntityVO> queryEntities;

    @Schema(description = "匹配的专利列表")
    private List<MatchItemVO> matches;

    @Schema(description = "总匹配数量")
    private Integer totalCount;

    /**
     * 查询实体VO
     */
    @Data
    @Schema(description = "查询实体")
    public static class QueryEntityVO {
        @Schema(description = "实体名称")
        private String name;

        @Schema(description = "实体类型")
        private String type;
    }

    /**
     * 匹配项VO
     */
    @Data
    @Schema(description = "匹配项")
    public static class MatchItemVO {
        @Schema(description = "专利ID")
        private Long patentId;

        @Schema(description = "公开号")
        private String publicationNo;

        @Schema(description = "专利标题")
        private String title;

        @Schema(description = "专利摘要")
        private String patentAbstract;

        @Schema(description = "申请人")
        private String applicant;

        @Schema(description = "领域代码列表")
        private List<String> domainCodes;

        @Schema(description = "相似度评分(0-1)")
        private BigDecimal similarityScore;

        @Schema(description = "匹配类型")
        private String matchType;

        @Schema(description = "实体匹配数量")
        private Integer entityMatchCount;

        @Schema(description = "领域是否匹配")
        private Boolean domainMatch;

        @Schema(description = "匹配原因")
        private String matchReason;

        // ========== 增强字段：详细匹配解释 ==========

        @Schema(description = "匹配的实体详情列表")
        private List<EntityMatchDetailVO> matchedEntityDetails;

        @Schema(description = "详细匹配解释")
        private MatchExplanationVO explanation;
    }

    /**
     * 实体匹配详情VO
     */
    @Data
    @Schema(description = "实体匹配详情")
    public static class EntityMatchDetailVO {
        @Schema(description = "查询实体名称")
        private String queryEntity;

        @Schema(description = "匹配到的目标实体")
        private String matchedEntity;

        @Schema(description = "实体类型(PRODUCT/METHOD/MATERIAL/COMPONENT/EFFECT/APPLICATION)")
        private String entityType;

        @Schema(description = "相似度(0-100)")
        private Integer similarity;

        @Schema(description = "匹配原因说明")
        private String matchReason;
    }

    /**
     * 详细匹配解释VO
     */
    @Data
    @Schema(description = "详细匹配解释")
    public static class MatchExplanationVO {
        @Schema(description = "整体匹配分析")
        private String overallAnalysis;

        @Schema(description = "技术相似性分析")
        private TechnicalSimilarityVO technicalSimilarity;

        @Schema(description = "创新点对比")
        private String innovationPoint;

        @Schema(description = "应用场景分析")
        private String applicationScenario;
    }

    /**
     * 技术相似性VO
     */
    @Data
    @Schema(description = "技术相似性分析")
    public static class TechnicalSimilarityVO {
        @Schema(description = "方法相似度(0-100)")
        private Integer methodSimilarity;

        @Schema(description = "结构相似度(0-100)")
        private Integer structureSimilarity;

        @Schema(description = "效果相似度(0-100)")
        private Integer effectSimilarity;

        @Schema(description = "关键差异点")
        private String keyDifference;
    }
}
