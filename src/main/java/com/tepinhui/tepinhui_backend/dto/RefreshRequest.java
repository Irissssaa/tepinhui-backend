package com.tepinhui.tepinhui_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshRequest {

    @NotBlank(message = "Refresh Token不能为空")
    private String refreshToken;
}
