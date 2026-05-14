package com.tepinhui.tepinhui_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(name = "LoginRequest", description = "用户登录请求")
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    @Schema(description = "用户名", example = "demo_user")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "登录密码", example = "Tph@123456")
    private String password;
}
