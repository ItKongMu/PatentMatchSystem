package com.patent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.patent.model.entity.Patent;
import com.patent.model.vo.PatentListVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 专利Mapper
 */
@Mapper
public interface PatentMapper extends BaseMapper<Patent> {

    /**
     * 更新解析状态
     */
    @Update("UPDATE patent SET parse_status = #{status}, parse_error = #{error}, updated_at = NOW() WHERE id = #{id}")
    int updateParseStatus(@Param("id") Long id, @Param("status") String status, @Param("error") String error);

    /**
     * 根据公开号查询专利
     */
    @Select("SELECT * FROM patent WHERE publication_no = #{publicationNo}")
    Patent selectByPublicationNo(@Param("publicationNo") String publicationNo);

    /**
     * 分页查询专利列表
     */
    IPage<PatentListVO> selectPatentPage(Page<PatentListVO> page, 
                                          @Param("parseStatus") String parseStatus,
                                          @Param("keyword") String keyword,
                                          @Param("userId") Long userId);

    /**
     * 高级检索专利（支持多条件）
     */
    IPage<PatentListVO> selectPatentAdvanced(Page<PatentListVO> page,
                                              @Param("title") String title,
                                              @Param("abstractKeyword") String abstractKeyword,
                                              @Param("applicant") String applicant,
                                              @Param("domainCode") String domainCode,
                                              @Param("entityType") String entityType);
}
