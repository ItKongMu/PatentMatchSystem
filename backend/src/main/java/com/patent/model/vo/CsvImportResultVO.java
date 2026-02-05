package com.patent.model.vo;

import com.patent.model.dto.PatentCsvDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * CSV导入结果VO
 */
@Data
@Schema(description = "CSV导入结果")
public class CsvImportResultVO {

    @Schema(description = "总行数")
    private Integer totalRows = 0;

    @Schema(description = "成功导入数量")
    private Integer successCount = 0;

    @Schema(description = "失败数量")
    private Integer failedCount = 0;

    @Schema(description = "跳过数量（如重复）")
    private Integer skippedCount = 0;

    @Schema(description = "成功导入的专利ID列表")
    private List<Long> importedPatentIds = new ArrayList<>();

    @Schema(description = "失败的行数据")
    private List<PatentCsvDTO> failedRows = new ArrayList<>();

    @Schema(description = "导入状态消息")
    private String message;

    /**
     * 添加成功的专利ID
     */
    public void addSuccess(Long patentId) {
        this.successCount++;
        this.importedPatentIds.add(patentId);
    }

    /**
     * 添加失败的行
     */
    public void addFailed(PatentCsvDTO row) {
        this.failedCount++;
        this.failedRows.add(row);
    }

    /**
     * 添加跳过的行
     */
    public void addSkipped() {
        this.skippedCount++;
    }

    /**
     * 构建结果消息
     */
    public void buildMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("导入完成：");
        sb.append("总计").append(totalRows).append("条，");
        sb.append("成功").append(successCount).append("条");
        if (failedCount > 0) {
            sb.append("，失败").append(failedCount).append("条");
        }
        if (skippedCount > 0) {
            sb.append("，跳过").append(skippedCount).append("条");
        }
        this.message = sb.toString();
    }
}
