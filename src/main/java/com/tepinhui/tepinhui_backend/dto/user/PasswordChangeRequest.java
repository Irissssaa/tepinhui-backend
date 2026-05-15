package com.tepinhui.tepinhui_backend.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "PasswordChangeRequest", description = "修改密码请求")
public class PasswordChangeRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "当前用户绑定的邮箱", example = "demo@tepinhui.com")
    private String email;

    @NotBlank(message = "验证码不能为空")
    @Schema(description = "邮箱验证码", example = "123456")
    private String code;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "新密码长度需在6-20位之间")
    @Schema(description = "新密码", example = "newPassword123")
    private String newPassword;

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim();
    }
}
