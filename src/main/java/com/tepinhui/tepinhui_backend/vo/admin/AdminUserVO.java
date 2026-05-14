package com.tepinhui.tepinhui_backend.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "AdminUserVO", description = "管理员侧用户响应")
public class AdminUserVO {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "角色：CONSUMER/MERCHANT/ADMIN")
    private String role;

    @Schema(description = "状态：0=禁用，1=启用")
    private Integer status;
}
