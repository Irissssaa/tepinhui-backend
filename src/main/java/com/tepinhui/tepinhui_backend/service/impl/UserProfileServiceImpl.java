package com.tepinhui.tepinhui_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.tepinhui.tepinhui_backend.dto.user.UserProfileUpdateRequest;
import com.tepinhui.tepinhui_backend.entity.User;
import com.tepinhui.tepinhui_backend.exception.BusinessException;
import com.tepinhui.tepinhui_backend.mapper.UserMapper;
import com.tepinhui.tepinhui_backend.service.UserProfileService;
import com.tepinhui.tepinhui_backend.vo.user.UserProfileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserMapper userMapper;

    @Override
    public UserProfileVO getCurrentUserProfile() {
        User user = getCurrentUser();
        return buildUserProfileVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfileVO updateCurrentUserProfile(UserProfileUpdateRequest request) {
        User user = getCurrentUser();

        // 逐字段非null覆盖
        if (StringUtils.hasText(request.getNickname())) {
            user.setNickname(request.getNickname());
        }
        if (StringUtils.hasText(request.getAvatarUrl())) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (StringUtils.hasText(request.getEmail())) {
            user.setEmail(request.getEmail());
        }
        if (StringUtils.hasText(request.getPhone())) {
            user.setPhone(request.getPhone());
        }

        // 用 LambdaUpdateWrapper 只更新非空字段，避免将 password 等字段传回数据库
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        boolean hasUpdate = false;
        if (StringUtils.hasText(request.getNickname())) {
            updateWrapper.set(User::getNickname, request.getNickname());
            hasUpdate = true;
        }
        if (StringUtils.hasText(request.getAvatarUrl())) {
            updateWrapper.set(User::getAvatarUrl, request.getAvatarUrl());
            hasUpdate = true;
        }
        if (StringUtils.hasText(request.getEmail())) {
            updateWrapper.set(User::getEmail, request.getEmail());
            hasUpdate = true;
        }
        if (StringUtils.hasText(request.getPhone())) {
            updateWrapper.set(User::getPhone, request.getPhone());
            hasUpdate = true;
        }
        if (hasUpdate) {
            updateWrapper.eq(User::getId, user.getId());
            userMapper.update(null, updateWrapper);
        }

        // 重新查询返回最新数据
        User updated = userMapper.selectById(user.getId());
        return buildUserProfileVO(updated);
    }

    // ==================== 私有方法 ====================

    /**
     * 从 SecurityContext 获取当前登录用户
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(401, "未登录");
        }
        String username;
        Object principal = authentication.getPrincipal();
        if (principal instanceof String) {
            username = (String) principal;
        } else if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            throw new BusinessException(401, "登录状态无效");
        }
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(401, "登录状态无效");
        }
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .last("LIMIT 1")
        );
        if (user == null) {
            throw new BusinessException(404, "当前用户不存在");
        }
        return user;
    }

    /**
     * 将 User 实体转换为 UserProfileVO
     */
    private UserProfileVO buildUserProfileVO(User user) {
        UserProfileVO vo = new UserProfileVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatarUrl(user.getAvatarUrl());
        // 角色：Role enum → 字符串
        vo.setRole(user.getRole() != null ? user.getRole().name() : null);
        // 状态：UserStatus enum → Integer
        vo.setStatus(user.getStatus() != null ? user.getStatus().getValue() : null);
        return vo;
    }
}
