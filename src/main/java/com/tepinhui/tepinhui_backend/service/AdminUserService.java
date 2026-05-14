package com.tepinhui.tepinhui_backend.service;

import com.tepinhui.tepinhui_backend.dto.admin.AdminCreateUserRequest;
import com.tepinhui.tepinhui_backend.dto.admin.AdminUpdateUserRoleRequest;
import com.tepinhui.tepinhui_backend.vo.admin.AdminUserVO;

public interface AdminUserService {

    /**
     * 管理员创建受控账号，可用于初始化之后新增管理员或运营账号。
     */
    AdminUserVO createUser(AdminCreateUserRequest request);

    /**
     * 管理员调整用户角色；普通注册和登录流程不得直接调用角色选择。
     */
    AdminUserVO updateUserRole(Long id, AdminUpdateUserRoleRequest request);
}
