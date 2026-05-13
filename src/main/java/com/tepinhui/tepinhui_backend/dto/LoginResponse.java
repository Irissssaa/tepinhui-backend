package com.tepinhui.tepinhui_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(name = "LoginResponse", description = "登录与刷新 Token 响应")
public class LoginResponse {

    @Schema(description = "访问令牌", example = "eyJhbGciOiJIUzI1NiJ9.access")
    private String accessToken;

    @Schema(description = "刷新令牌", example = "eyJhbGciOiJIUzI1NiJ9.refresh")
    private String refreshToken;

    @Schema(description = "令牌类型", example = "Bearer")
    private String tokenType;

    @Schema(description = "access token 有效期，单位秒", example = "7200")
    private Long expiresIn;

    @Schema(description = "当前登录用户信息")
    private UserInfo userInfo;

    @Data
    @AllArgsConstructor
    @Schema(name = "LoginUserInfo", description = "登录态中的用户基础信息")
    public static class UserInfo {
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
}
