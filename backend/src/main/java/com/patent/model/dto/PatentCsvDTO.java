package com.patent.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * CSV导入专利DTO
 */
@Data
@Schema(description = "CSV导入专利数据")
public class PatentCsvDTO {

    @Schema(description = "公开号/专利号")
    private String publicationNo;

    @Schema(description = "专利标题")
    private String title;

    @Schema(description = "申请人")
    private String applicant;

    @Schema(description = "公开日期（格式：yyyy-MM-dd）")
    private String publicationDate;

    @Schema(description = "专利摘要")
    private String patentAbstract;

    @Schema(description = "行号（CSV中的行）")
    private Integer rowNum;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "是否有效")
    private Boolean valid = true;

    /**
     * 校验数据
     */
    public void validate() {
        if (title == null || title.trim().isEmpty()) {
            this.valid = false;
            this.errorMessage = "专利标题不能为空";
        } else if (patentAbstract == null || patentAbstract.trim().isEmpty()) {
            this.valid = false;
            this.errorMessage = "专利摘要不能为空";
        }
    }
}
