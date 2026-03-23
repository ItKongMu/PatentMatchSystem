package com.patent.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.patent.common.PageResult;
import com.patent.common.Result;
import com.patent.model.dto.PatentCsvDTO;
import com.patent.model.dto.PatentTextDTO;
import com.patent.model.vo.CsvImportResultVO;
import com.patent.model.vo.CsvPreviewVO;
import com.patent.model.vo.PatentListVO;
import com.patent.model.vo.PatentVO;
import com.patent.model.vo.UploadResultVO;
import com.patent.service.PatentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 专利管理控制器
 */
@Tag(name = "专利管理", description = "专利上传、查询、处理等接口")
@RestController
@RequestMapping("/api/patent")
@RequiredArgsConstructor
public class PatentController {

    private final PatentService patentService;

    @Operation(summary = "上传专利PDF文件")
    @SaCheckLogin
    @PostMapping("/upload")
    public Result<UploadResultVO> uploadPatentPdf(
            @Parameter(description = "PDF文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "公开号/专利号") @RequestParam(value = "publicationNo", required = false) String publicationNo) {
        return Result.success("上传成功", patentService.uploadPatentPdf(file, publicationNo));
    }

    @Operation(summary = "文本录入专利")
    @SaCheckLogin
    @PostMapping("/text")
    public Result<UploadResultVO> createPatentFromText(@Valid @RequestBody PatentTextDTO dto) {
        return Result.success("录入成功", patentService.createPatentFromText(dto));
    }

    @Operation(summary = "触发专利处理流程（仅管理员或专利创建者）")
    @SaCheckLogin
    @PostMapping("/process/{id}")
    public Result<Void> processPatent(@PathVariable("id") Long id) {
        patentService.processPatent(id);
        return Result.success("处理任务已提交", null);
    }

    @Operation(summary = "重新处理专利（仅管理员）")
    @SaCheckLogin
    @PostMapping("/reprocess/{id}")
    public Result<Void> reprocessPatent(@PathVariable("id") Long id) {
        patentService.reprocessPatent(id);
        return Result.success("重新处理任务已提交", null);
    }

    @Operation(summary = "获取专利详情")
    @GetMapping("/{id}")
    public Result<PatentVO> getPatentDetail(@PathVariable("id") Long id) {
        return Result.success(patentService.getPatentDetail(id));
    }

    @Operation(summary = "获取专利实体列表")
    @GetMapping("/{id}/entities")
    public Result<PatentVO> getPatentEntities(@PathVariable("id") Long id) {
        PatentVO vo = patentService.getPatentDetail(id);
        PatentVO result = new PatentVO();
        result.setId(vo.getId());
        result.setEntities(vo.getEntities());
        return Result.success(result);
    }

    @Operation(summary = "获取专利领域层次")
    @GetMapping("/{id}/domains")
    public Result<PatentVO> getPatentDomains(@PathVariable("id") Long id) {
        PatentVO vo = patentService.getPatentDetail(id);
        PatentVO result = new PatentVO();
        result.setId(vo.getId());
        result.setDomains(vo.getDomains());
        return Result.success(result);
    }

    @Operation(summary = "获取专利向量信息")
    @GetMapping("/{id}/vector")
    public Result<PatentVO> getPatentVector(@PathVariable("id") Long id) {
        PatentVO vo = patentService.getPatentDetail(id);
        PatentVO result = new PatentVO();
        result.setId(vo.getId());
        result.setVector(vo.getVector());
        return Result.success(result);
    }

    @Operation(summary = "分页查询专利列表")
    @GetMapping("/list")
    public Result<PageResult<PatentListVO>> getPatentList(
            @Parameter(description = "页码") @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @Parameter(description = "解析状态") @RequestParam(value = "parseStatus", required = false) String parseStatus,
            @Parameter(description = "关键词") @RequestParam(value = "keyword", required = false) String keyword) {
        return Result.success(patentService.getPatentList(pageNum, pageSize, parseStatus, keyword));
    }

    @Operation(summary = "删除专利")
    @SaCheckLogin
    @DeleteMapping("/{id}")
    public Result<Void> deletePatent(@PathVariable("id") Long id) {
        patentService.deletePatent(id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "预览CSV文件")
    @SaCheckLogin
    @PostMapping("/csv/preview")
    public Result<CsvPreviewVO> previewCsv(
            @Parameter(description = "CSV文件") @RequestParam("file") MultipartFile file) {
        return Result.success(patentService.previewCsv(file));
    }

    @Operation(summary = "导入CSV文件")
    @SaCheckLogin
    @PostMapping("/csv/import")
    public Result<CsvImportResultVO> importCsv(
            @Parameter(description = "CSV文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "是否自动处理") @RequestParam(value = "autoProcess", defaultValue = "false") Boolean autoProcess) {
        return Result.success("导入完成", patentService.importCsv(file, autoProcess));
    }

    @Operation(summary = "导入预览的CSV数据")
    @SaCheckLogin
    @PostMapping("/csv/import/preview")
    public Result<CsvImportResultVO> importCsvPreviewData(
            @RequestBody List<PatentCsvDTO> dataList,
            @Parameter(description = "是否自动处理") @RequestParam(value = "autoProcess", defaultValue = "false") Boolean autoProcess) {
        return Result.success("导入完成", patentService.importCsvData(dataList, autoProcess));
    }
}
