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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

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

    /**
     * 下载图谱 CSV 模板（管理员用于全量导入）
     */
    @Operation(summary = "下载 CSV 模板", description = "下载图谱 CSV 导入模板，用于 neo4j-admin 全量导入")
    @GetMapping("/csv-template")
    public ResponseEntity<byte[]> downloadCsvTemplate() {
        String template = buildCsvTemplate();
        byte[] bytes = template.getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", "graph_import_template.csv");
        headers.setContentLength(bytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(bytes);
    }

    /**
     * 构建 CSV 模板内容
     */
    private String buildCsvTemplate() {
        StringBuilder sb = new StringBuilder();
        sb.append("# 专利知识图谱 CSV 导入模板\n");
        sb.append("# 使用 neo4j-admin database import full 命令导入\n\n");

        sb.append("# === patent_nodes.csv ===\n");
        sb.append("publication_no:ID(Patent),title,abstract_text,applicant,publication_date,domain_codes:string[],source_type,created_at\n");
        sb.append("CN123456789A,一种图像识别方法,专利摘要...,华为技术有限公司,2024-05-20,G06F16/30;G06F17/00,FILE,2024-05-25T09:00:00\n\n");

        sb.append("# === ipc_nodes.csv ===\n");
        sb.append("ipc_code:ID(IPC),name,level,parent_code\n");
        sb.append("G06F16/30,信息检索,5,G06F16\n\n");

        sb.append("# === entity_nodes.csv ===\n");
        sb.append("entity_name:ID(Entity),entity_type,alias,source\n");
        sb.append("图像传感器,PRODUCT,图像传感元件,LLM\n\n");

        sb.append("# === applicant_nodes.csv ===\n");
        sb.append("applicant_name:ID(Applicant)\n");
        sb.append("华为技术有限公司\n\n");

        sb.append("# === has_ipc_rels.csv ===\n");
        sb.append(":START_ID(Patent),:END_ID(IPC)\n");
        sb.append("CN123456789A,G06F16/30\n\n");

        sb.append("# === mentions_rels.csv ===\n");
        sb.append(":START_ID(Patent),:END_ID(Entity),confidence\n");
        sb.append("CN123456789A,图像传感器,0.91\n\n");

        sb.append("# === filed_by_rels.csv ===\n");
        sb.append(":START_ID(Patent),:END_ID(Applicant)\n");
        sb.append("CN123456789A,华为技术有限公司\n\n");

        sb.append("# === ipc_parent_rels.csv ===\n");
        sb.append(":START_ID(IPC),:END_ID(IPC)\n");
        sb.append("G06F16/30,G06F16\n\n");

        sb.append("# === 导入命令 ===\n");
        sb.append("# neo4j-admin database import full \\\n");
        sb.append("#   --nodes=Patent=patent_nodes.csv \\\n");
        sb.append("#   --nodes=IPC=ipc_nodes.csv \\\n");
        sb.append("#   --nodes=Entity=entity_nodes.csv \\\n");
        sb.append("#   --nodes=Applicant=applicant_nodes.csv \\\n");
        sb.append("#   --relationships=HAS_IPC=has_ipc_rels.csv \\\n");
        sb.append("#   --relationships=MENTIONS=mentions_rels.csv \\\n");
        sb.append("#   --relationships=FILED_BY=filed_by_rels.csv \\\n");
        sb.append("#   --relationships=PARENT_OF=ipc_parent_rels.csv \\\n");
        sb.append("#   neo4j\n");

        return sb.toString();
    }
}
