package com.patent.controller;

import com.patent.common.PageResult;
import com.patent.common.Result;
import com.patent.model.dto.SearchDTO;
import com.patent.model.vo.PatentListVO;
import com.patent.model.vo.SearchResultVO;
import com.patent.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 检索控制器
 * 
 * <p>提供以下检索能力：
 * <ul>
 *   <li>快速检索：多字段匹配 + 高亮显示</li>
 *   <li>高级检索：多条件组合 + 过滤 + 排序</li>
 *   <li>深度分页：search_after 实现</li>
 *   <li>聚合统计：领域分布、申请人排行</li>
 * </ul>
 */
@Tag(name = "专利检索", description = "关键词检索、高级检索、聚合统计等接口")
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    // ==================== 快速检索 ====================

    @Operation(summary = "快速检索（推荐）", 
               description = "支持多字段匹配、短语提升、高亮显示，适用于首页搜索框")
    @GetMapping("/quick")
    public Result<PageResult<SearchResultVO>> quickSearch(
            @Parameter(description = "关键词", required = true) 
            @RequestParam("keyword") String keyword,
            @Parameter(description = "页码") 
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") 
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        return Result.success(searchService.quickSearch(keyword, pageNum, pageSize));
    }

    @Operation(summary = "关键词检索（兼容旧接口）")
    @GetMapping
    public Result<PageResult<PatentListVO>> searchByKeyword(
            @Parameter(description = "关键词", required = true) 
            @RequestParam("keyword") String keyword,
            @Parameter(description = "页码") 
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") 
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        return Result.success(searchService.searchByKeyword(keyword, pageNum, pageSize));
    }

    // ==================== 高级检索 ====================

    @Operation(summary = "高级检索（推荐）", 
               description = "支持多条件组合、日期范围、领域过滤、实体类型筛选、高亮显示")
    @PostMapping("/advanced/v2")
    public Result<PageResult<SearchResultVO>> advancedSearchV2(@RequestBody SearchDTO dto) {
        return Result.success(searchService.advancedSearchWithHighlight(dto));
    }

    @Operation(summary = "高级检索（兼容旧接口）")
    @PostMapping("/advanced")
    public Result<PageResult<PatentListVO>> advancedSearch(@RequestBody SearchDTO dto) {
        return Result.success(searchService.advancedSearch(dto));
    }

    // ==================== 深度分页 ====================

    @Operation(summary = "深度分页检索", 
               description = "使用search_after实现深度分页，避免from+size超过10000的限制。" +
                            "首次请求不传searchAfter，后续请求传入上次返回的sortValues")
    @PostMapping("/scroll")
    public Result<PageResult<SearchResultVO>> searchWithSearchAfter(@RequestBody SearchDTO dto) {
        return Result.success(searchService.searchWithSearchAfter(dto));
    }

    // ==================== 聚合统计 ====================

    @Operation(summary = "领域分布统计", 
               description = "统计各IPC领域部的专利数量分布")
    @GetMapping("/stats/domain")
    public Result<Map<String, Object>> aggregateDomainStats(
            @Parameter(description = "检索关键词（可选）") 
            @RequestParam(value = "keyword", required = false) String keyword) {
        return Result.success(searchService.aggregateDomainStats(keyword));
    }

    @Operation(summary = "申请人排行统计", 
               description = "统计专利数量最多的申请人排行")
    @GetMapping("/stats/applicants")
    public Result<List<Map<String, Object>>> aggregateTopApplicants(
            @Parameter(description = "检索关键词（可选）") 
            @RequestParam(value = "keyword", required = false) String keyword,
            @Parameter(description = "返回前N名", example = "10") 
            @RequestParam(value = "topN", defaultValue = "10") int topN) {
        return Result.success(searchService.aggregateTopApplicants(keyword, topN));
    }

    // ==================== 系统管理 ====================

    @Operation(summary = "检查ES可用性")
    @GetMapping("/health")
    public Result<Boolean> checkEsHealth() {
        return Result.success(searchService.isEsAvailable());
    }

    @Operation(summary = "初始化ES索引", description = "创建索引和映射，幂等操作")
    @PostMapping("/init-index")
    public Result<Void> initIndex() {
        searchService.initIndex();
        return Result.success();
    }
}
