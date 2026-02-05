package com.patent.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.patent.common.PageResult;
import com.patent.common.Result;
import com.patent.model.dto.MatchQueryDTO;
import com.patent.model.entity.MatchRecord;
import com.patent.model.vo.MatchResultVO;
import com.patent.service.MatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 匹配控制器
 */
@Tag(name = "技术匹配", description = "LLM技术匹配、相似专利匹配等接口")
@RestController
@RequestMapping("/api/match")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @Operation(summary = "文本查询技术匹配")
    @SaCheckLogin
    @PostMapping
    public Result<MatchResultVO> matchByText(@Valid @RequestBody MatchQueryDTO dto) {
        return Result.success(matchService.matchByText(dto));
    }

    @Operation(summary = "相似专利匹配")
    @SaCheckLogin
    @PostMapping("/patent/{id}")
    public Result<MatchResultVO> matchByPatent(
            @PathVariable("id") Long id,
            @Parameter(description = "返回数量") @RequestParam(value = "topK", defaultValue = "10") Integer topK) {
        return Result.success(matchService.matchByPatent(id, topK));
    }

    @Operation(summary = "查询匹配历史记录")
    @SaCheckLogin
    @GetMapping("/history")
    public Result<PageResult<MatchRecord>> getMatchHistory(
            @Parameter(description = "页码") @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @Parameter(description = "匹配模式") @RequestParam(value = "matchMode", required = false) String matchMode) {
        return Result.success(matchService.getMatchHistory(pageNum, pageSize, matchMode));
    }
}
