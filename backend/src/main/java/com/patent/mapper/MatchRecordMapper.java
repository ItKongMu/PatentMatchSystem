package com.patent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.patent.model.entity.MatchRecord;
import com.patent.model.vo.MatchSessionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 匹配记录Mapper
 */
@Mapper
public interface MatchRecordMapper extends BaseMapper<MatchRecord> {

    /**
     * 统计用户的匹配session总数（去重）
     */
    long countSessionByUser(@Param("userId") Long userId,
                             @Param("matchMode") String matchMode);

    /**
     * 按session聚合分页查询历史记录（每个session一行）
     */
    List<MatchSessionVO> selectSessionList(@Param("userId") Long userId,
                                            @Param("matchMode") String matchMode,
                                            @Param("offset") int offset,
                                            @Param("limit") int limit);

    /**
     * 查询某session下的前N条匹配专利（用于预览）
     */
    List<MatchSessionVO.MatchItemSummary> selectTopMatchesBySession(@Param("sessionId") String sessionId,
                                                                     @Param("userId") Long userId,
                                                                     @Param("limit") int limit);

    /**
     * 查询某个session下的所有匹配记录（按相似度降序）
     */
    List<MatchRecord> selectBySessionId(@Param("sessionId") String sessionId,
                                         @Param("userId") Long userId);
}
