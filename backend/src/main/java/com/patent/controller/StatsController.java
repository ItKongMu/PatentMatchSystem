package com.patent.controller;

import com.patent.common.Result;
import com.patent.model.vo.StatsVO;
import com.patent.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 统计分析控制器
 * 
 * <p>提供以下统计功能：
 * <ul>
 *   <li>系统概览：专利数、实体数、领域数等</li>
 *   <li>实体词云：高频实体词汇统计</li>
 *   <li>领域分布：IPC分类统计</li>
 *   <li>专利趋势：按年份的专利数量趋势</li>
 *   <li>申请人排行：专利申请数量排行</li>
 * </ul>
 */
@Tag(name = "统计分析", description = "数据可视化统计接口")
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    // ==================== 概览统计 ====================

    @Operation(summary = "获取系统概览统计", description = "返回专利总数、实体总数、领域总数等概览数据")
    @GetMapping("/overview")
    public Result<StatsVO.OverviewVO> getOverviewStats() {
        return Result.success(statsService.getOverviewStats());
    }

    // ==================== 实体统计 ====================

    @Operation(summary = "获取实体词云数据", description = "返回高频实体词汇及其出现次数，用于词云可视化")
    @GetMapping("/entity/wordcloud")
    public Result<StatsVO.WordCloudVO> getEntityWordCloud(
            @Parameter(description = "返回前N个高频实体", example = "100")
            @RequestParam(value = "topN", defaultValue = "100") int topN) {
        return Result.success(statsService.getEntityWordCloud(topN));
    }

    @Operation(summary = "获取实体类型分布", description = "返回各实体类型的数量统计")
    @GetMapping("/entity/types")
    public Result<List<StatsVO.EntityTypeStatVO>> getEntityTypeStats() {
        return Result.success(statsService.getEntityTypeStats());
    }

    // ==================== 领域统计 ====================

    @Operation(summary = "获取领域分布统计", description = "返回IPC部级领域的专利数量分布，用于饼图可视化")
    @GetMapping("/domain/distribution")
    public Result<List<StatsVO.DomainStatVO>> getDomainStats() {
        return Result.success(statsService.getDomainSectionStats());
    }

    // ==================== 趋势统计 ====================

    @Operation(summary = "获取专利申请趋势", description = "返回按年份的专利数量统计，用于折线图可视化")
    @GetMapping("/trend")
    public Result<List<StatsVO.TrendStatVO>> getPatentTrend(
            @Parameter(description = "统计最近N年的数据", example = "10")
            @RequestParam(value = "years", defaultValue = "10") int years) {
        return Result.success(statsService.getPatentTrend(years));
    }

    // ==================== 排行统计 ====================

    @Operation(summary = "获取申请人排行", description = "返回专利申请数量最多的申请人排行")
    @GetMapping("/applicants/top")
    public Result<List<StatsVO.EntityStatVO>> getTopApplicants(
            @Parameter(description = "返回前N名", example = "10")
            @RequestParam(value = "topN", defaultValue = "10") int topN) {
        return Result.success(statsService.getTopApplicants(topN));
    }

    // ==================== 缓存管理 ====================

    @Operation(summary = "刷新统计缓存", description = "清除所有统计数据缓存，下次请求时重新计算")
    @PostMapping("/cache/refresh")
    public Result<Void> refreshCache() {
        statsService.evictAllStatsCache();
        return Result.success();
    }
}
