package com.patent.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.patent.common.PageResult;
import com.patent.common.Result;
import com.patent.model.dto.FavoriteDTO;
import com.patent.model.vo.PatentListVO;
import com.patent.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 专利收藏控制器
 */
@Tag(name = "专利收藏", description = "专利收藏夹管理接口")
@RestController
@RequestMapping("/api/favorite")
@RequiredArgsConstructor
@SaCheckLogin
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "添加收藏")
    @PostMapping
    public Result<Long> addFavorite(@Valid @RequestBody FavoriteDTO dto) {
        Long id = favoriteService.addFavorite(dto);
        return Result.success("收藏成功", id);
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/{patentId}")
    public Result<Void> removeFavorite(@PathVariable("patentId") Long patentId) {
        favoriteService.removeFavorite(patentId);
        return Result.success("取消收藏成功", null);
    }

    @Operation(summary = "检查是否已收藏")
    @GetMapping("/check/{patentId}")
    public Result<Boolean> checkFavorite(@PathVariable("patentId") Long patentId) {
        return Result.success(favoriteService.isFavorite(patentId));
    }

    @Operation(summary = "批量检查是否已收藏")
    @PostMapping("/check/batch")
    public Result<Set<Long>> batchCheckFavorite(@RequestBody List<Long> patentIds) {
        return Result.success(favoriteService.batchCheckFavorite(patentIds));
    }

    @Operation(summary = "获取收藏列表")
    @GetMapping("/list")
    public Result<PageResult<PatentListVO>> getFavoriteList(
            @Parameter(description = "页码") @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @Parameter(description = "关键词") @RequestParam(value = "keyword", required = false) String keyword,
            @Parameter(description = "分组名称") @RequestParam(value = "groupName", required = false) String groupName) {
        return Result.success(favoriteService.getFavoriteList(pageNum, pageSize, keyword, groupName));
    }

    @Operation(summary = "获取收藏夹分组列表")
    @GetMapping("/groups")
    public Result<List<String>> getFavoriteGroups() {
        return Result.success(favoriteService.getFavoriteGroups());
    }

    @Operation(summary = "更新收藏信息")
    @PutMapping("/{patentId}")
    public Result<Void> updateFavorite(
            @PathVariable("patentId") Long patentId,
            @RequestBody Map<String, String> params) {
        favoriteService.updateFavorite(patentId, params.get("remark"), params.get("groupName"));
        return Result.success("更新成功", null);
    }

    @Operation(summary = "获取收藏数量")
    @GetMapping("/count")
    public Result<Integer> getFavoriteCount() {
        return Result.success(favoriteService.getFavoriteCount());
    }
}
