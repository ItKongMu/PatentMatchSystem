package com.patent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.patent.model.entity.PatentEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 专利实体Mapper
 */
@Mapper
public interface PatentEntityMapper extends BaseMapper<PatentEntity> {

    /**
     * 根据专利ID查询实体列表
     */
    @Select("SELECT * FROM patent_entity WHERE patent_id = #{patentId}")
    List<PatentEntity> selectByPatentId(@Param("patentId") Long patentId);

    /**
     * 根据专利ID删除实体
     */
    @Delete("DELETE FROM patent_entity WHERE patent_id = #{patentId}")
    int deleteByPatentId(@Param("patentId") Long patentId);
}
