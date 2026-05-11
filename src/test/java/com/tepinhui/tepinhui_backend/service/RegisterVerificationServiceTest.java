package com.tepinhui.tepinhui_backend.service;

import com.tepinhui.tepinhui_backend.common.Constants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterVerificationServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private MailService mailService;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    void sendRegistrationCodeShouldUseAtomicResendLock() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(
                Constants.REGISTER_CODE_SEND_LOCK_PREFIX + "user@example.com",
                "1", 60L, TimeUnit.SECONDS)).thenReturn(Boolean.TRUE);

        RegisterVerificationService service = new RegisterVerificationService(redisTemplate, mailService, 5, 60);

        service.sendRegistrationCode("user@example.com");

        verify(valueOperations).setIfAbsent(
                Constants.REGISTER_CODE_SEND_LOCK_PREFIX + "user@example.com",
                "1", 60L, TimeUnit.SECONDS);
        verify(valueOperations).set(
                anyString(),
                anyString(),
                org.mockito.ArgumentMatchers.eq(5L),
                org.mockito.ArgumentMatchers.eq(TimeUnit.MINUTES));
        verify(mailService).sendRegistrationCode(anyString(), anyString(), org.mockito.ArgumentMatchers.eq(5L));
    }

    @Test
    void sendRegistrationCodeShouldRejectConcurrentResend() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(Boolean.FALSE);
        when(redisTemplate.getExpire(anyString(), any())).thenReturn(42L);

        RegisterVerificationService service = new RegisterVerificationService(redisTemplate, mailService, 5, 60);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.sendRegistrationCode("user@example.com"));

        assertTrue(ex.getMessage().contains("42"));
        verifyNoInteractions(mailService);
    }

    @Test
    void sendRegistrationCodeShouldNormalizeEmailForRedisAndMail() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(
                Constants.REGISTER_CODE_SEND_LOCK_PREFIX + "user@example.com",
                "1", 60L, TimeUnit.SECONDS)).thenReturn(Boolean.TRUE);

        RegisterVerificationService service = new RegisterVerificationService(redisTemplate, mailService, 5, 60);

        service.sendRegistrationCode(" User@Example.com ");

        verify(valueOperations).setIfAbsent(
                Constants.REGISTER_CODE_SEND_LOCK_PREFIX + "user@example.com",
                "1", 60L, TimeUnit.SECONDS);
        verify(valueOperations).set(
                eq(Constants.REGISTER_CODE_PREFIX + "user@example.com"),
                anyString(),
                eq(5L),
                eq(TimeUnit.MINUTES));
        verify(mailService).sendRegistrationCode(eq("user@example.com"), anyString(), eq(5L));
    }

    @Test
    void sendRegistrationCodeShouldDeleteKeysAndRethrowMailFailure() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(
                Constants.REGISTER_CODE_SEND_LOCK_PREFIX + "user@example.com",
                "1", 60L, TimeUnit.SECONDS)).thenReturn(Boolean.TRUE);
        doThrow(new IllegalStateException("smtp unavailable"))
                .when(mailService)
                .sendRegistrationCode(eq("user@example.com"), anyString(), eq(5L));

        RegisterVerificationService service = new RegisterVerificationService(redisTemplate, mailService, 5, 60);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.sendRegistrationCode(" User@Example.com "));

        assertEquals("smtp unavailable", ex.getMessage());
        verify(redisTemplate).delete(Constants.REGISTER_CODE_PREFIX + "user@example.com");
        verify(redisTemplate).delete(Constants.REGISTER_CODE_SEND_LOCK_PREFIX + "user@example.com");
    }

    @Test
    void verifyRegistrationCodeShouldConsumeAtomically() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any())).thenReturn(1L);

        RegisterVerificationService service = new RegisterVerificationService(redisTemplate, mailService, 5, 60);
        boolean result = service.verifyRegistrationCode("user@example.com", "123456");

        assertTrue(result);
        verify(redisTemplate).delete(Constants.REGISTER_CODE_SEND_LOCK_PREFIX + "user@example.com");
    }

    @Test
    void verifyRegistrationCodeShouldFailWhenAlreadyConsumed() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any())).thenReturn(0L);

        RegisterVerificationService service = new RegisterVerificationService(redisTemplate, mailService, 5, 60);
        boolean result = service.verifyRegistrationCode("user@example.com", "123456");

        assertFalse(result);
        verify(redisTemplate).execute(any(RedisScript.class), anyList(), any());
    }
}
