package com.patent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.patent.model.entity.MatchRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 匹配记录Mapper
 */
@Mapper
public interface MatchRecordMapper extends BaseMapper<MatchRecord> {

    /**
     * 分页查询匹配记录
     */
    IPage<MatchRecord> selectMatchRecordPage(Page<MatchRecord> page,
                                              @Param("userId") Long userId,
                                              @Param("matchMode") String matchMode);
}
