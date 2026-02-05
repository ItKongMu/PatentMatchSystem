package com.patent.service.impl;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import com.patent.common.PageResult;
import com.patent.mapper.PatentDomainMapper;
import com.patent.mapper.PatentEntityMapper;
import com.patent.mapper.PatentMapper;
import com.patent.model.dto.SearchDTO;
import com.patent.model.entity.Patent;
import com.patent.model.entity.PatentDomain;
import com.patent.model.entity.PatentEntity;
import com.patent.model.es.PatentDocument;
import com.patent.model.vo.PatentListVO;
import com.patent.model.vo.SearchResultVO;
import com.patent.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Elasticsearch检索服务实现
 * 
 * <p>支持以下功能：
 * <ul>
 *   <li>快速检索：多字段匹配 + 短语提升 + 高亮显示</li>
 *   <li>高级检索：多条件组合 + 过滤 + 排序</li>
 *   <li>深度分页：search_after 实现</li>
 *   <li>聚合统计：领域分布、申请人排行</li>
 * </ul>
 * 
 * @version 2.0
 * @since ES 8.12+
 */
@Slf4j
@Service
public class SearchServiceImpl implements SearchService {

    private static final String INDEX_NAME = "patent_index";
    private static final String HIGHLIGHT_PRE_TAG = "<em class=\"highlight\">";
    private static final String HIGHLIGHT_POST_TAG = "</em>";
    
    /** 搜索结果返回的字段列表 */
    private static final String[] SOURCE_FIELDS = {
            "id", "publication_no", "title", "abstract_text", "applicant",
            "publication_date", "domain_codes", "domain_section",
            "entities", "entity_types", "parse_status", "created_at"
    };
    
    /**
     * Elasticsearch 操作接口
     * <p>Spring Data Elasticsearch 推荐使用 {@link ElasticsearchOperations} 接口进行操作。
     * 
     * @see <a href="https://docs.spring.io/spring-data/elasticsearch/reference/elasticsearch/template.html">Elasticsearch Operations</a>
     */
    private final ElasticsearchOperations elasticsearchOperations;
    private final PatentMapper patentMapper;
    private final PatentEntityMapper patentEntityMapper;
    private final PatentDomainMapper patentDomainMapper;

    public SearchServiceImpl(
            @Autowired(required = false) ElasticsearchOperations elasticsearchOperations,
            PatentMapper patentMapper,
            PatentEntityMapper patentEntityMapper,
            PatentDomainMapper patentDomainMapper) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.patentMapper = patentMapper;
        this.patentEntityMapper = patentEntityMapper;
        this.patentDomainMapper = patentDomainMapper;
        
