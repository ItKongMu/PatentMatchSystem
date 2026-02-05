package com.patent.service;

import com.patent.common.PageResult;
import com.patent.model.dto.SearchDTO;
import com.patent.model.vo.PatentListVO;
import com.patent.model.vo.SearchResultVO;

import java.util.List;
import java.util.Map;

/**
 * 检索服务接口（Elasticsearch）
 */
public interface SearchService {

    /**
     * 快速检索（关键词检索，支持多字段匹配和高亮）
     *
     * @param keyword  关键词
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 检索结果（包含高亮）
     */
    PageResult<SearchResultVO> quickSearch(String keyword, Integer pageNum, Integer pageSize);

    /**
     * 关键词检索（兼容旧接口）
     *
     * @param keyword  关键词
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 检索结果
     */
    PageResult<PatentListVO> searchByKeyword(String keyword, Integer pageNum, Integer pageSize);

    /**
     * 高级检索（支持多条件组合、过滤、高亮）
     *
     * @param dto 检索条件
     * @return 检索结果
     */
    PageResult<SearchResultVO> advancedSearchWithHighlight(SearchDTO dto);

    /**
     * 高级检索（兼容旧接口）
     *
     * @param dto 检索条件
     * @return 检索结果
     */
    PageResult<PatentListVO> advancedSearch(SearchDTO dto);

    /**
     * 深度分页检索（使用search_after）
     *
     * @param dto 检索条件（需包含searchAfter参数）
     * @return 检索结果
     */
    PageResult<SearchResultVO> searchWithSearchAfter(SearchDTO dto);

    /**
     * 聚合统计 - 领域分布
     *
     * @param keyword 检索关键词（可选）
     * @return 领域统计结果
     */
    Map<String, Object> aggregateDomainStats(String keyword);

    /**
     * 聚合统计 - 申请人排行
     *
     * @param keyword 检索关键词（可选）
     * @param topN    返回前N名
     * @return 申请人统计结果
     */
    List<Map<String, Object>> aggregateTopApplicants(String keyword, int topN);

    /**
     * 同步专利到ES索引
     *
     * @param patentId 专利ID
     */
    void syncPatentToEs(Long patentId);

    /**
     * 批量同步专利到ES索引
     *
     * @param patentIds 专利ID列表
     * @return 成功同步的数量
     */
    int batchSyncPatentsToEs(java.util.List<Long> patentIds);

    /**
     * 从ES删除专利
     *
     * @param patentId 专利ID
     */
    void deleteFromEs(Long patentId);

    /**
     * 初始化ES索引（创建索引和映射）
     */
    void initIndex();

    /**
     * 检查ES是否可用
     *
     * @return true-可用，false-不可用
     */
    boolean isEsAvailable();
}
