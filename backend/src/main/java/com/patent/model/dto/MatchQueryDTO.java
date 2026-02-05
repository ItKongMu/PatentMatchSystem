package com.patent.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 技术匹配查询DTO
 */
@Data
@Schema(description = "技术匹配查询请求")
public class MatchQueryDTO {

    @NotBlank(message = "查询文本不能为空")
    @Schema(description = "查询文本", required = true)
    private String query;

    @Schema(description = "领域过滤（IPC部，如G/H/A）")
    private String domainFilter;

    @Schema(description = "返回结果数量", defaultValue = "10")
    private Integer topK = 10;
}
