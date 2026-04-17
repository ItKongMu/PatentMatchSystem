package com.patent.service.impl;

import com.alibaba.fastjson2.JSON;
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
import com.patent.model.vo.MatchSessionVO;
import com.patent.model.vo.MatchTaskVO;
import com.patent.service.AuthService;
import com.patent.service.GraphService;
import com.patent.service.LlmService;
import com.patent.service.MatchService;
import com.patent.service.VectorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 匹配服务实现（异步版本，解决大量专利匹配时前端超时问题）
 * 采用提交任务→轮询状态模式，前端立即获得 sessionId，后端异步执行匹配
 *
 * <p>任务状态存储迁移至 Redis：
 * <ul>
 *   <li>Key 格式：{@code match:task:{sessionId}}</li>
 *   <li>进行中（RUNNING）：TTL = 2 小时，防止 LLM 长任务被误删</li>
 *   <li>已完成/失败：TTL 主动缩短为 30 分钟，到期自动清理，避免内存积压</li>
 * </ul>
 *
 * <p>{@code matchByText} 同步方法（供 Chat @Tool 调用）内部也复用异步任务框架：
 * 提交任务后每 500ms 轮询一次 Redis，最长等待 90 秒，超时返回空结果集。
 */
@Slf4j
@Service
public class MatchServiceImpl implements MatchService {

    // ==================== Redis Key & TTL 常量 ====================

    /** Redis Key 前缀 */
    private static final String TASK_KEY_PREFIX = "match:task:";

    /** 进行中任务 TTL：2 小时（兼容 LLM 慢速场景） */
    private static final Duration TTL_RUNNING = Duration.ofHours(2);

    /** 完成/失败任务 TTL：30 分钟（供用户查看结果后自动清理） */
    private static final Duration TTL_DONE = Duration.ofMinutes(30);

    /**
     * matchByText 同步等待最长时间（毫秒）：90 秒
     * 超时后返回空结果，防止 Chat @Tool 调用无限阻塞
     */
    private static final long SYNC_WAIT_TIMEOUT_MS = 90_000L;

    /** 同步等待轮询间隔（毫秒）：500ms */
    private static final long SYNC_POLL_INTERVAL_MS = 500L;

    // ==================== 依赖注入 ====================

    private final PatentMapper patentMapper;
    private final PatentEntityMapper patentEntityMapper;
    private final PatentDomainMapper patentDomainMapper;
    private final MatchRecordMapper matchRecordMapper;
    private final VectorService vectorService;
    private final LlmService llmService;
    private final AuthService authService;
    private final GraphService graphService;
    private final Executor matchExecutor;
    private final RedisTemplate<String, Object> redisTemplate;

    public MatchServiceImpl(PatentMapper patentMapper,
                            PatentEntityMapper patentEntityMapper,
                            PatentDomainMapper patentDomainMapper,
                            MatchRecordMapper matchRecordMapper,
                            VectorService vectorService,
                            LlmService llmService,
                            AuthService authService,
                            GraphService graphService,
                            @Qualifier("matchExecutor") Executor matchExecutor,
                            RedisTemplate<String, Object> redisTemplate) {
        this.patentMapper = patentMapper;
        this.patentEntityMapper = patentEntityMapper;
        this.patentDomainMapper = patentDomainMapper;
        this.matchRecordMapper = matchRecordMapper;
        this.vectorService = vectorService;
        this.llmService = llmService;
        this.authService = authService;
        this.graphService = graphService;
        this.matchExecutor = matchExecutor;
        this.redisTemplate = redisTemplate;
    }

    // ==================== 接口实现 ====================

