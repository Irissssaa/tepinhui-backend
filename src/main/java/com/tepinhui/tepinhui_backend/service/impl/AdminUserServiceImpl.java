package com.tepinhui.tepinhui_backend.service.impl;

import com.tepinhui.tepinhui_backend.dto.admin.AdminCreateUserRequest;
import com.tepinhui.tepinhui_backend.dto.admin.AdminUpdateUserRoleRequest;
import com.tepinhui.tepinhui_backend.exception.BusinessException;
import com.tepinhui.tepinhui_backend.service.AdminUserService;
import com.tepinhui.tepinhui_backend.vo.admin.AdminUserVO;
import org.springframework.stereotype.Service;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    @Override
    public AdminUserVO createUser(AdminCreateUserRequest request) {
        throw new BusinessException(501, "管理员创建受控账号业务逻辑待实现");
    }

    @Override
    public AdminUserVO updateUserRole(Long id, AdminUpdateUserRoleRequest request) {
        throw new BusinessException(501, "管理员调整用户角色业务逻辑待实现");
    }
}
