package com.tepinhui.tepinhui_backend.controller.admin;

import com.tepinhui.tepinhui_backend.common.Result;
import com.tepinhui.tepinhui_backend.dto.admin.AdminCreateUserRequest;
import com.tepinhui.tepinhui_backend.dto.admin.AdminUpdateUserRoleRequest;
import com.tepinhui.tepinhui_backend.service.AdminUserService;
import com.tepinhui.tepinhui_backend.vo.admin.AdminUserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理员用户管理", description = "管理员创建受控账号和调整用户角色")
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @PostMapping
    @Operation(
        summary = "创建受控账号",
        description = "仅管理员可创建 ADMIN/MERCHANT/CONSUMER 账号；普通注册仍固定为 CONSUMER"
    )
    public Result<AdminUserVO> createUser(
        @Parameter(description = "受控账号创建请求", required = true)
        @RequestBody @Valid AdminCreateUserRequest request
    ) {
        return Result.success("账号创建成功", adminUserService.createUser(request));
    }

    @PutMapping("/{id}/role")
    @Operation(
        summary = "调整用户角色",
        description = "仅管理员可调整用户角色；商家入驻审核通过时应优先走商家审核流程授予 MERCHANT"
    )
    public Result<AdminUserVO> updateRole(
        @Parameter(description = "用户ID", required = true)
        @PathVariable Long id,
        @Parameter(description = "角色调整请求", required = true)
        @RequestBody @Valid AdminUpdateUserRoleRequest request
    ) {
        return Result.success("用户角色更新成功", adminUserService.updateUserRole(id, request));
    }
}