    @Override
    public MatchResultVO matchByText(MatchQueryDTO dto) {
        log.info("[Chat同步匹配] 复用异步任务框架，query长度={}", dto.getQuery() != null ? dto.getQuery().length() : 0);

        // 1. 提交异步任务，拿到 sessionId
        String sessionId = submitTextMatch(dto);

        // 2. 阻塞轮询 Redis，等待任务完成（最长 90 秒）
        long deadline = System.currentTimeMillis() + SYNC_WAIT_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            try {
                TimeUnit.MILLISECONDS.sleep(SYNC_POLL_INTERVAL_MS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.warn("[Chat同步匹配] 等待被中断，sessionId={}", sessionId);
                break;
            }

            MatchTaskVO task = getTask(sessionId);
            if (task == null) {
                log.warn("[Chat同步匹配] Redis中任务消失，sessionId={}", sessionId);
                break;
            }

            if ("COMPLETED".equals(task.getStatus())) {
                MatchResultVO result = task.getResult();
                log.info("[Chat同步匹配] 完成，sessionId={}，结果数={}", sessionId,
                        result != null && result.getMatches() != null ? result.getMatches().size() : 0);
                return result != null ? result : emptyResult(dto.getQuery());
            }

            if ("FAILED".equals(task.getStatus())) {
                log.warn("[Chat同步匹配] 任务失败，sessionId={}，原因={}", sessionId, task.getErrorMsg());
                return emptyResult(dto.getQuery());
            }
        }

        // 3. 超时兜底：返回空结果，不阻断 Chat 流程
        log.warn("[Chat同步匹配] 等待超时 {}ms，sessionId={}，返回空结果", SYNC_WAIT_TIMEOUT_MS, sessionId);
        return emptyResult(dto.getQuery());
    }

    /**
     * 构造空匹配结果（超时/失败兜底）
     */
    private MatchResultVO emptyResult(String query) {
        MatchResultVO result = new MatchResultVO();
        result.setQuery(query);
        result.setQueryEntities(new ArrayList<>());
        result.setMatches(new ArrayList<>());
        result.setTotalCount(0);
        return result;
    }

    @Override
    public String submitTextMatch(MatchQueryDTO dto) {
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        Long userId = getCurrentUserId();

        MatchTaskVO task = initTask(sessionId);
        saveTask(task); // 持久化到 Redis（TTL = 2h）

        // 提交异步任务（使用 CompletableFuture 避免 @Async 自调用失效）
        CompletableFuture.runAsync(() -> executeTextMatchAsync(sessionId, dto, userId), matchExecutor);

        return sessionId;
    }

    @Override
    public String submitPatentMatch(Long patentId, Integer topK) {
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        Long userId = getCurrentUserId();

        MatchTaskVO task = initTask(sessionId);
        saveTask(task); // 持久化到 Redis（TTL = 2h）

        CompletableFuture.runAsync(() -> executePatentMatchAsync(sessionId, patentId, topK, userId), matchExecutor);

        return sessionId;
    }

    @Override
    public MatchTaskVO getTaskStatus(String sessionId) {
        MatchTaskVO task = getTask(sessionId);
        if (task == null) {
            // 任务 Key 在 Redis 中不存在（已过期或从未创建）
            throw new BusinessException("任务不存在或已过期，请重新发起匹配");
        }
        return task;
    }

    @Override
    public PageResult<MatchSessionVO> getMatchHistory(Integer pageNum, Integer pageSize, String matchMode) {
        Long userId = getCurrentUserId();
        int offset = (pageNum - 1) * pageSize;

        long total = matchRecordMapper.countSessionByUser(userId, matchMode);
        List<MatchSessionVO> sessions = matchRecordMapper.selectSessionList(userId, matchMode, offset, pageSize);

        // 为每个session填充前3条匹配预览（传入 userId 防止跨用户访问）
        for (MatchSessionVO session : sessions) {
            List<MatchSessionVO.MatchItemSummary> topMatches =
                    matchRecordMapper.selectTopMatchesBySession(session.getSessionId(), userId, 3);
            session.setTopMatches(topMatches);
        }

        return PageResult.of(sessions, total, pageNum, pageSize);
    }

