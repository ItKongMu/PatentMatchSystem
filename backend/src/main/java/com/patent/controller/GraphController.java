package com.patent.controller;

import com.patent.common.Result;
import com.patent.model.dto.GraphQueryDTO;
import com.patent.model.vo.GraphVO;
import com.patent.service.GraphService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 知识图谱 Controller
 * 提供图谱查询接口，返回 nodes + links 结构（兼容 ECharts graph）
 */
@Slf4j
@Tag(name = "知识图谱", description = "专利知识图谱查询接口")
@RestController
@RequestMapping("/api/graph")
@RequiredArgsConstructor
public class GraphController {

    private final GraphService graphService;

    /**
     * 查询某专利的邻接图谱
     */
    @Operation(summary = "专利图谱", description = "返回某专利的图谱邻接节点与关系，用于专利详情页可视化")
    @GetMapping("/patent/{publicationNo}")
    public Result<GraphVO> getPatentGraph(
            @Parameter(description = "专利公开号") @PathVariable String publicationNo) {
        GraphVO vo = graphService.getPatentGraph(publicationNo);
        return Result.success(vo);
    }

    /**
     * 查询某实体的关联图谱
     */
    @Operation(summary = "实体图谱", description = "返回某实体的关联专利/方法/组件，用于实体关系图")
    @GetMapping("/entity/{entityName}")
    public Result<GraphVO> getEntityGraph(
            @Parameter(description = "实体名称") @PathVariable String entityName) {
        GraphVO vo = graphService.getEntityGraph(entityName);
        return Result.success(vo);
    }

    /**
     * 查询 IPC 层次图谱
     * 使用 @RequestParam 避免 IPC 编码中的 "/" 被 Tomcat 截断（如 A61K9/70）
     */
    @Operation(summary = "IPC 图谱", description = "返回 IPC 层次结构与关联专利数，用于领域可视化")
    @GetMapping("/ipc")
    public Result<GraphVO> getIpcGraph(
            @Parameter(description = "IPC 编码，如 G06F16/30 或 A61K9/70") @RequestParam String ipcCode) {
        GraphVO vo = graphService.getIpcGraph(ipcCode);
        return Result.success(vo);
    }

    /**
     * 通用图谱查询
     */
    @Operation(summary = "通用图谱查询", description = "支持节点类型、关系类型、深度、过滤条件的通用查询")
    @PostMapping("/query")
    public Result<GraphVO> queryGraph(@RequestBody GraphQueryDTO dto) {
        GraphVO vo = graphService.queryGraph(dto);
        return Result.success(vo);
    }

}
