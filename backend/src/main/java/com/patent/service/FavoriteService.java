package com.patent.service;

import com.patent.common.PageResult;
import com.patent.model.dto.FavoriteDTO;
import com.patent.model.vo.FavoriteVO;
import com.patent.model.vo.PatentListVO;

import java.util.List;
import java.util.Set;

/**
 * 专利收藏服务接口
 */
public interface FavoriteService {

    /**
     * 添加收藏
     *
     * @param dto 收藏信息
     * @return 收藏ID
     */
    Long addFavorite(FavoriteDTO dto);

    /**
     * 取消收藏
     *
     * @param patentId 专利ID
     */
    void removeFavorite(Long patentId);

    /**
     * 检查是否已收藏
     *
     * @param patentId 专利ID
     * @return 是否已收藏
     */
    boolean isFavorite(Long patentId);

    /**
     * 批量检查是否已收藏
     *
     * @param patentIds 专利ID列表
     * @return 已收藏的专利ID集合
     */
    Set<Long> batchCheckFavorite(List<Long> patentIds);

    /**
     * 获取用户收藏的专利列表
     *
     * @param pageNum   页码
     * @param pageSize  每页大小
     * @param keyword   关键词（可选）
     * @param groupName 分组名称（可选）
     * @return 分页结果
     */
    PageResult<PatentListVO> getFavoriteList(Integer pageNum, Integer pageSize, String keyword, String groupName);

    /**
     * 获取用户的收藏夹分组列表
     *
     * @return 分组名称列表
     */
    List<String> getFavoriteGroups();

    /**
     * 更新收藏信息（备注、分组）
     *
     * @param patentId  专利ID
     * @param remark    备注
     * @param groupName 分组名称
     */
    void updateFavorite(Long patentId, String remark, String groupName);

    /**
     * 获取用户收藏数量
     *
     * @return 收藏数量
     */
    int getFavoriteCount();
}