    @Override
    public MatchSessionVO.SessionDetailVO getSessionDetails(String sessionId) {
        Long userId = getCurrentUserId();
        List<MatchRecord> records = matchRecordMapper.selectBySessionId(sessionId, userId);

        // 解析查询实体（取第一条记录中的 queryEntities JSON）
        List<MatchSessionVO.QueryEntityItem> queryEntities = new ArrayList<>();
        if (!records.isEmpty() && records.get(0).getQueryEntities() != null) {
            try {
                // queryEntities 可能是两种格式：
                // TEXT模式：[{"text":"xxx","type":"PRODUCT",...}]（LlmService.EntityInfo）
                // PATENT模式：[{"entityName":"xxx","entityType":"PRODUCT",...}]（PatentEntity）
                com.alibaba.fastjson2.JSONArray arr =
                        com.alibaba.fastjson2.JSON.parseArray(records.get(0).getQueryEntities());
                for (int i = 0; i < arr.size(); i++) {
                    com.alibaba.fastjson2.JSONObject obj = arr.getJSONObject(i);
                    MatchSessionVO.QueryEntityItem item = new MatchSessionVO.QueryEntityItem();
                    // 兼容两种格式
                    String name = obj.getString("text") != null ? obj.getString("text") : obj.getString("entityName");
                    String type = obj.getString("type") != null ? obj.getString("type") : obj.getString("entityType");
                    if (name != null) {
                        item.setName(name);
                        item.setType(type);
                        queryEntities.add(item);
                    }
                }
            } catch (Exception e) {
                log.warn("[getSessionDetails] 解析queryEntities失败，sessionId={}, err={}", sessionId, e.getMessage());
            }
        }

        // 构建匹配列表（带详细字段）
        List<MatchSessionVO.MatchItemSummary> matches = records.stream()
                .map(r -> {
                    MatchSessionVO.MatchItemSummary s = new MatchSessionVO.MatchItemSummary();
                    s.setTargetPatentId(r.getTargetPatentId());
                    s.setTargetPatentTitle(r.getTargetPatentTitle() != null
                            ? r.getTargetPatentTitle() : "专利 #" + r.getTargetPatentId());
                    s.setSimilarityScore(r.getSimilarityScore());
                    s.setEntityMatchCount(r.getEntityMatchCount());
                    s.setDomainMatch(r.getDomainMatch() != null && r.getDomainMatch() == 1);
                    s.setMatchReason(r.getMatchReason());

                    // 补充专利详细信息
                    if (r.getTargetPatentId() != null) {
                        Patent patent = patentMapper.selectById(r.getTargetPatentId());
                        if (patent != null) {
                            s.setPublicationNo(patent.getPublicationNo());
                            s.setApplicant(patent.getApplicant());
                            s.setPatentAbstract(patent.getPatentAbstract());
                        }
                        // 补充领域信息
                        List<PatentDomain> domains = patentDomainMapper.selectByPatentId(r.getTargetPatentId());
                        if (domains != null && !domains.isEmpty()) {
                            s.setDomainCodes(domains.stream()
                                    .map(PatentDomain::getDomainCode)
                                    .collect(Collectors.toList()));
                        }
                    }
                    return s;
                })
                .collect(Collectors.toList());

        MatchSessionVO.SessionDetailVO detail = new MatchSessionVO.SessionDetailVO();
        detail.setQueryEntities(queryEntities);
        detail.setMatches(matches);
        return detail;
    }

    // ==================== 异步执行方法（通过 CompletableFuture.runAsync 投递到 matchExecutor 线程池） ====================