        if (elasticsearchOperations != null) {
            log.info("Elasticsearch 连接成功，ElasticsearchOperations 已注入");
        } else {
            log.warn("Elasticsearch 不可用，检索功能将降级到 MySQL");
        }
    }

    // ==================== 快速检索 ====================

    @Override
    public PageResult<SearchResultVO> quickSearch(String keyword, Integer pageNum, Integer pageSize) {
        if (!isEsAvailable()) {
            return convertToSearchResultVO(fallbackSearch(keyword, pageNum, pageSize));
        }

        try {
            // 构建快速检索查询：多字段匹配 + 短语提升
            Query quickSearchQuery = buildQuickSearchQuery(keyword);
            
            // 构建高亮配置
            HighlightQuery highlightQuery = buildHighlightQuery();
            
            NativeQuery query = new NativeQueryBuilder()
                    .withQuery(quickSearchQuery)
                    .withHighlightQuery(highlightQuery)
                    .withPageable(PageRequest.of(pageNum - 1, pageSize))
                    .withSort(Sort.by(Sort.Direction.DESC, "_score"))
                    .withSourceFilter(org.springframework.data.elasticsearch.core.query.FetchSourceFilter.of(
                            b -> b.withIncludes(SOURCE_FIELDS)))
                    .build();

            SearchHits<PatentDocument> searchHits = elasticsearchOperations.search(query, PatentDocument.class);
            
            List<SearchResultVO> list = searchHits.stream()
                    .map(this::convertToSearchResultVO)
                    .toList();

            return PageResult.of(list, searchHits.getTotalHits(), pageNum, pageSize);

        } catch (Exception e) {
            log.error("ES快速检索失败，降级到MySQL: keyword={}", keyword, e);
            return convertToSearchResultVO(fallbackSearch(keyword, pageNum, pageSize));
        }
    }

    /**
     * 构建快速检索查询
     * <p>使用 bool 查询组合：
     * <ul>
     *   <li>best_fields 多字段匹配（核心匹配）</li>
     *   <li>phrase 短语匹配（提升精确匹配评分）</li>
     *   <li>filter 过滤已解析成功的专利</li>
     * </ul>
     */
    private Query buildQuickSearchQuery(String keyword) {
        return Query.of(q -> q.bool(b -> b
                // 核心匹配：多字段best_fields
                .should(s -> s.multiMatch(mm -> mm
                        .query(keyword)
                        .fields(List.of(
                                "title^4",           // 标题权重最高
                                "abstract_text^2",   // 摘要次之
                                "entities^3"         // 实体较高
                        ))
                        .type(TextQueryType.BestFields)
                        .operator(co.elastic.clients.elasticsearch._types.query_dsl.Operator.Or)
                        .minimumShouldMatch("30%")
                        .fuzziness("AUTO")
                ))
                // 短语匹配提升：完全匹配的评分更高
                .should(s -> s.multiMatch(mm -> mm
                        .query(keyword)
                        .fields(List.of("title^5", "abstract_text^3"))
                        .type(TextQueryType.Phrase)
                        .slop(2)
                        .boost(2.0f)
                ))
                .minimumShouldMatch("1")
                // 过滤：只返回解析成功的专利
                .filter(f -> f.term(t -> t.field("parse_status").value("SUCCESS")))
        ));
    }

    @Override
    public PageResult<PatentListVO> searchByKeyword(String keyword, Integer pageNum, Integer pageSize) {
        // 复用快速检索逻辑，转换返回类型
        PageResult<SearchResultVO> searchResult = quickSearch(keyword, pageNum, pageSize);
        
        List<PatentListVO> list = searchResult.getList().stream().map(vo -> {
            PatentListVO listVO = new PatentListVO();
            listVO.setId(vo.getId());
            listVO.setPublicationNo(vo.getPublicationNo());
            listVO.setTitle(vo.getTitle());
            listVO.setApplicant(vo.getApplicant());
            listVO.setPatentAbstract(vo.getPatentAbstract());
            listVO.setPublicationDate(vo.getPublicationDate());
            listVO.setSourceType(vo.getSourceType());
            listVO.setParseStatus(vo.getParseStatus());
            listVO.setCreatedAt(vo.getCreatedAt());
            listVO.setEntities(vo.getEntities());
            listVO.setDomains(vo.getDomains());
            return listVO;
        }).toList();
        
        return PageResult.of(list, searchResult.getTotal(), searchResult.getPageNum(), searchResult.getPageSize());
    }

    // ==================== 高级检索 ====================

    @Override
    public PageResult<SearchResultVO> advancedSearchWithHighlight(SearchDTO dto) {
        if (!isEsAvailable()) {
            return convertToSearchResultVO(fallbackAdvancedSearch(dto));
        }

        try {
            // 构建高级检索查询
            Query advancedQuery = buildAdvancedSearchQuery(dto);
            
            // 构建查询
            NativeQueryBuilder queryBuilder = new NativeQueryBuilder()
                    .withQuery(advancedQuery);
            
            // 高亮配置
            if (Boolean.TRUE.equals(dto.getEnableHighlight())) {
                queryBuilder.withHighlightQuery(buildHighlightQuery());
            }
            
            // 排序配置
            configureSort(queryBuilder, dto);
            
            // 分页配置
            if (Boolean.TRUE.equals(dto.getUseSearchAfter()) && dto.getSearchAfter() != null) {
                // 深度分页模式
                queryBuilder.withSearchAfter(dto.getSearchAfter());
                queryBuilder.withMaxResults(dto.getPageSize());
            } else {
                // 普通分页
                queryBuilder.withPageable(PageRequest.of(dto.getPageNum() - 1, dto.getPageSize()));
            }
            
            // 字段过滤
            queryBuilder.withSourceFilter(org.springframework.data.elasticsearch.core.query.FetchSourceFilter.of(
                    b -> b.withIncludes(SOURCE_FIELDS)));
            
            NativeQuery query = queryBuilder.build();
            SearchHits<PatentDocument> searchHits = elasticsearchOperations.search(query, PatentDocument.class);
            
            List<SearchResultVO> list = searchHits.stream()
                    .map(this::convertToSearchResultVO)
                    .toList();

            return PageResult.of(list, searchHits.getTotalHits(), dto.getPageNum(), dto.getPageSize());

        } catch (Exception e) {
            log.error("ES高级检索失败，降级到MySQL", e);
            return convertToSearchResultVO(fallbackAdvancedSearch(dto));
        }
    }

    /**
     * 构建高级检索查询
     * <p>支持以下条件组合：
     * <ul>
     *   <li>must: 标题、摘要的全文匹配</li>
     *   <li>should: 实体匹配（增加相关性）</li>
     *   <li>filter: 领域、申请人、日期范围、实体类型等精确过滤</li>
     *   <li>must_not: 排除已删除的专利</li>
     * </ul>
     */
    private Query buildAdvancedSearchQuery(SearchDTO dto) {
        return Query.of(q -> q.bool(b -> {
            BoolQuery.Builder builder = b;
            
            // ===== MUST 条件（参与评分）=====
            // 标题匹配
            if (StringUtils.hasText(dto.getTitle())) {
                builder.must(m -> m.match(mm -> mm
                        .field("title")
                        .query(dto.getTitle())
                        .operator(co.elastic.clients.elasticsearch._types.query_dsl.Operator.And)
                        .boost(2.0f)
                ));
            }
            
            // 摘要匹配
            if (StringUtils.hasText(dto.getAbstractKeyword())) {
                builder.must(m -> m.match(mm -> mm
                        .field("abstract_text")
                        .query(dto.getAbstractKeyword())
                        .minimumShouldMatch("50%")
                ));
            }
            
            // 通用关键词（跨字段搜索）
            if (StringUtils.hasText(dto.getKeyword())) {
                builder.must(m -> m.multiMatch(mm -> mm
                        .query(dto.getKeyword())
                        .fields(List.of("title^3", "abstract_text^2", "entities^2"))
                        .type(TextQueryType.CrossFields)
                        .operator(co.elastic.clients.elasticsearch._types.query_dsl.Operator.Or)
                        .minimumShouldMatch("30%")
                ));
            }
            
            // ===== SHOULD 条件（增加相关性）=====
            // 实体关键词匹配
            if (StringUtils.hasText(dto.getEntityKeyword())) {
                builder.should(s -> s.match(mm -> mm
                        .field("entities")
                        .query(dto.getEntityKeyword())
                        .boost(1.5f)
                ));
            }
            
            // ===== FILTER 条件（不参与评分，可缓存）=====
            // 解析状态过滤
            builder.filter(f -> f.term(t -> t.field("parse_status").value("SUCCESS")));
            
            // 专利号精确匹配
            if (StringUtils.hasText(dto.getPublicationNo())) {
                builder.filter(f -> f.term(t -> t.field("publication_no").value(dto.getPublicationNo())));
            }
            
            // 申请人精确匹配
            if (StringUtils.hasText(dto.getApplicant())) {
                builder.filter(f -> f.term(t -> t.field("applicant").value(dto.getApplicant())));
            }
            
            // 申请人模糊匹配
            if (StringUtils.hasText(dto.getApplicantKeyword())) {
                builder.filter(f -> f.match(mm -> mm
                        .field("applicant.text")
                        .query(dto.getApplicantKeyword())
                ));
            }
            
            // 领域代码前缀匹配
            if (StringUtils.hasText(dto.getDomainCode())) {
                builder.filter(f -> f.prefix(p -> p.field("domain_codes").value(dto.getDomainCode())));
            }
            
            // 领域部多选
            if (dto.getDomainSections() != null && !dto.getDomainSections().isEmpty()) {
                builder.filter(f -> f.terms(t -> t
                        .field("domain_section")
                        .terms(tv -> tv.value(dto.getDomainSections().stream()
                                .map(FieldValue::of)
                                .toList()))
                ));
            }
            
            // 实体类型单选
            if (StringUtils.hasText(dto.getEntityType())) {
                builder.filter(f -> f.term(t -> t.field("entity_types").value(dto.getEntityType())));
            }
            
            // 实体类型多选
            if (dto.getEntityTypes() != null && !dto.getEntityTypes().isEmpty()) {
                builder.filter(f -> f.terms(t -> t
                        .field("entity_types")
                        .terms(tv -> tv.value(dto.getEntityTypes().stream()
                                .map(FieldValue::of)
                                .toList()))
                ));
            }
            
            // 日期范围过滤
            if (dto.getPublicationDateFrom() != null || dto.getPublicationDateTo() != null) {
                builder.filter(f -> f.range(r -> {
                    var rangeBuilder = r.field("publication_date");
                    if (dto.getPublicationDateFrom() != null) {
                        rangeBuilder.gte(co.elastic.clients.json.JsonData.of(
                                dto.getPublicationDateFrom().format(DateTimeFormatter.ISO_DATE)));
                    }
                    if (dto.getPublicationDateTo() != null) {
                        rangeBuilder.lte(co.elastic.clients.json.JsonData.of(
                                dto.getPublicationDateTo().format(DateTimeFormatter.ISO_DATE)));
                    }
                    return rangeBuilder;
                }));
            }
            
            return builder;
        }));
    }

    @Override
    public PageResult<PatentListVO> advancedSearch(SearchDTO dto) {
        // 复用带高亮的高级检索逻辑，禁用高亮以提升性能
        dto.setEnableHighlight(false);
        PageResult<SearchResultVO> searchResult = advancedSearchWithHighlight(dto);
        
        // 转换为 PatentListVO
        List<PatentListVO> list = searchResult.getList().stream().map(vo -> {
            PatentListVO listVO = new PatentListVO();
            listVO.setId(vo.getId());
            listVO.setPublicationNo(vo.getPublicationNo());
            listVO.setTitle(vo.getTitle());
            listVO.setApplicant(vo.getApplicant());
            listVO.setPatentAbstract(vo.getPatentAbstract());
            listVO.setPublicationDate(vo.getPublicationDate());
            listVO.setSourceType(vo.getSourceType());
            listVO.setParseStatus(vo.getParseStatus());
            listVO.setCreatedAt(vo.getCreatedAt());
            listVO.setEntities(vo.getEntities());
            listVO.setDomains(vo.getDomains());
            return listVO;
        }).toList();
        
        return PageResult.of(list, searchResult.getTotal(), searchResult.getPageNum(), searchResult.getPageSize());
    }

    // ==================== 深度分页 ====================

    @Override
    public PageResult<SearchResultVO> searchWithSearchAfter(SearchDTO dto) {
        if (!isEsAvailable()) {
            log.warn("ES不可用，深度分页功能无法使用");
            return PageResult.of(Collections.emptyList(), 0L, 1, dto.getPageSize());
        }

        try {
            dto.setUseSearchAfter(true);
            return advancedSearchWithHighlight(dto);
        } catch (Exception e) {
            log.error("深度分页查询失败", e);
            return PageResult.of(Collections.emptyList(), 0L, 1, dto.getPageSize());
        }
    }

    // ==================== 聚合统计 ====================

    @Override
    public Map<String, Object> aggregateDomainStats(String keyword) {
        if (!isEsAvailable()) {
            log.warn("ES不可用，无法执行聚合统计");
            return Collections.emptyMap();
        }

        try {
            NativeQueryBuilder queryBuilder = new NativeQueryBuilder()
                    .withMaxResults(0); // 不返回文档，只返回聚合结果
            
            // 如果有关键词，添加查询条件
            if (StringUtils.hasText(keyword)) {
                queryBuilder.withQuery(Query.of(q -> q.multiMatch(mm -> mm
                        .query(keyword)
                        .fields(List.of("title", "abstract_text", "entities"))
                )));
            } else {
                queryBuilder.withQuery(Query.of(q -> q.matchAll(m -> m)));
            }
            
            // 添加聚合
            queryBuilder.withAggregation("domain_section_stats", Aggregation.of(a -> a
                    .terms(t -> t.field("domain_section").size(10))
            ));
            queryBuilder.withAggregation("publication_year_trend", Aggregation.of(a -> a
                    .dateHistogram(dh -> dh
                            .field("publication_date")
                            .calendarInterval(co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval.Year)
                            .format("yyyy")
                            .minDocCount(0)
                    )
            ));
            
            NativeQuery query = queryBuilder.build();
            SearchHits<PatentDocument> searchHits = elasticsearchOperations.search(query, PatentDocument.class);
            
            Map<String, Object> result = new HashMap<>();
            result.put("totalHits", searchHits.getTotalHits());
            
            // 解析聚合结果
            if (searchHits.getAggregations() != null) {
                ElasticsearchAggregations aggregations = (ElasticsearchAggregations) searchHits.getAggregations();
                
                // 领域部统计
                var domainAggContainer = aggregations.get("domain_section_stats");
                if (domainAggContainer != null) {
                    List<Map<String, Object>> domainStats = new ArrayList<>();
                    domainAggContainer.aggregation().getAggregate().sterms().buckets().array().forEach(bucket -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("section", bucket.key().stringValue());
                        item.put("count", bucket.docCount());
                        domainStats.add(item);
                    });
                    result.put("domainSectionStats", domainStats);
                }
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("领域聚合统计失败", e);
            return Collections.emptyMap();
        }
    }

    @Override
    public List<Map<String, Object>> aggregateTopApplicants(String keyword, int topN) {
        if (!isEsAvailable()) {
            log.warn("ES不可用，无法执行申请人统计");
            return Collections.emptyList();
        }

        try {
            NativeQueryBuilder queryBuilder = new NativeQueryBuilder()
                    .withMaxResults(0);
            
            if (StringUtils.hasText(keyword)) {
                queryBuilder.withQuery(Query.of(q -> q.multiMatch(mm -> mm
                        .query(keyword)
                        .fields(List.of("title", "abstract_text"))
                )));
            } else {
                queryBuilder.withQuery(Query.of(q -> q.matchAll(m -> m)));
            }
            
            queryBuilder.withAggregation("top_applicants", Aggregation.of(a -> a
                    .terms(t -> t.field("applicant").size(topN))
            ));
            
            NativeQuery query = queryBuilder.build();
            SearchHits<PatentDocument> searchHits = elasticsearchOperations.search(query, PatentDocument.class);
            
            List<Map<String, Object>> result = new ArrayList<>();
            
            if (searchHits.getAggregations() != null) {
                ElasticsearchAggregations aggregations = (ElasticsearchAggregations) searchHits.getAggregations();
                var applicantAggContainer = aggregations.get("top_applicants");
                if (applicantAggContainer != null) {
                    applicantAggContainer.aggregation().getAggregate().sterms().buckets().array().forEach(bucket -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("applicant", bucket.key().stringValue());
                        item.put("count", bucket.docCount());
                        result.add(item);
                    });
                }
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("申请人统计失败", e);
            return Collections.emptyList();
        }
    }

    // ==================== 索引管理 ====================

    @Override
    public void syncPatentToEs(Long patentId) {
        if (!isEsAvailable()) {
            log.warn("ES不可用，跳过同步: {}", patentId);
            return;
        }
        
        try {
            Patent patent = patentMapper.selectById(patentId);
            if (patent == null || !"SUCCESS".equals(patent.getParseStatus())) {
                return;
            }

            // 获取实体和领域
            List<PatentEntity> entities = patentEntityMapper.selectByPatentId(patentId);
            List<PatentDomain> domains = patentDomainMapper.selectByPatentId(patentId);

            // 构建ES文档
            PatentDocument doc = new PatentDocument();
            doc.setId(patent.getId().toString());
            doc.setPublicationNo(patent.getPublicationNo());
            doc.setTitle(patent.getTitle());
            doc.setAbstractText(patent.getPatentAbstract());
            doc.setApplicant(patent.getApplicant());
            doc.setPublicationDate(patent.getPublicationDate());
            doc.setSourceType(patent.getSourceType());
            doc.setParseStatus(patent.getParseStatus());
            doc.setCreatedAt(patent.getCreatedAt() != null ? patent.getCreatedAt().toLocalDate() : null);
            doc.setUpdatedAt(patent.getUpdatedAt() != null ? patent.getUpdatedAt().toLocalDate() : null);

            doc.setDomainCodes(domains.stream().map(PatentDomain::getDomainCode).toList());
            doc.setDomainSection(domains.stream()
                    .filter(d -> d.getDomainLevel() == 1)
                    .map(PatentDomain::getDomainCode)
                    .findFirst().orElse(null));

            doc.setEntities(entities.stream().map(PatentEntity::getEntityName).toList());
            doc.setEntityTypes(entities.stream()
                    .map(PatentEntity::getEntityType)
                    .distinct()
                    .toList());

            // 保存到ES
            elasticsearchOperations.save(doc);
            log.info("专利同步到ES成功: {}", patentId);

        } catch (Exception e) {
            log.error("专利同步到ES失败: {}", patentId, e);
        }
    }

    @Override
    public void deleteFromEs(Long patentId) {
        if (!isEsAvailable()) {
            log.warn("ES不可用，跳过删除: {}", patentId);
            return;
        }
        
        try {
            elasticsearchOperations.delete(patentId.toString(), PatentDocument.class);
            log.info("从ES删除专利成功: {}", patentId);
        } catch (Exception e) {
            log.error("从ES删除专利失败: {}", patentId, e);
        }
    }

    @Override
    public void initIndex() {
        if (!isEsAvailable()) {
            log.warn("ES不可用，无法初始化索引");
            return;
        }
        
        try {
            IndexCoordinates indexCoordinates = IndexCoordinates.of(INDEX_NAME);
            
            // 检查索引是否存在
            if (!elasticsearchOperations.indexOps(indexCoordinates).exists()) {
                // 创建索引
                elasticsearchOperations.indexOps(PatentDocument.class).create();
                elasticsearchOperations.indexOps(PatentDocument.class).putMapping();
                log.info("ES索引创建成功: {}", INDEX_NAME);
            } else {
                log.info("ES索引已存在: {}", INDEX_NAME);
            }
        } catch (Exception e) {
            log.error("初始化ES索引失败", e);
        }
    }

    @Override
    public boolean isEsAvailable() {
        if (elasticsearchOperations == null) {
            return false;
        }
        try {
            // 简单的连接检查
            elasticsearchOperations.indexOps(IndexCoordinates.of(INDEX_NAME)).exists();
            return true;
        } catch (Exception e) {
            log.warn("Elasticsearch 连接检查失败: {}", e.getMessage());
            return false;
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 构建高亮配置
     */
    private HighlightQuery buildHighlightQuery() {
        List<HighlightField> highlightFields = List.of(
                new HighlightField("title"),
                new HighlightField("abstract_text"),
                new HighlightField("entities")
        );
        
        HighlightParameters parameters = HighlightParameters.builder()
                .withPreTags(HIGHLIGHT_PRE_TAG)
                .withPostTags(HIGHLIGHT_POST_TAG)
                .withNumberOfFragments(3)
                .withFragmentSize(150)
                .build();
        
        Highlight highlight = new Highlight(parameters, highlightFields);
        
        return new HighlightQuery(highlight, PatentDocument.class);
    }

    /**
     * 配置排序
     */
    private void configureSort(NativeQueryBuilder queryBuilder, SearchDTO dto) {
        String sortField = dto.getSortField();
        Sort.Direction direction = "asc".equalsIgnoreCase(dto.getSortOrder()) 
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        
        if ("_score".equals(sortField)) {
            queryBuilder.withSort(Sort.by(Sort.Direction.DESC, "_score"));
        } else if ("publication_date".equals(sortField) || "publicationDate".equals(sortField)) {
            queryBuilder.withSort(Sort.by(direction, "publication_date"));
        } else if ("created_at".equals(sortField) || "createdAt".equals(sortField)) {
            queryBuilder.withSort(Sort.by(direction, "created_at"));
        } else {
            // 默认按评分排序
            queryBuilder.withSort(Sort.by(Sort.Direction.DESC, "_score"));
        }
        
        // 添加ID作为第二排序键（用于search_after的稳定性）
        queryBuilder.withSort(Sort.by(Sort.Direction.ASC, "id"));
    }

    /**
     * 转换为SearchResultVO（带高亮）
     */
    private SearchResultVO convertToSearchResultVO(SearchHit<PatentDocument> hit) {
        PatentDocument doc = hit.getContent();
        Long patentId = Long.parseLong(doc.getId());
        
        SearchResultVO vo = new SearchResultVO();
        vo.setId(patentId);
        vo.setPublicationNo(doc.getPublicationNo());
        vo.setTitle(doc.getTitle());
        vo.setApplicant(doc.getApplicant());
        vo.setPatentAbstract(doc.getAbstractText());
        vo.setPublicationDate(doc.getPublicationDate());
        vo.setParseStatus(doc.getParseStatus());
        vo.setCreatedAt(doc.getCreatedAt() != null ? doc.getCreatedAt().atStartOfDay() : null);
        vo.setDomainCodes(doc.getDomainCodes());
        vo.setScore(hit.getScore());
        
        // 设置排序值（用于search_after）
        if (hit.getSortValues() != null && !hit.getSortValues().isEmpty()) {
            vo.setSortValues(new ArrayList<>(hit.getSortValues()));
        }
        
        // 设置高亮结果
        Map<String, List<String>> highlightFields = hit.getHighlightFields();
        if (highlightFields != null && !highlightFields.isEmpty()) {
            vo.setHighlights(new HashMap<>(highlightFields));
            
            // 如果标题有高亮，替换原标题
            if (highlightFields.containsKey("title") && !highlightFields.get("title").isEmpty()) {
                vo.setTitle(highlightFields.get("title").get(0));
            }
            // 如果摘要有高亮，合并显示
            if (highlightFields.containsKey("abstract_text") && !highlightFields.get("abstract_text").isEmpty()) {
                vo.setPatentAbstract(String.join("...", highlightFields.get("abstract_text")));
            }
        }
        
        // 加载实体信息
        List<PatentEntity> entities = patentEntityMapper.selectByPatentId(patentId);
        if (entities != null && !entities.isEmpty()) {
            vo.setEntities(entities.stream().map(e -> {
                PatentListVO.EntityVO entityVO = new PatentListVO.EntityVO();
                entityVO.setId(e.getId());
                entityVO.setEntityName(e.getEntityName());
                entityVO.setEntityType(e.getEntityType());
                return entityVO;
            }).toList());
        }
        
        // 加载领域信息
        List<PatentDomain> domains = patentDomainMapper.selectByPatentId(patentId);
        if (domains != null && !domains.isEmpty()) {
            vo.setDomains(domains.stream().map(d -> {
                PatentListVO.DomainVO domainVO = new PatentListVO.DomainVO();
                domainVO.setId(d.getId());
                domainVO.setDomainCode(d.getDomainCode());
                domainVO.setDomainDesc(d.getDomainDesc());
                domainVO.setDomainLevel(d.getDomainLevel());
                return domainVO;
            }).toList());
        }
        
        return vo;
    }

    /**
     * 将PatentListVO PageResult转换为SearchResultVO PageResult
     */
    private PageResult<SearchResultVO> convertToSearchResultVO(PageResult<PatentListVO> source) {
        List<SearchResultVO> list = source.getList().stream().map(vo -> {
            SearchResultVO result = new SearchResultVO();
            result.setId(vo.getId());
            result.setPublicationNo(vo.getPublicationNo());
            result.setTitle(vo.getTitle());
            result.setApplicant(vo.getApplicant());
            result.setPatentAbstract(vo.getPatentAbstract());
            result.setPublicationDate(vo.getPublicationDate());
            result.setSourceType(vo.getSourceType());
            result.setParseStatus(vo.getParseStatus());
            result.setCreatedAt(vo.getCreatedAt());
            result.setEntities(vo.getEntities());
            result.setDomains(vo.getDomains());
            return result;
        }).toList();
        
        return PageResult.of(list, source.getTotal(), source.getPageNum(), source.getPageSize());
    }

    /**
     * 降级MySQL检索（快速检索）
     */
    private PageResult<PatentListVO> fallbackSearch(String keyword, Integer pageNum, Integer pageSize) {
        log.warn("ES不可用，降级到MySQL检索");
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<PatentListVO> page = 
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize);
        var result = patentMapper.selectPatentPage(page, null, keyword, null);
        
        // 填充实体和领域信息
        List<PatentListVO> records = result.getRecords();
        if (records != null) {
            for (PatentListVO vo : records) {
                fillEntitiesAndDomains(vo);
            }
        }
        
        return PageResult.of(records, result.getTotal(), pageNum, pageSize);
    }

    /**
     * 降级MySQL高级检索
     */
    private PageResult<PatentListVO> fallbackAdvancedSearch(SearchDTO dto) {
        log.warn("ES不可用，降级到MySQL高级检索");
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<PatentListVO> page = 
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(dto.getPageNum(), dto.getPageSize());
        
        var result = patentMapper.selectPatentAdvanced(
                page,
                dto.getTitle(),
                dto.getAbstractKeyword(),
                dto.getApplicant(),
                dto.getDomainCode(),
                dto.getEntityType()
        );
        
        // 填充实体和领域信息
        List<PatentListVO> records = result.getRecords();
        if (records != null) {
            for (PatentListVO vo : records) {
                fillEntitiesAndDomains(vo);
            }
        }
        
        return PageResult.of(records, result.getTotal(), dto.getPageNum(), dto.getPageSize());
    }
    
    /**
     * 填充实体和领域信息
     */
    private void fillEntitiesAndDomains(PatentListVO vo) {
        if (vo == null || vo.getId() == null) {
            return;
        }
        
        // 加载实体信息
        List<PatentEntity> entities = patentEntityMapper.selectByPatentId(vo.getId());
        if (entities != null && !entities.isEmpty()) {
            vo.setEntities(entities.stream().map(e -> {
                PatentListVO.EntityVO entityVO = new PatentListVO.EntityVO();
                entityVO.setId(e.getId());
                entityVO.setEntityName(e.getEntityName());
                entityVO.setEntityType(e.getEntityType());
                return entityVO;
            }).toList());
        }
        
        // 加载领域信息
        List<PatentDomain> domains = patentDomainMapper.selectByPatentId(vo.getId());
        if (domains != null && !domains.isEmpty()) {
            vo.setDomains(domains.stream().map(d -> {
                PatentListVO.DomainVO domainVO = new PatentListVO.DomainVO();
                domainVO.setId(d.getId());
                domainVO.setDomainCode(d.getDomainCode());
                domainVO.setDomainDesc(d.getDomainDesc());
                domainVO.setDomainLevel(d.getDomainLevel());
                return domainVO;
            }).toList());
        }
    }
}
