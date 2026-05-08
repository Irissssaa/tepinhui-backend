package com.tepinhui.tepinhui_backend.service;

import com.tepinhui.tepinhui_backend.common.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Service
public class RegisterVerificationService {

    private static final DefaultRedisScript<Long> CONSUME_CODE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then " +
                    "redis.call('DEL', KEYS[1]); " +
                    "return 1 " +
                    "end " +
                    "return 0",
            Long.class);

    private final StringRedisTemplate redisTemplate;
    private final MailService mailService;
    private final long codeExpireMinutes;
    private final long resendSeconds;
    private final SecureRandom secureRandom = new SecureRandom();

    public RegisterVerificationService(StringRedisTemplate redisTemplate,
                                       MailService mailService,
                                       @Value("${app.auth.register-code.expire-minutes:5}") long codeExpireMinutes,
                                       @Value("${app.auth.register-code.resend-seconds:60}") long resendSeconds) {
        this.redisTemplate = redisTemplate;
        this.mailService = mailService;
        this.codeExpireMinutes = codeExpireMinutes;
        this.resendSeconds = resendSeconds;
    }

    public void sendRegistrationCode(String email) {
        String normalizedEmail = normalizeEmail(email);
        String sendLockKey = Constants.REGISTER_CODE_SEND_LOCK_PREFIX + normalizedEmail;
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(sendLockKey, "1", resendSeconds, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            Long ttl = redisTemplate.getExpire(sendLockKey, TimeUnit.SECONDS);
            long remainSeconds = (ttl != null && ttl > 0) ? ttl : resendSeconds;
            throw new IllegalArgumentException("验证码发送过于频繁，请" + remainSeconds + "秒后重试");
        }

        String code = generateVerificationCode();
        String codeKey = Constants.REGISTER_CODE_PREFIX + normalizedEmail;

        redisTemplate.opsForValue().set(codeKey, code, codeExpireMinutes, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(sendLockKey, "1", resendSeconds, TimeUnit.SECONDS);

        try {
            mailService.sendRegistrationCode(normalizedEmail, code, codeExpireMinutes);
        } catch (RuntimeException e) {
            redisTemplate.delete(codeKey);
            redisTemplate.delete(sendLockKey);
            throw e;
        }
    }

    public boolean verifyRegistrationCode(String email, String code) {
        String normalizedEmail = normalizeEmail(email);
        String codeKey = Constants.REGISTER_CODE_PREFIX + normalizedEmail;
        Long consumed = redisTemplate.execute(
                CONSUME_CODE_SCRIPT,
                List.of(codeKey),
                code == null ? null : code.trim());
        if (!Long.valueOf(1L).equals(consumed)) {
            return false;
        }

        redisTemplate.delete(Constants.REGISTER_CODE_SEND_LOCK_PREFIX + normalizedEmail);
        return true;
    }

    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String generateVerificationCode() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }
}
