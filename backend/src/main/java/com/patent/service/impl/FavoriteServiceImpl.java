package com.patent.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.patent.common.PageResult;
import com.patent.common.exception.BusinessException;
import com.patent.mapper.PatentFavoriteMapper;
import com.patent.mapper.PatentMapper;
import com.patent.model.dto.FavoriteDTO;
import com.patent.model.entity.Patent;
import com.patent.model.entity.PatentFavorite;
import com.patent.model.vo.FavoriteVO;
import com.patent.model.vo.PatentListVO;
import com.patent.service.AuthService;
import com.patent.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 专利收藏服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final PatentFavoriteMapper favoriteMapper;
    private final PatentMapper patentMapper;
    private final AuthService authService;

    @Override
    @Transactional
    public Long addFavorite(FavoriteDTO dto) {
        Long userId = getCurrentUserId();
        Long patentId = dto.getPatentId();

        // 检查专利是否存在
        Patent patent = patentMapper.selectById(patentId);
        if (patent == null) {
            throw new BusinessException("专利不存在");
        }

        // 检查是否已收藏
        PatentFavorite existing = favoriteMapper.selectByUserAndPatent(userId, patentId);
        if (existing != null) {
            throw new BusinessException("该专利已收藏");
        }

        // 创建收藏记录
        PatentFavorite favorite = new PatentFavorite();
        favorite.setUserId(userId);
        favorite.setPatentId(patentId);
        favorite.setRemark(dto.getRemark());
        favorite.setGroupName(dto.getGroupName());
        favoriteMapper.insert(favorite);

        log.info("用户{}收藏专利{}", userId, patentId);
        return favorite.getId();
    }

    @Override
    @Transactional
    public void removeFavorite(Long patentId) {
        Long userId = getCurrentUserId();
        int deleted = favoriteMapper.deleteByUserAndPatent(userId, patentId);
        if (deleted == 0) {
            throw new BusinessException("未收藏该专利");
        }
        log.info("用户{}取消收藏专利{}", userId, patentId);
    }

    @Override
    public boolean isFavorite(Long patentId) {
        Long userId = getCurrentUserId();
        PatentFavorite favorite = favoriteMapper.selectByUserAndPatent(userId, patentId);
        return favorite != null;
    }

    @Override
    public Set<Long> batchCheckFavorite(List<Long> patentIds) {
        if (patentIds == null || patentIds.isEmpty()) {
            return new HashSet<>();
        }
        Long userId = getCurrentUserId();
        List<Long> favoritePatentIds = favoriteMapper.selectPatentIdsByUserId(userId);
        Set<Long> favoriteSet = new HashSet<>(favoritePatentIds);
        favoriteSet.retainAll(patentIds);
        return favoriteSet;
    }

    @Override
    public PageResult<PatentListVO> getFavoriteList(Integer pageNum, Integer pageSize, String keyword, String groupName) {
        Long userId = getCurrentUserId();
        Page<PatentListVO> page = new Page<>(pageNum, pageSize);
        IPage<PatentListVO> result = favoriteMapper.selectFavoritePatentPage(page, userId, keyword, groupName);
        return PageResult.of(result.getRecords(), result.getTotal(), pageNum, pageSize);
    }

    @Override
    public List<String> getFavoriteGroups() {
        Long userId = getCurrentUserId();
        return favoriteMapper.selectGroupsByUserId(userId);
    }

    @Override
    @Transactional
    public void updateFavorite(Long patentId, String remark, String groupName) {
        Long userId = getCurrentUserId();
        PatentFavorite favorite = favoriteMapper.selectByUserAndPatent(userId, patentId);
        if (favorite == null) {
            throw new BusinessException("未收藏该专利");
        }

        LambdaUpdateWrapper<PatentFavorite> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(PatentFavorite::getId, favorite.getId())
                .set(PatentFavorite::getRemark, remark)
                .set(PatentFavorite::getGroupName, groupName);
        favoriteMapper.update(null, updateWrapper);

        log.info("用户{}更新收藏专利{}的信息", userId, patentId);
    }

    @Override
    public int getFavoriteCount() {
        Long userId = getCurrentUserId();
        return favoriteMapper.countByUserId(userId);
    }

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId() {
        Long userId = authService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException("请先登录");
        }
        return userId;
    }
}
