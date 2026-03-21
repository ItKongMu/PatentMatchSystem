package com.patent.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.patent.common.PageResult;
import com.patent.common.exception.BusinessException;
import com.patent.mapper.MatchRecordMapper;
import com.patent.mapper.PatentDomainMapper;
import com.patent.mapper.PatentEntityMapper;
import com.patent.mapper.PatentMapper;
import com.patent.model.dto.MatchQueryDTO;
import com.patent.model.entity.MatchRecord;
import com.patent.model.entity.Patent;
import com.patent.model.entity.PatentDomain;
import com.patent.model.entity.PatentEntity;
import com.patent.model.vo.MatchResultVO;
import com.patent.service.AuthService;
import com.patent.service.GraphService;
import com.patent.service.LlmService;
import com.patent.service.MatchService;
import com.patent.service.VectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 匹配服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchServiceImpl implements MatchService {

    private final PatentMapper patentMapper;
    private final PatentEntityMapper patentEntityMapper;
    private final PatentDomainMapper patentDomainMapper;
    private final MatchRecordMapper matchRecordMapper;
    private final VectorService vectorService;
    private final LlmService llmService;
    private final AuthService authService;
    private final GraphService graphService;

    @Override
    @Transactional
    public MatchResultVO matchByText(MatchQueryDTO dto) {
        log.info("开始文本匹配查询: {}", dto.getQuery().substring(0, Math.min(50, dto.getQuery().length())));

        // 1. 提取查询实体
        LlmService.PatentAnalysisResult queryAnalysis = llmService.extractEntitiesAndDomain(dto.getQuery());

        // 2. 向量检索
        List<Document> candidates = vectorService.semanticSearch(
                dto.getQuery(),
                dto.getDomainFilter(),
                dto.getTopK() * 2  // 多检索一些用于后续精排
        );

        // 3. 构建匹配结果
        List<MatchResultVO.MatchItemVO> matchItems = new ArrayList<>();
        String queryEntitiesJson = JSON.toJSONString(queryAnalysis.entities());
        
        for (Document doc : candidates) {
            Long patentId = Long.valueOf(doc.getMetadata().get("patent_id").toString());
            Patent patent = patentMapper.selectById(patentId);
            if (patent == null) continue;

            // 4. LLM精排评估
            LlmService.MatchScoreResult scoreResult = llmService.evaluateMatch(
                    dto.getQuery(),
                    queryEntitiesJson,
                    patent
            );

            // 构建匹配项
            MatchResultVO.MatchItemVO item = buildMatchItem(patent, scoreResult);
            matchItems.add(item);

            // 5. 保存匹配记录
            saveMatchRecord(null, dto.getQuery(), queryEntitiesJson, dto.getDomainFilter(),
                    patent.getId(), scoreResult, "TEXT");
        }

        // 6. 按分数排序并截取topK
        matchItems.sort(Comparator.comparing(MatchResultVO.MatchItemVO::getSimilarityScore).reversed());
        if (matchItems.size() > dto.getTopK()) {
            matchItems = matchItems.subList(0, dto.getTopK());
        }

        // 7. 构建响应
        MatchResultVO result = new MatchResultVO();
        result.setQuery(dto.getQuery());
        result.setQueryEntities(queryAnalysis.entities().stream()
                .map(e -> {
                    MatchResultVO.QueryEntityVO qe = new MatchResultVO.QueryEntityVO();
                    qe.setName(e.text());
                    qe.setType(e.type());
                    return qe;
                })
                .toList());
        result.setMatches(matchItems);
        result.setTotalCount(matchItems.size());

        log.info("文本匹配查询完成，找到 {} 个结果", matchItems.size());
        return result;
    }

    @Override
    @Transactional
    public MatchResultVO matchByPatent(Long patentId, Integer topK) {
        // 获取源专利
        Patent sourcePatent = patentMapper.selectById(patentId);
        if (sourcePatent == null) {
            throw new BusinessException("专利不存在");
        }

        log.info("开始专利相似匹配: {}", sourcePatent.getTitle());

        // 获取源专利实体
        List<PatentEntity> sourceEntities = patentEntityMapper.selectByPatentId(patentId);
        List<PatentDomain> sourceDomains = patentDomainMapper.selectByPatentId(patentId);

        // 构造查询文本
        String queryText = String.format("%s %s %s",
                sourcePatent.getTitle(),
                sourcePatent.getPatentAbstract() != null ? sourcePatent.getPatentAbstract() : "",
                sourceEntities.stream().map(PatentEntity::getEntityName).collect(Collectors.joining(" "))
        );

        // 获取领域过滤
        String domainFilter = sourceDomains.stream()
                .filter(d -> d.getDomainLevel() == 1)
                .map(PatentDomain::getDomainCode)
                .findFirst()
                .orElse(null);

        // 向量检索
        List<Document> candidates = vectorService.semanticSearch(queryText, domainFilter, topK * 2);

        // 构建匹配结果
        List<MatchResultVO.MatchItemVO> matchItems = new ArrayList<>();
        String sourceEntitiesJson = JSON.toJSONString(sourceEntities.stream()
                .map(e -> new LlmService.EntityInfo(e.getEntityName(), e.getEntityType(), e.getImportance()))
                .toList());

        for (Document doc : candidates) {
            Long candidateId = Long.valueOf(doc.getMetadata().get("patent_id").toString());
            // 排除自身
            if (candidateId.equals(patentId)) continue;

            Patent candidate = patentMapper.selectById(candidateId);
            if (candidate == null) continue;

            // LLM精排
            LlmService.MatchScoreResult scoreResult = llmService.evaluateMatch(
                    sourcePatent.getTitle() + " " + sourcePatent.getPatentAbstract(),
                    sourceEntitiesJson,
                    candidate
            );

            MatchResultVO.MatchItemVO item = buildMatchItem(candidate, scoreResult);
            matchItems.add(item);

            // 保存匹配记录
            saveMatchRecord(patentId, null, sourceEntitiesJson, domainFilter,
                    candidate.getId(), scoreResult, "PATENT");
        }

        // 排序并截取
        matchItems.sort(Comparator.comparing(MatchResultVO.MatchItemVO::getSimilarityScore).reversed());
        if (matchItems.size() > topK) {
            matchItems = matchItems.subList(0, topK);
        }

        // 构建响应
        MatchResultVO result = new MatchResultVO();
        result.setQuery(sourcePatent.getTitle());
        result.setQueryEntities(sourceEntities.stream()
                .map(e -> {
                    MatchResultVO.QueryEntityVO qe = new MatchResultVO.QueryEntityVO();
                    qe.setName(e.getEntityName());
                    qe.setType(e.getEntityType());
                    return qe;
                })
                .toList());
        result.setMatches(matchItems);
        result.setTotalCount(matchItems.size());

        log.info("专利相似匹配完成，找到 {} 个结果", matchItems.size());
        return result;
    }

    @Override
    public PageResult<MatchRecord> getMatchHistory(Integer pageNum, Integer pageSize, String matchMode) {
        Long userId = getCurrentUserId();
        Page<MatchRecord> page = new Page<>(pageNum, pageSize);
        IPage<MatchRecord> result = matchRecordMapper.selectMatchRecordPage(page, userId, matchMode);
        return PageResult.of(result.getRecords(), result.getTotal(), pageNum, pageSize);
    }

    /**
     * 构建匹配项 - 增强版，包含详细匹配解释和图谱增强评分
     */
    private MatchResultVO.MatchItemVO buildMatchItem(Patent patent, LlmService.MatchScoreResult scoreResult) {
        return buildMatchItem(patent, scoreResult, null);
    }

    /**
     * 构建匹配项 - 带查询实体列表，用于图谱增强评分
     */
    private MatchResultVO.MatchItemVO buildMatchItem(Patent patent, LlmService.MatchScoreResult scoreResult,
                                                      List<String> queryEntityNames) {
        MatchResultVO.MatchItemVO item = new MatchResultVO.MatchItemVO();
        item.setPatentId(patent.getId());
        item.setPublicationNo(patent.getPublicationNo());
        item.setTitle(patent.getTitle());
        item.setPatentAbstract(patent.getPatentAbstract());
        item.setApplicant(patent.getApplicant());

        // 获取领域代码
        List<PatentDomain> domains = patentDomainMapper.selectByPatentId(patent.getId());
        item.setDomainCodes(domains.stream().map(PatentDomain::getDomainCode).toList());

        // LLM 基础评分（归一化到 0~1）
        double sVec = scoreResult.score() / 100.0;

        // 图谱增强评分 S_graph（Neo4j 不可用时降级为 0）
        double sGraph = 0.0;
        if (patent.getPublicationNo() != null) {
            sGraph = graphService.calculateGraphScore(patent.getPublicationNo(), queryEntityNames);
        }

        // 综合评分：w1=0.9（LLM评分已包含语义+实体+领域）, w4=0.1（图谱路径分）
        double finalScore = 0.9 * sVec + 0.1 * sGraph;
        finalScore = Math.min(1.0, Math.max(0.0, finalScore));

        // 设置评分
        item.setSimilarityScore(BigDecimal.valueOf(finalScore).setScale(4, RoundingMode.HALF_UP));
        item.setMatchType("HYBRID");
        item.setEntityMatchCount(scoreResult.matchedEntities().size());
        item.setDomainMatch(scoreResult.domainMatched());
        item.setMatchReason(scoreResult.reason());

        // ========== 增强功能：设置详细匹配解释 ==========
        
        // 设置匹配实体详情
        LlmService.MatchExplanation explanation = scoreResult.explanation();
        if (explanation != null) {
            // 转换实体匹配详情
            if (explanation.entityMatches() != null) {
                List<MatchResultVO.EntityMatchDetailVO> entityDetails = explanation.entityMatches().stream()
                        .map(em -> {
                            MatchResultVO.EntityMatchDetailVO vo = new MatchResultVO.EntityMatchDetailVO();
                            vo.setQueryEntity(em.queryEntity());
                            vo.setMatchedEntity(em.matchedEntity());
                            vo.setEntityType(em.entityType());
                            vo.setSimilarity(em.similarity());
                            vo.setMatchReason(em.matchReason());
                            return vo;
                        })
                        .toList();
                item.setMatchedEntityDetails(entityDetails);
            }

            // 转换详细解释
            MatchResultVO.MatchExplanationVO explanationVO = new MatchResultVO.MatchExplanationVO();
            explanationVO.setOverallAnalysis(explanation.overallAnalysis());
            explanationVO.setInnovationPoint(explanation.innovationPoint());
            explanationVO.setApplicationScenario(explanation.applicationScenario());

            // 转换技术相似性
            if (explanation.technicalSimilarity() != null) {
                MatchResultVO.TechnicalSimilarityVO techVO = new MatchResultVO.TechnicalSimilarityVO();
                techVO.setMethodSimilarity(explanation.technicalSimilarity().methodSimilarity());
                techVO.setStructureSimilarity(explanation.technicalSimilarity().structureSimilarity());
                techVO.setEffectSimilarity(explanation.technicalSimilarity().effectSimilarity());
                techVO.setKeyDifference(explanation.technicalSimilarity().keyDifference());
                explanationVO.setTechnicalSimilarity(techVO);
            }

            item.setExplanation(explanationVO);
        }

        return item;
    }

    /**
     * 保存匹配记录
     */
    private void saveMatchRecord(Long sourcePatentId, String queryText, String queryEntities,
                                  String queryDomain, Long targetPatentId,
                                  LlmService.MatchScoreResult scoreResult, String matchMode) {
        MatchRecord record = new MatchRecord();
        record.setUserId(getCurrentUserId());
        record.setMatchMode(matchMode);
        record.setSourcePatentId(sourcePatentId);
        record.setQueryText(queryText);
        record.setQueryEntities(queryEntities);
        record.setQueryDomain(queryDomain);
        record.setTargetPatentId(targetPatentId);
        record.setSimilarityScore(BigDecimal.valueOf(scoreResult.score())
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        record.setMatchType("HYBRID");
        record.setEntityMatchCount(scoreResult.matchedEntities().size());
        record.setDomainMatch(scoreResult.domainMatched() ? 1 : 0);
        record.setMatchReason(scoreResult.reason());

        matchRecordMapper.insert(record);
    }

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId() {
        try {
            return authService.getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }
}
