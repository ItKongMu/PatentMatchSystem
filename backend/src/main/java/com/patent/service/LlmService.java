package com.patent.service;

import com.patent.model.entity.Patent;

/**
 * LLM服务接口
 */
public interface LlmService {

    /**
     * 提取专利实体和领域信息
     *
     * @param patentText 专利文本（标题+摘要+权利要求）
     * @return 分析结果
     */
    PatentAnalysisResult extractEntitiesAndDomain(String patentText);

    /**
     * 评估专利匹配度
     *
     * @param query           查询文本
     * @param queryEntities   查询实体
     * @param candidatePatent 候选专利
     * @return 匹配评分结果
     */
    MatchScoreResult evaluateMatch(String query, String queryEntities, Patent candidatePatent);

    /**
     * 专利分析结果
     */
    record PatentAnalysisResult(
            java.util.List<EntityInfo> entities,
            DomainInfo domain,
            java.util.List<String> keywords
    ) {}

    /**
     * 实体信息
     */
    record EntityInfo(
            String text,
            String type,
            String importance
    ) {}

    /**
     * 领域信息
     */
    record DomainInfo(
            String section,
            String mainClass,
            String subclass,
            String fullCode,
            String description
    ) {}

    /**
     * 匹配评分结果
     */
    record MatchScoreResult(
            int score,
            String reason,
            java.util.List<String> matchedEntities,
            boolean domainMatched,
            // 增强字段 - 详细匹配解释
            MatchExplanation explanation
    ) {
        /**
         * 兼容旧构造器
         */
        public MatchScoreResult(int score, String reason, java.util.List<String> matchedEntities, boolean domainMatched) {
            this(score, reason, matchedEntities, domainMatched, null);
        }
    }

    /**
     * 详细匹配解释
     */
    record MatchExplanation(
            String overallAnalysis,           // 整体匹配分析
            java.util.List<EntityMatchDetail> entityMatches,  // 实体匹配详情
            TechnicalSimilarity technicalSimilarity,  // 技术相似性分析
            String innovationPoint,           // 创新点对比
            String applicationScenario        // 应用场景分析
    ) {}

    /**
     * 实体匹配详情
     */
    record EntityMatchDetail(
            String queryEntity,      // 查询实体
            String matchedEntity,    // 匹配到的实体
            String entityType,       // 实体类型
            int similarity,          // 相似度(0-100)
            String matchReason       // 匹配原因
    ) {}

    /**
     * 技术相似性分析
     */
    record TechnicalSimilarity(
            int methodSimilarity,    // 方法相似度(0-100)
            int structureSimilarity, // 结构相似度(0-100)
            int effectSimilarity,    // 效果相似度(0-100)
            String keyDifference     // 关键差异点
    ) {}
}
