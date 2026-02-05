package com.patent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.patent.model.entity.PatentFavorite;
import com.patent.model.vo.PatentListVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 专利收藏Mapper
 */
@Mapper
public interface PatentFavoriteMapper extends BaseMapper<PatentFavorite> {

    /**
     * 查询用户是否已收藏某专利
     */
    @Select("SELECT * FROM patent_favorite WHERE user_id = #{userId} AND patent_id = #{patentId}")
    PatentFavorite selectByUserAndPatent(@Param("userId") Long userId, @Param("patentId") Long patentId);

    /**
     * 删除用户对某专利的收藏
     */
    @Delete("DELETE FROM patent_favorite WHERE user_id = #{userId} AND patent_id = #{patentId}")
    int deleteByUserAndPatent(@Param("userId") Long userId, @Param("patentId") Long patentId);

    /**
     * 查询用户的收藏专利ID列表
     */
    @Select("SELECT patent_id FROM patent_favorite WHERE user_id = #{userId}")
    List<Long> selectPatentIdsByUserId(@Param("userId") Long userId);

    /**
     * 分页查询用户收藏的专利列表
     */
    IPage<PatentListVO> selectFavoritePatentPage(Page<PatentListVO> page,
                                                  @Param("userId") Long userId,
                                                  @Param("keyword") String keyword,
                                                  @Param("groupName") String groupName);

    /**
     * 查询用户的收藏夹分组列表
     */
    @Select("SELECT DISTINCT group_name FROM patent_favorite WHERE user_id = #{userId} AND group_name IS NOT NULL AND group_name != ''")
    List<String> selectGroupsByUserId(@Param("userId") Long userId);

    /**
     * 统计用户收藏数量
     */
    @Select("SELECT COUNT(*) FROM patent_favorite WHERE user_id = #{userId}")
    int countByUserId(@Param("userId") Long userId);
}
