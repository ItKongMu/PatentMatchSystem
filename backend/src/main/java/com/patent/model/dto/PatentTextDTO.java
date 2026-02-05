package com.patent.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 专利文本录入DTO
 */
@Data
@Schema(description = "专利文本录入请求")
public class PatentTextDTO {

    @Size(max = 50, message = "公开号长度不能超过50个字符")
    @Pattern(regexp = "^$|^[A-Z]{2}\\d+[A-Z]?\\d*$", message = "公开号格式不正确，示例：CN123456789A")
    @Schema(description = "公开号/专利号", example = "CN123456789A")
    private String publicationNo;

    @NotBlank(message = "专利名称不能为空")
    @Size(min = 5, max = 200, message = "专利名称长度应在5-200个字符之间")
    @Schema(description = "专利名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "一种智能检测方法")
    private String title;

    @Size(max = 200, message = "申请人长度不能超过200个字符")
    @Schema(description = "申请人", example = "华为技术有限公司")
    private String applicant;

    @Schema(description = "公开日期", example = "2024-01-15")
    private LocalDate publicationDate;

    @NotBlank(message = "专利摘要不能为空")
    @Size(min = 50, max = 5000, message = "专利摘要长度应在50-5000个字符之间")
    @Schema(description = "专利摘要", requiredMode = Schema.RequiredMode.REQUIRED)
    private String patentAbstract;

    @Size(max = 500, message = "IPC分类号长度不能超过500个字符")
    @Schema(description = "IPC分类号（可选，多个用逗号分隔）", example = "G06F 16/30, G06N 3/08")
    private String ipcClassification;

    @Size(max = 100000, message = "专利正文长度不能超过10万个字符")
    @Schema(description = "专利正文（可选，如有则存入系统用于深度分析）")
    private String fullText;
}