    private void executeTextMatchAsync(String sessionId, MatchQueryDTO dto, Long userId) {
        MatchTaskVO task = getTask(sessionId);
        if (task == null) {
            log.warn("[{}] 任务在 Redis 中不存在，跳过执行", sessionId);
            return;
        }
        try {
            log.info("[{}] 开始文本匹配查询: {}", sessionId,
                    dto.getQuery().substring(0, Math.min(50, dto.getQuery().length())));

            updateTaskProgress(task, 5);
            LlmService.PatentAnalysisResult queryAnalysis = llmService.extractEntitiesAndDomain(dto.getQuery());

            updateTaskProgress(task, 15);
            int topK = dto.getTopK();
            List<Document> candidates = vectorService.semanticSearch(
                    dto.getQuery(), dto.getDomainFilter(), topK * 2);
            int totalCandidates = candidates.size();
            // totalCount 使用用户期望的结果数，而非候选数
            task.setTotalCount(topK);
            saveTask(task);
            log.info("[{}] 向量检索候选数量: {}", sessionId, totalCandidates);

            // 用于在循环中追踪每条候选的 LLM 评分结果（保存历史记录时需要）
            Map<Long, LlmService.MatchScoreResult> scoreResultMap = new LinkedHashMap<>();
            Map<Long, Patent> patentMap = new LinkedHashMap<>();
            List<MatchResultVO.MatchItemVO> matchItems = new ArrayList<>();
            String queryEntitiesJson = JSON.toJSONString(queryAnalysis.entities());

            for (int i = 0; i < totalCandidates; i++) {
                Document doc = candidates.get(i);
                Long patentId = Long.valueOf(doc.getMetadata().get("patent_id").toString());
                Patent patent = patentMapper.selectById(patentId);
                if (patent == null) continue;

                LlmService.MatchScoreResult scoreResult = llmService.evaluateMatch(
                        dto.getQuery(), queryEntitiesJson, patent);

                MatchResultVO.MatchItemVO item = buildMatchItem(patent, scoreResult);
                matchItems.add(item);
                scoreResultMap.put(patentId, scoreResult);
                patentMap.put(patentId, patent);

                // processedCount 显示已找到的候选数，上限为 topK
                int progress = 15 + (int) ((double) (i + 1) / totalCandidates * 80);
                task.setProgress(Math.min(progress, 95));
                task.setProcessedCount(Math.min(matchItems.size(), topK));
                saveTask(task); // 每处理一条专利，同步进度到 Redis
            }

            matchItems.sort(Comparator.comparing(MatchResultVO.MatchItemVO::getSimilarityScore).reversed());
            // 截取 topK 条，保证返回数量符合用户请求
            if (matchItems.size() > topK) {
                matchItems = new ArrayList<>(matchItems.subList(0, topK));
            }

            // 只将最终的 topK 条结果保存到历史记录
            for (MatchResultVO.MatchItemVO item : matchItems) {
                Patent patent = patentMap.get(item.getPatentId());
                LlmService.MatchScoreResult scoreResult = scoreResultMap.get(item.getPatentId());
                if (patent != null && scoreResult != null) {
                    saveMatchRecord(sessionId, userId, null, null, dto.getQuery(), queryEntitiesJson,
                            dto.getDomainFilter(), patent, scoreResult, "TEXT", topK);
                }
            }

            MatchResultVO result = new MatchResultVO();
            result.setQuery(dto.getQuery());
            result.setQueryEntities(queryAnalysis.entities().stream()
                    .map(e -> {
                        MatchResultVO.QueryEntityVO qe = new MatchResultVO.QueryEntityVO();
                        qe.setName(e.text());
                        qe.setType(e.type());
                        return qe;
                    }).collect(Collectors.toList()));
            result.setMatches(matchItems);
            result.setTotalCount(matchItems.size());

            task.setProgress(100);
            task.setStatus("COMPLETED");
            task.setResult(result);
            finishTask(task); // 完成后缩短 TTL 为 30min
            log.info("[{}] 文本匹配完成，找到 {} 个结果", sessionId, matchItems.size());

        } catch (Exception e) {
            log.error("[{}] 文本匹配失败: {}", sessionId, e.getMessage(), e);
            task.setStatus("FAILED");
            task.setErrorMsg(e.getMessage());
            finishTask(task); // 失败后同样缩短 TTL 为 30min
        }
    }

