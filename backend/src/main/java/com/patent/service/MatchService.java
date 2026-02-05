package com.patent.service;

import com.patent.common.PageResult;
import com.patent.model.dto.MatchQueryDTO;
import com.patent.model.entity.MatchRecord;
import com.patent.model.vo.MatchResultVO;

/**
 * 匹配服务接口
 */
public interface MatchService {

    /**
     * 文本查询技术匹配
     *
     * @param dto 查询参数
     * @return 匹配结果
     */
    MatchResultVO matchByText(MatchQueryDTO dto);

    /**
     * 专利相似匹配
     *
     * @param patentId 源专利ID
     * @param topK     返回数量
     * @return 匹配结果
     */
    MatchResultVO matchByPatent(Long patentId, Integer topK);

    /**
     * 查询匹配历史记录
     *
     * @param pageNum   页码
     * @param pageSize  每页大小
     * @param matchMode 匹配模式（可选）
     * @return 分页结果
     */
    PageResult<MatchRecord> getMatchHistory(Integer pageNum, Integer pageSize, String matchMode);
}
