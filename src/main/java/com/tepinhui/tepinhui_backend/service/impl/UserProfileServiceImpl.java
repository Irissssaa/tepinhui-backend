package com.tepinhui.tepinhui_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.tepinhui.tepinhui_backend.common.Constants;
import com.tepinhui.tepinhui_backend.dto.user.PasswordChangeCodeRequest;
import com.tepinhui.tepinhui_backend.dto.user.PasswordChangeRequest;
import com.tepinhui.tepinhui_backend.dto.user.UserProfileUpdateRequest;
import com.tepinhui.tepinhui_backend.entity.User;
import com.tepinhui.tepinhui_backend.exception.BusinessException;
import com.tepinhui.tepinhui_backend.mapper.UserMapper;
import com.tepinhui.tepinhui_backend.service.MailService;
import com.tepinhui.tepinhui_backend.service.RegisterVerificationService;
import com.tepinhui.tepinhui_backend.service.UserProfileService;
import com.tepinhui.tepinhui_backend.vo.user.UserProfileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif");
    private static final long MAX_AVATAR_SIZE_BYTES = 2 * 1024 * 1024; // 2MB

    /** Content-Type → 安全扩展名映射（不再从用户文件名提取扩展名，防止路径遍历和扩展名注入） */
    private static final Map<String, String> CONTENT_TYPE_TO_EXTENSION = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/gif", ".gif");

    /** 文件魔数：JPEG (FF D8 FF)、PNG (89 50 4E 47)、GIF (47 49 46 38) */
    private static final Map<String, byte[]> MAGIC_BYTES = Map.of(
            "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
            "image/png",  new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47},
            "image/gif",  new byte[]{0x47, 0x49, 0x46, 0x38});

    private static final DefaultRedisScript<Long> CONSUME_CODE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then " +
                    "redis.call('DEL', KEYS[1]); " +
                    "return 1 " +
                    "end " +
                    "return 0",
            Long.class);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final MailService mailService;

    @Value("${app.auth.register-code.expire-minutes:5}")
    private long codeExpireMinutes;

    @Value("${app.auth.register-code.resend-seconds:60}")
    private long resendSeconds;

    @Value("${app.upload.avatar-dir:./uploads/avatar/}")
    private String avatarDir;

    private final SecureRandom secureRandom = new SecureRandom();

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

    @Override
    public void sendPasswordChangeCode(PasswordChangeCodeRequest request) {
        User user = getCurrentUser();

        String normalizedEmail = RegisterVerificationService.normalizeEmail(request.getEmail());

        // 验证邮箱属于当前登录用户
        if (!normalizedEmail.equals(RegisterVerificationService.normalizeEmail(user.getEmail()))) {
            throw new BusinessException(400, "该邮箱不是当前用户绑定的邮箱");
        }

        // 发送锁：防止频繁发送
        String sendLockKey = Constants.PASSWORD_RESET_CODE_SEND_LOCK_PREFIX + normalizedEmail;
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(sendLockKey, "1", resendSeconds, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            Long ttl = redisTemplate.getExpire(sendLockKey, TimeUnit.SECONDS);
            long remainSeconds = (ttl != null && ttl > 0) ? ttl : resendSeconds;
            throw new BusinessException(400, "验证码发送过于频繁，请" + remainSeconds + "秒后重试");
        }

        // 生成验证码并存入 Redis
        String code = generateVerificationCode();
        String codeKey = Constants.PASSWORD_RESET_CODE_PREFIX + normalizedEmail;
        redisTemplate.opsForValue().set(codeKey, code, codeExpireMinutes, TimeUnit.MINUTES);

        try {
            mailService.sendPasswordResetCode(normalizedEmail, code, codeExpireMinutes);
        } catch (RuntimeException e) {
            // 发送失败清理 Redis
            redisTemplate.delete(codeKey);
            redisTemplate.delete(sendLockKey);
            throw new BusinessException(500, "验证码发送失败，请稍后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(PasswordChangeRequest request) {
        User user = getCurrentUser();

        String normalizedEmail = RegisterVerificationService.normalizeEmail(request.getEmail());

        // 验证邮箱属于当前登录用户
        if (!normalizedEmail.equals(RegisterVerificationService.normalizeEmail(user.getEmail()))) {
            throw new BusinessException(400, "该邮箱不是当前用户绑定的邮箱");
        }

        // 使用 Lua 脚本消费验证码（原子操作）
        String codeKey = Constants.PASSWORD_RESET_CODE_PREFIX + normalizedEmail;
        Long consumed = redisTemplate.execute(
                CONSUME_CODE_SCRIPT,
                List.of(codeKey),
                request.getCode() == null ? null : request.getCode().trim());
        if (!Long.valueOf(1L).equals(consumed)) {
            throw new BusinessException(400, "验证码错误或已过期");
        }

        // 清理发送锁
        redisTemplate.delete(Constants.PASSWORD_RESET_CODE_SEND_LOCK_PREFIX + normalizedEmail);

        // 使用 BCrypt 加密新密码并更新
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, user.getId())
                .set(User::getPassword, encodedPassword));

        log.info("用户 {} 修改密码成功", user.getUsername());
    }

    @Override
    public String uploadAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "上传文件不能为空");
        }

        // 文件大小校验
        if (file.getSize() > MAX_AVATAR_SIZE_BYTES) {
            throw new BusinessException(400, "头像文件大小不能超过2MB");
        }

        // 文件类型校验
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException(400, "仅支持JPEG、PNG、GIF格式的图片");
        }

        // 文件魔数校验：防止 Content-Type 伪造
        validateMagicBytes(file, contentType);

        // 根据 contentType 映射安全扩展名，不从用户文件名提取（防止路径遍历和扩展名注入）
        String extension = getExtensionFromContentType(contentType);
        String filename = UUID.randomUUID().toString() + extension;

        // 确保目录存在
        Path dirPath = Paths.get(avatarDir);
        try {
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }
        } catch (IOException e) {
            log.error("创建头像目录失败: {}", avatarDir, e);
            throw new BusinessException(500, "头像上传失败，请稍后重试");
        }

        // 保存文件
        Path filePath = dirPath.resolve(filename);
        try {
            file.transferTo(filePath.toFile());
        } catch (IOException e) {
            log.error("保存头像文件失败: {}", filePath, e);
            throw new BusinessException(500, "头像上传失败，请稍后重试");
        }

        // 构建可访问的 URL
        String avatarUrl = "/uploads/avatar/" + filename;

        // 更新用户 avatarUrl，删除旧头像文件
        User user = getCurrentUser();
        deleteOldAvatar(user.getAvatarUrl());
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, user.getId())
                .set(User::getAvatarUrl, avatarUrl));

        log.info("用户 {} 头像上传成功: {}", user.getUsername(), avatarUrl);
        return avatarUrl;
    }

    // ==================== 私有方法 ====================

    /**
     * 根据 Content-Type 映射到安全扩展名
     * 不在映射中的类型直接拒绝，防止任意扩展名注入
     */
    private String getExtensionFromContentType(String contentType) {
        String extension = CONTENT_TYPE_TO_EXTENSION.get(contentType);
        if (extension == null) {
            throw new BusinessException(400, "不支持的图片类型: " + contentType);
        }
        return extension;
    }

    /**
     * 校验文件头部魔数，确保文件内容确实是声明的图片格式
     * 防止通过伪造 Content-Type 上传非法文件
     */
    private void validateMagicBytes(MultipartFile file, String contentType) {
        byte[] expected = MAGIC_BYTES.get(contentType);
        if (expected == null) {
            throw new BusinessException(400, "不支持的图片类型: " + contentType);
        }

        byte[] header = new byte[expected.length];
        try (InputStream is = file.getInputStream()) {
            int read = is.read(header);
            if (read < expected.length) {
                log.warn("文件头部读取不足，预期 {} 字节，实际读取 {} 字节", expected.length, read);
                throw new BusinessException(400, "文件内容不合法");
            }
        } catch (IOException e) {
            log.error("读取文件头部魔数失败", e);
            throw new BusinessException(500, "头像上传失败，请稍后重试");
        }

        for (int i = 0; i < expected.length; i++) {
            if (header[i] != expected[i]) {
                log.warn("文件魔数校验失败，contentType={}，文件可能被伪造", contentType);
                throw new BusinessException(400, "文件内容与声明的类型不匹配");
            }
        }
    }

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

    private void deleteOldAvatar(String avatarUrl) {
        if (!StringUtils.hasText(avatarUrl) || !avatarUrl.startsWith("/uploads/avatar/")) {
            return;
        }
        Path oldFile = Paths.get(avatarDir).resolve(Paths.get(avatarUrl).getFileName().toString());
        try {
            if (Files.exists(oldFile)) {
                Files.delete(oldFile);
                log.info("已删除旧头像文件: {}", oldFile);
            }
        } catch (IOException e) {
            log.warn("删除旧头像文件失败: {}", oldFile, e);
        }
    }

    private String generateVerificationCode() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }
}
