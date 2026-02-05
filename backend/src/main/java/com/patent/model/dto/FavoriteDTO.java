package com.patent.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 收藏请求DTO
 */
@Data
@Schema(description = "专利收藏请求")
public class FavoriteDTO {

    @NotNull(message = "专利ID不能为空")
    @Schema(description = "专利ID")
    private Long patentId;

    @Schema(description = "收藏备注")
    private String remark;

    @Schema(description = "收藏夹分组名称")
    private String groupName;
}
