package com.patent.service;

import com.patent.common.PageResult;
import com.patent.model.dto.MatchQueryDTO;
import com.patent.model.vo.MatchResultVO;
import com.patent.model.vo.MatchSessionVO;
import com.patent.model.vo.MatchTaskVO;

import java.util.List;

/**
 * 匹配服务接口
 */
public interface MatchService {

    /**
     * 同步文本查询技术匹配，直接返回匹配结果（供 Chat 工具调用等场景使用）
     *
     * @param dto 查询参数
     * @return 匹配结果VO
     */
    MatchResultVO matchByText(MatchQueryDTO dto);

    /**
     * 异步文本查询技术匹配，立即返回 sessionId
     *
     * @param dto 查询参数
     * @return sessionId
     */
    String submitTextMatch(MatchQueryDTO dto);

    /**
     * 异步专利相似匹配，立即返回 sessionId
     *
     * @param patentId 源专利ID
     * @param topK     返回数量
     * @return sessionId
     */
    String submitPatentMatch(Long patentId, Integer topK);

    /**
     * 轮询任务状态
     *
     * @param sessionId 会话ID
     * @return 任务状态VO
     */
    MatchTaskVO getTaskStatus(String sessionId);

    /**
     * 查询匹配历史记录（按session聚合，每个session为一条）
     *
     * @param pageNum   页码
     * @param pageSize  每页大小
     * @param matchMode 匹配模式（可选）
     * @return 分页结果
     */
    PageResult<MatchSessionVO> getMatchHistory(Integer pageNum, Integer pageSize, String matchMode);

    /**
     * 查询某个session下的所有匹配专利详情（包含查询实体+匹配列表）
     *
     * @param sessionId 会话ID
     * @return Session详情VO（含查询实体和匹配列表）
     */
    MatchSessionVO.SessionDetailVO getSessionDetails(String sessionId);
}
