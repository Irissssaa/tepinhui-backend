package com.tepinhui.tepinhui_backend.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "UserProfileVO", description = "用户资料响应")
public class UserProfileVO {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "头像URL")
    private String avatarUrl;

    @Schema(description = "用户角色：CONSUMER/MERCHANT/ADMIN")
    private String role;

    @Schema(description = "用户状态：0禁用，1启用")
    private Integer status;
}
