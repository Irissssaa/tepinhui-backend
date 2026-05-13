package com.tepinhui.tepinhui_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(name = "RegisterCodeRequest", description = "发送注册邮箱验证码请求")
public class RegisterCodeRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "注册邮箱", example = "demo@tepinhui.com")
    private String email;

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim();
    }
}
