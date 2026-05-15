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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

        // 审计日志：记录创建受控账号操作
        String currentAdmin = getCurrentAdminUsernameOrSystem();
        log.info("管理员 {} 创建了受控账号 username={}, role={}", currentAdmin, user.getUsername(), user.getRole());

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

        // 2. 取当前登录管理员
        User currentAdmin = getCurrentAdminUserOrNull();

        // 3. 自我降级保护：当前管理员不能调整自己的角色为非 ADMIN
        if (currentAdmin != null
                && currentAdmin.getId().equals(id)
                && request.getRole() != Role.ADMIN) {
            throw new BusinessException(403, "不允许调整自己的角色");
        }

        // 4. 唯一管理员保护：当目标用户当前为 ADMIN 且变更为非 ADMIN 时，系统至少需要保留一个 ADMIN
        if (user.getRole() == Role.ADMIN && request.getRole() != Role.ADMIN) {
            long adminCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getRole, Role.ADMIN)
            );
            if (adminCount <= 1) {
                throw new BusinessException(409, "系统至少需要保留一个管理员");
            }
        }

        // 5. 审计日志（变更前）
        String adminUsername = currentAdmin != null ? currentAdmin.getUsername() : "system";
        log.info("管理员 {} 准备将用户 {} 的角色从 {} 调整为 {}",
                adminUsername, id, user.getRole(), request.getRole());

        // 6. 更新角色
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, id)
                     .set(User::getRole, request.getRole());
        userMapper.update(null, updateWrapper);

        // 7. 审计日志（变更后）
        log.info("管理员 {} 已将用户 {} 的角色从 {} 调整为 {}",
                adminUsername, id, user.getRole(), request.getRole());

        // 8. 重新查询返回最新数据
        User updated = userMapper.selectById(id);
        if (updated == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return buildAdminUserVO(updated);
    }

    // ==================== 私有方法 ====================

    /**
     * 取当前登录管理员实体；若 SecurityContext 不可用或无法定位用户则返回 null。
     */
    private User getCurrentAdminUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String username = authentication.getName();
        if (username == null || username.isEmpty()) {
            return null;
        }
        return userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
    }

    /**
     * 取当前登录管理员 username，取不到则返回 "system" 兜底（仅用于审计日志）。
     */
    private String getCurrentAdminUsernameOrSystem() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "system";
        }
        String username = authentication.getName();
        if (username == null || username.isEmpty()) {
            return "system";
        }
        return username;
    }

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
