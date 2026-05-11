package com.tepinhui.tepinhui_backend.dto;

import com.tepinhui.tepinhui_backend.service.RegisterVerificationService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    private String nickname;

    @NotBlank(message = "验证码不能为空")
    private String code;

    public void setEmail(String email) {
        this.email = RegisterVerificationService.normalizeEmail(email);
    }

    public void setCode(String code) {
        this.code = code == null ? null : code.trim();
    }
}
