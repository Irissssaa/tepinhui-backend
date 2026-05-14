package com.tepinhui.tepinhui_backend.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "UserProfileUpdateRequest", description = "用户资料更新请求")
public class UserProfileUpdateRequest {

    @Size(max = 50, message = "昵称不能超过50个字符")
    @Schema(description = "昵称", example = "西湖龙井爱好者")
    private String nickname;

    @Size(max = 255, message = "头像URL不能超过255个字符")
    @Schema(description = "头像URL", example = "https://oss.example.com/avatar/user-1.png")
    private String avatarUrl;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱不能超过100个字符")
    @Schema(description = "邮箱", example = "demo@tepinhui.com")
    private String email;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号", example = "13800138000")
    private String phone;
}
