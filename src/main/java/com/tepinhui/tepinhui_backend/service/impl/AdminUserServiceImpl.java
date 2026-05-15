package com.tepinhui.tepinhui_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.tepinhui.tepinhui_backend.common.Role;
import com.tepinhui.tepinhui_backend.common.UserStatus;
import com.tepinhui.tepinhui_backend.dto.admin.AdminCreateUserRequest;
import com.tepinhui.tepinhui_backend.dto.admin.AdminUpdateUserRoleRequest;
import com.tepinhui.tepinhui_backend.entity.User;
import com.tepinhui.tepinhui_backend.exception.BusinessException;
import com.tepinhui.tepinhui_backend.mapper.UserMapper;
import com.tepinhui.tepinhui_backend.service.AdminUserService;
import com.tepinhui.tepinhui_backend.vo.admin.AdminUserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserVO createUser(AdminCreateUserRequest request) {
        // 1. 用户名唯一性校验
        long usernameCount = userMapper.selectCount(
            new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername())
        );
        if (usernameCount > 0) {
            throw new BusinessException(409, "用户名已存在");
        }

        // 2. 手机号唯一性校验
        long phoneCount = userMapper.selectCount(
            new LambdaQueryWrapper<User>().eq(User::getPhone, request.getPhone())
        );
        if (phoneCount > 0) {
            throw new BusinessException(409, "手机号已被使用");
        }

        // 3. 构造用户实体
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setNickname(request.getNickname());
        user.setRole(request.getRole());
        user.setStatus(UserStatus.ENABLED);

        userMapper.insert(user);

        return buildAdminUserVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserVO updateUserRole(Long id, AdminUpdateUserRoleRequest request) {
        // 1. 查用户存在
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        // 2. 更新角色
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, id)
                     .set(User::getRole, request.getRole());
        userMapper.update(null, updateWrapper);

        // 3. 重新查询返回最新数据
        User updated = userMapper.selectById(id);
        return buildAdminUserVO(updated);
    }

    // ==================== 私有方法 ====================

    private AdminUserVO buildAdminUserVO(User user) {
        AdminUserVO vo = new AdminUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setNickname(user.getNickname());
        vo.setRole(user.getRole() != null ? user.getRole().name() : null);
        vo.setStatus(user.getStatus() != null ? user.getStatus().getValue() : null);
        return vo;
    }
}