    private void executePatentMatchAsync(String sessionId, Long patentId, Integer topK, Long userId) {
        MatchTaskVO task = getTask(sessionId);
        if (task == null) {
            log.warn("[{}] 任务在 Redis 中不存在，跳过执行", sessionId);
            return;
        }
        try {
            Patent sourcePatent = patentMapper.selectById(patentId);
            if (sourcePatent == null) {
                task.setStatus("FAILED");
                task.setErrorMsg("专利不存在");
                finishTask(task);
                return;
            }

            log.info("[{}] 开始专利相似匹配: {}", sessionId, sourcePatent.getTitle());
            updateTaskProgress(task, 5);

            List<PatentEntity> sourceEntities = patentEntityMapper.selectByPatentId(patentId);
            String queryText = String.format("%s %s %s",
                    sourcePatent.getTitle(),
                    sourcePatent.getPatentAbstract() != null ? sourcePatent.getPatentAbstract() : "",
                    sourceEntities.stream().map(PatentEntity::getEntityName).collect(Collectors.joining(" ")));

            updateTaskProgress(task, 10);
            List<Document> candidates = vectorService.semanticSearch(queryText, null, topK * 2, patentId);
            int totalCandidates = candidates.size();
            // totalCount 使用用户期望的结果数，而非候选数
            task.setTotalCount(topK);
            saveTask(task);
            log.info("[{}] 向量检索候选数量: {}", sessionId, totalCandidates);

            // 用于在循环中追踪每条候选的 LLM 评分结果（保存历史记录时需要）
            Map<Long, LlmService.MatchScoreResult> scoreResultMap = new LinkedHashMap<>();
            Map<Long, Patent> candidateMap = new LinkedHashMap<>();
            List<MatchResultVO.MatchItemVO> matchItems = new ArrayList<>();
            String sourceEntitiesJson = JSON.toJSONString(sourceEntities.stream()
                    .map(e -> new LlmService.EntityInfo(e.getEntityName(), e.getEntityType(), e.getImportance()))
                    .collect(Collectors.toList()));

            for (int i = 0; i < totalCandidates; i++) {
                Document doc = candidates.get(i);
                Long candidateId = Long.valueOf(doc.getMetadata().get("patent_id").toString());
                if (candidateId.equals(patentId)) continue;

                Patent candidate = patentMapper.selectById(candidateId);
                if (candidate == null) continue;

                LlmService.MatchScoreResult scoreResult = llmService.evaluateMatch(
                        sourcePatent.getTitle() + " " + sourcePatent.getPatentAbstract(),
                        sourceEntitiesJson, candidate);

                MatchResultVO.MatchItemVO item = buildMatchItem(candidate, scoreResult);
                matchItems.add(item);
                scoreResultMap.put(candidateId, scoreResult);
                candidateMap.put(candidateId, candidate);

                // processedCount 显示已找到的候选数，上限为 topK
                int progress = 10 + (int) ((double) (i + 1) / totalCandidates * 85);
                task.setProgress(Math.min(progress, 95));
                task.setProcessedCount(Math.min(matchItems.size(), topK));
                saveTask(task); // 每处理一条专利，同步进度到 Redis
            }

            matchItems.sort(Comparator.comparing(MatchResultVO.MatchItemVO::getSimilarityScore).reversed());
            // 截取 topK 条，保证返回数量符合用户请求
            if (matchItems.size() > topK) {
                matchItems = new ArrayList<>(matchItems.subList(0, topK));
            }

            // 只将最终的 topK 条结果保存到历史记录
            for (MatchResultVO.MatchItemVO item : matchItems) {
                Patent candidate = candidateMap.get(item.getPatentId());
                LlmService.MatchScoreResult scoreResult = scoreResultMap.get(item.getPatentId());
                if (candidate != null && scoreResult != null) {
                    saveMatchRecord(sessionId, userId, patentId, sourcePatent.getTitle(), null,
                            sourceEntitiesJson, null, candidate, scoreResult, "PATENT", topK);
                }
            }

            MatchResultVO result = new MatchResultVO();
            result.setQuery(sourcePatent.getTitle());
            result.setQueryEntities(sourceEntities.stream()
                    .map(e -> {
                        MatchResultVO.QueryEntityVO qe = new MatchResultVO.QueryEntityVO();
                        qe.setName(e.getEntityName());
                        qe.setType(e.getEntityType());
                        return qe;
                    }).collect(Collectors.toList()));
            result.setMatches(matchItems);
            result.setTotalCount(matchItems.size());

            task.setProgress(100);
            task.setStatus("COMPLETED");
            task.setResult(result);
            finishTask(task); // 完成后缩短 TTL 为 30min
            log.info("[{}] 专利相似匹配完成，找到 {} 个结果", sessionId, matchItems.size());

        } catch (Exception e) {
            log.error("[{}] 专利匹配失败: {}", sessionId, e.getMessage(), e);
            task.setStatus("FAILED");
            task.setErrorMsg(e.getMessage());
            finishTask(task); // 失败后同样缩短 TTL 为 30min
        }
    }

    // ==================== Redis 操作辅助方法 ====================

    /**
     * 构建 Redis Key
     */
    private String taskKey(String sessionId) {
        return TASK_KEY_PREFIX + sessionId;
    }

    /**
     * 将任务状态写入 Redis，TTL = {@link #TTL_RUNNING}（2小时）
     * 适用于：任务初始化、进度更新等中间状态
     */
    private void saveTask(MatchTaskVO task) {
        redisTemplate.opsForValue().set(taskKey(task.getSessionId()), task, TTL_RUNNING);
    }

    /**
     * 从 Redis 读取任务状态
     *
     * @return 任务VO，Key 不存在（已过期）时返回 null
     */
    private MatchTaskVO getTask(String sessionId) {
        Object value = redisTemplate.opsForValue().get(taskKey(sessionId));
        if (value instanceof MatchTaskVO) {
            return (MatchTaskVO) value;
        }
        return null;
    }

