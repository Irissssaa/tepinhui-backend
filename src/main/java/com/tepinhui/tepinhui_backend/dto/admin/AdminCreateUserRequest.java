package com.tepinhui.tepinhui_backend.dto.admin;

import com.tepinhui.tepinhui_backend.common.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(name = "AdminCreateUserRequest", description = "管理员创建受控账号请求")
public class AdminCreateUserRequest {

    @NotBlank(message = "用户名不能为空")
    @Schema(description = "用户名", example = "admin2")
    private String username;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号", example = "13900139009")
    private String phone;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "初始密码", example = "Tph@123456")
    private String password;

    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱", example = "admin2@example.com")
    private String email;

    @Schema(description = "昵称", example = "平台运营管理员")
    private String nickname;

    @NotNull(message = "角色不能为空")
    @Schema(description = "账号角色。普通开放注册不得使用此字段，只有管理员后台可用", example = "ADMIN")
    private Role role;
}
