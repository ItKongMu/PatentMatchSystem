package com.patent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.patent.model.entity.PatentVector;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 专利向量Mapper
 */
@Mapper
public interface PatentVectorMapper extends BaseMapper<PatentVector> {

    /**
     * 根据专利ID查询向量信息
     */
    @Select("SELECT * FROM patent_vector WHERE patent_id = #{patentId}")
    PatentVector selectByPatentId(@Param("patentId") Long patentId);

    /**
     * 根据向量ID查询
     */
    @Select("SELECT * FROM patent_vector WHERE vector_id = #{vectorId}")
    PatentVector selectByVectorId(@Param("vectorId") String vectorId);

    /**
     * 根据专利ID删除向量
     */
    @Delete("DELETE FROM patent_vector WHERE patent_id = #{patentId}")
    int deleteByPatentId(@Param("patentId") Long patentId);
}
