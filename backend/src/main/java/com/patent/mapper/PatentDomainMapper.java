package com.patent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.patent.model.entity.PatentDomain;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 专利领域Mapper
 */
@Mapper
public interface PatentDomainMapper extends BaseMapper<PatentDomain> {

    /**
     * 根据专利ID查询领域列表
     */
    @Select("SELECT * FROM patent_domain WHERE patent_id = #{patentId} ORDER BY domain_level")
    List<PatentDomain> selectByPatentId(@Param("patentId") Long patentId);

    /**
     * 根据专利ID删除领域
     */
    @Delete("DELETE FROM patent_domain WHERE patent_id = #{patentId}")
    int deleteByPatentId(@Param("patentId") Long patentId);
}