    /**
     * 任务终态（COMPLETED/FAILED）处理：写入 Redis 并主动缩短 TTL 为 30 分钟
     * 避免已完成的大体积结果长期占用 Redis 内存
     */
    private void finishTask(MatchTaskVO task) {
        redisTemplate.opsForValue().set(taskKey(task.getSessionId()), task, TTL_DONE);
        log.debug("[{}] 任务已终态 {}，Redis TTL 缩短至 {}min",
                task.getSessionId(), task.getStatus(), TTL_DONE.toMinutes());
    }

    /**
     * 更新进度并持久化到 Redis（中间态）
     */
    private void updateTaskProgress(MatchTaskVO task, int progress) {
        task.setProgress(progress);
        saveTask(task);
    }

    // ==================== 私有辅助方法 ====================

    private MatchTaskVO initTask(String sessionId) {
        MatchTaskVO task = new MatchTaskVO();
        task.setSessionId(sessionId);
        task.setStatus("RUNNING");
        task.setProgress(0);
        task.setProcessedCount(0);
        task.setTotalCount(0);
        task.setStartTime(LocalDateTime.now());
        return task;
    }

    private MatchResultVO.MatchItemVO buildMatchItem(Patent patent, LlmService.MatchScoreResult scoreResult) {
        return buildMatchItem(patent, scoreResult, null);
    }

    private MatchResultVO.MatchItemVO buildMatchItem(Patent patent, LlmService.MatchScoreResult scoreResult,
                                                      List<String> queryEntityNames) {
        MatchResultVO.MatchItemVO item = new MatchResultVO.MatchItemVO();
        item.setPatentId(patent.getId());
        item.setPublicationNo(patent.getPublicationNo());
        item.setTitle(patent.getTitle());
        item.setPatentAbstract(patent.getPatentAbstract());
        item.setApplicant(patent.getApplicant());

        List<PatentDomain> domains = patentDomainMapper.selectByPatentId(patent.getId());
        item.setDomainCodes(domains.stream().map(PatentDomain::getDomainCode).collect(Collectors.toList()));

        double sVec = scoreResult.score() / 100.0;
        double sGraph = 0.0;
        if (patent.getPublicationNo() != null) {
            sGraph = graphService.calculateGraphScore(patent.getPublicationNo(), queryEntityNames);
        }

        double finalScore = Math.min(1.0, Math.max(0.0, 0.9 * sVec + 0.1 * sGraph));
        item.setSimilarityScore(BigDecimal.valueOf(finalScore).setScale(4, RoundingMode.HALF_UP));
        item.setMatchType("HYBRID");
        item.setEntityMatchCount(scoreResult.matchedEntities().size());
        item.setDomainMatch(scoreResult.domainMatched());
        item.setMatchReason(scoreResult.reason());

        LlmService.MatchExplanation explanation = scoreResult.explanation();
        if (explanation != null) {
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
                        }).collect(Collectors.toList());
                item.setMatchedEntityDetails(entityDetails);
            }

            MatchResultVO.MatchExplanationVO explanationVO = new MatchResultVO.MatchExplanationVO();
            explanationVO.setOverallAnalysis(explanation.overallAnalysis());
            explanationVO.setInnovationPoint(explanation.innovationPoint());
            explanationVO.setApplicationScenario(explanation.applicationScenario());

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

    private void saveMatchRecord(String sessionId, Long userId,
                                  Long sourcePatentId, String sourcePatentTitle,
                                  String queryText, String queryEntities,
                                  String queryDomain, Patent targetPatent,
                                  LlmService.MatchScoreResult scoreResult,
                                  String matchMode, Integer topK) {
        MatchRecord record = new MatchRecord();
        record.setSessionId(sessionId);
        record.setUserId(userId);
        record.setMatchMode(matchMode);
        record.setSourcePatentId(sourcePatentId);
        record.setSourcePatentTitle(sourcePatentTitle);
        record.setQueryText(queryText);
        record.setQueryEntities(queryEntities);
        record.setQueryDomain(queryDomain);
        record.setTargetPatentId(targetPatent.getId());
        record.setTargetPatentTitle(targetPatent.getTitle());
        record.setTopK(topK);
        record.setSimilarityScore(BigDecimal.valueOf(scoreResult.score())
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        record.setMatchType("HYBRID");
        record.setEntityMatchCount(scoreResult.matchedEntities().size());
        record.setDomainMatch(scoreResult.domainMatched() ? 1 : 0);
        record.setMatchReason(scoreResult.reason());
        matchRecordMapper.insert(record);
    }

    private Long getCurrentUserId() {
        try {
            return authService.getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }
}
