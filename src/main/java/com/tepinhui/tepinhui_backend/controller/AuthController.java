package com.tepinhui.tepinhui_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tepinhui.tepinhui_backend.common.Constants;
import com.tepinhui.tepinhui_backend.common.Result;
import io.jsonwebtoken.Claims;
import com.tepinhui.tepinhui_backend.common.Role;
import com.tepinhui.tepinhui_backend.common.UserStatus;
import com.tepinhui.tepinhui_backend.dto.LoginRequest;
import com.tepinhui.tepinhui_backend.dto.LoginResponse;
import com.tepinhui.tepinhui_backend.dto.RefreshRequest;
import com.tepinhui.tepinhui_backend.dto.RegisterRequest;
import com.tepinhui.tepinhui_backend.entity.User;
import com.tepinhui.tepinhui_backend.mapper.UserMapper;
import com.tepinhui.tepinhui_backend.security.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@Tag(name = "认证管理", description = "登录、注册、刷新Token、登出")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String username = request.getUsername();
        String failKey = Constants.LOGIN_FAIL_PREFIX + username;

        // 检查是否被锁定
        String failCountStr = redisTemplate.opsForValue().get(failKey);
        if (failCountStr != null && Long.parseLong(failCountStr) >= Constants.LOGIN_MAX_ATTEMPTS) {
            Long ttl = redisTemplate.getExpire(failKey, TimeUnit.SECONDS);
            long remainMinutes = (ttl != null && ttl > 0) ? ttl / 60 + 1 : Constants.LOGIN_LOCK_DURATION_MINUTES;
            return Result.error(429, "登录失败次数过多，请" + remainMinutes + "分钟后重试");
        }

        // 先查用户是否存在
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));

        if (user == null) {
            redisTemplate.opsForValue().increment(failKey);
            redisTemplate.expire(failKey, Constants.LOGIN_LOCK_DURATION_MINUTES, TimeUnit.MINUTES);
            return Result.error(401, "用户名或密码错误");
        }

        // 检查账号状态
        if (user.getStatus() == UserStatus.DISABLED) {
            return Result.error(403, "用户已被禁用");
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            Long count = redisTemplate.opsForValue().increment(failKey);
            if (count != null && count == 1) {
                redisTemplate.expire(failKey, Constants.LOGIN_LOCK_DURATION_MINUTES, TimeUnit.MINUTES);
            }
            return Result.error(401, "用户名或密码错误");
        }

        // 登录成功，清除失败计数
        redisTemplate.delete(failKey);

        String roleName = user.getRole().name();
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), roleName);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername(), roleName);

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                user.getId(), user.getUsername(), user.getNickname(), roleName);

        LoginResponse response = new LoginResponse(
                accessToken, refreshToken, "Bearer",
                jwtUtil.getAccessExpiration(), userInfo);

        return Result.success(response);
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        // 检查用户名是否已存在
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (count > 0) {
            return Result.error(400, "用户名已存在");
        }

        // 检查手机号是否已存在
        count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getPhone, request.getPhone()));
        if (count > 0) {
            return Result.error(400, "手机号已注册");
        }

        // 检查邮箱是否已存在
        if (request.getEmail() != null) {
            count = userMapper.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail()));
            if (count > 0) {
                return Result.error(400, "邮箱已注册");
            }
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setNickname(request.getNickname());
        user.setRole(Role.CONSUMER);
        user.setStatus(UserStatus.ENABLED);

        userMapper.insert(user);

        return Result.success("注册成功", null);
    }

    @Operation(summary = "刷新Token")
    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtUtil.validateToken(refreshToken)) {
            return Result.error(401, "Refresh Token无效或已过期");
        }

        Claims claims = jwtUtil.parseToken(refreshToken);
        String type = claims.get("type", String.class);
        if (!"refresh".equals(type)) {
            return Result.error(401, "Token类型错误");
        }

        String username = claims.getSubject();
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));

        if (user == null || user.getStatus() == UserStatus.DISABLED) {
            return Result.error(401, "用户不存在或已被禁用");
        }

        // 使用数据库中的最新角色，而非 token 中的旧角色
        String roleName = user.getRole().name();
        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), roleName);
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername(), roleName);

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                user.getId(), user.getUsername(), user.getNickname(), roleName);

        LoginResponse response = new LoginResponse(
                newAccessToken, newRefreshToken, "Bearer",
                jwtUtil.getAccessExpiration(), userInfo);

        return Result.success(response);
    }

    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            if (jwtUtil.validateToken(token)) {
                long remainingTime = jwtUtil.getRemainingExpiration(token);
                if (remainingTime > 0) {
                    // 将 token 加入黑名单，TTL 设为 token 剩余有效期
                    redisTemplate.opsForValue().set(
                            Constants.TOKEN_BLACKLIST_PREFIX + token, "1",
                            remainingTime, TimeUnit.MILLISECONDS);
                }
            }
        }
        return Result.success("登出成功", null);
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public Result<LoginResponse.UserInfo> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Result.error(401, "未登录");
        }

        String username = (String) authentication.getPrincipal();
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));

        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                user.getId(), user.getUsername(), user.getNickname(), user.getRole().name());

        return Result.success(userInfo);
    }
}
