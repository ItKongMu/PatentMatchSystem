package com.patent.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 更新用户角色DTO
 */
@Data
@Schema(description = "更新用户角色")
public class UpdateUserRoleDTO {

    @NotNull(message = "用户ID不能为空")
    @Schema(description = "目标用户ID")
    private Long userId;

    @NotNull(message = "角色不能为空")
    @Pattern(regexp = "^(admin|user)$", message = "角色只能是 admin 或 user")
    @Schema(description = "新角色：admin/user")
    private String role;
}
