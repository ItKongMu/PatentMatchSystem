package com.patent.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.patent.common.PageResult;
import com.patent.common.Result;
import com.patent.model.dto.MatchQueryDTO;
import com.patent.model.vo.MatchSessionVO;
import com.patent.model.vo.MatchTaskVO;
import com.patent.service.MatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 匹配控制器
 */
@Tag(name = "技术匹配", description = "LLM技术匹配、相似专利匹配等接口")
@RestController
@RequestMapping("/api/match")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @Operation(summary = "提交文本查询技术匹配（异步）")
    @SaCheckLogin
    @PostMapping
    public Result<Map<String, String>> matchByText(@Valid @RequestBody MatchQueryDTO dto) {
        String sessionId = matchService.submitTextMatch(dto);
        return Result.success(Map.of("sessionId", sessionId));
    }

    @Operation(summary = "提交相似专利匹配（异步）")
    @SaCheckLogin
    @PostMapping("/patent/{id}")
    public Result<Map<String, String>> matchByPatent(
            @PathVariable("id") Long id,
            @Parameter(description = "返回数量") @RequestParam(value = "topK", defaultValue = "10") Integer topK) {
        String sessionId = matchService.submitPatentMatch(id, topK);
        return Result.success(Map.of("sessionId", sessionId));
    }

    @Operation(summary = "轮询匹配任务状态")
    @SaCheckLogin
    @GetMapping("/task/{sessionId}")
    public Result<MatchTaskVO> getTaskStatus(@PathVariable("sessionId") String sessionId) {
        return Result.success(matchService.getTaskStatus(sessionId));
    }

    @Operation(summary = "查询匹配历史（按session聚合）")
    @SaCheckLogin
    @GetMapping("/history")
    public Result<PageResult<MatchSessionVO>> getMatchHistory(
            @Parameter(description = "页码") @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @Parameter(description = "匹配模式") @RequestParam(value = "matchMode", required = false) String matchMode) {
        return Result.success(matchService.getMatchHistory(pageNum, pageSize, matchMode));
    }

    @Operation(summary = "查询某session下所有匹配专利（含查询实体和详细信息）")
    @SaCheckLogin
    @GetMapping("/history/{sessionId}")
    public Result<MatchSessionVO.SessionDetailVO> getSessionDetails(
            @PathVariable("sessionId") String sessionId) {
        return Result.success(matchService.getSessionDetails(sessionId));
    }
}
