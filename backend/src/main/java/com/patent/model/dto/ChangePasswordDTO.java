package com.patent.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码DTO（普通用户：需提供旧密码）
 */
@Data
public class ChangePasswordDTO {

    /**
     * 旧密码
     */
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    /**
     * 新密码
     */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "新密码长度在6到20个字符")
    private String newPassword;

    /**
     * 确认新密码
     */
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}
