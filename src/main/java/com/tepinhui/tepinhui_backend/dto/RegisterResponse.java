package com.tepinhui.tepinhui_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(name = "RegisterResponse", description = "注册成功后的用户信息响应")
public class RegisterResponse {

    @Schema(description = "用户ID", example = "1")
    private Long id;

    @Schema(description = "用户名", example = "demo_user")
    private String username;

    @Schema(description = "昵称", example = "西湖龙井爱好者")
    private String nickname;

    @Schema(description = "邮箱", example = "demo@tepinhui.com")
    private String email;

    @Schema(description = "用户角色：CONSUMER/MERCHANT/ADMIN", example = "CONSUMER")
    private String role;
}
