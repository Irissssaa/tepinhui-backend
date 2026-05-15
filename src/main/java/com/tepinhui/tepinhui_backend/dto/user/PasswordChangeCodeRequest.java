package com.tepinhui.tepinhui_backend.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(name = "PasswordChangeCodeRequest", description = "发送修改密码验证码请求")
public class PasswordChangeCodeRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "当前用户绑定的邮箱", example = "demo@tepinhui.com")
    private String email;

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim();
    }
}
