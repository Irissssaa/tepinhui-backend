package com.tepinhui.tepinhui_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(name = "RefreshRequest", description = "刷新 Token 请求")
public class RefreshRequest {

    @NotBlank(message = "Refresh Token不能为空")
    @Schema(description = "refresh token", example = "eyJhbGciOiJIUzI1NiJ9.refresh")
    private String refreshToken;
}
