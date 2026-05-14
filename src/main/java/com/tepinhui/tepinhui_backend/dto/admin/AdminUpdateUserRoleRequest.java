package com.tepinhui.tepinhui_backend.dto.admin;

import com.tepinhui.tepinhui_backend.common.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(name = "AdminUpdateUserRoleRequest", description = "管理员调整用户角色请求")
public class AdminUpdateUserRoleRequest {

    @NotNull(message = "角色不能为空")
    @Schema(description = "目标角色", example = "ADMIN")
    private Role role;
}
