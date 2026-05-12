package com.tepinhui.tepinhui_backend.controller;

import com.tepinhui.tepinhui_backend.config.SecurityConfig;
import com.tepinhui.tepinhui_backend.common.Role;
import com.tepinhui.tepinhui_backend.common.UserStatus;
import com.tepinhui.tepinhui_backend.entity.User;
import com.tepinhui.tepinhui_backend.exception.GlobalExceptionHandler;
import com.tepinhui.tepinhui_backend.mapper.UserMapper;
import com.tepinhui.tepinhui_backend.security.JwtAuthenticationFilter;
import com.tepinhui.tepinhui_backend.security.JwtUtil;
import com.tepinhui.tepinhui_backend.service.RegisterVerificationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@ContextConfiguration(classes = {
        AuthController.class,
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        AuthControllerTest.NoOpJwtFilterConfig.class
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @MockitoBean
    private RegisterVerificationService registerVerificationService;

    @Test
    void sendRegisterCodeShouldReturnSuccess() throws Exception {
        doReturn(0L).when(userMapper).selectCount(any());
        doNothing().when(registerVerificationService).sendRegistrationCode("user@example.com");

        mockMvc.perform(post("/auth/register/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "User@Example.com "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("验证码发送成功"));

        verify(registerVerificationService).sendRegistrationCode("user@example.com");
    }

    @Test
    void sendRegisterCodeShouldRejectFrequentRequest() throws Exception {
        doReturn(0L).when(userMapper).selectCount(any());
        doThrow(new IllegalArgumentException("验证码发送过于频繁，请42秒后重试"))
                .when(registerVerificationService).sendRegistrationCode("user@example.com");

        mockMvc.perform(post("/auth/register/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": " user@example.com "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("验证码发送过于频繁，请42秒后重试"));

        verify(registerVerificationService).sendRegistrationCode("user@example.com");
    }

    @Test
    void sendRegisterCodeShouldRejectRegisteredEmail() throws Exception {
        doReturn(1L).when(userMapper).selectCount(any());

        mockMvc.perform(post("/auth/register/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("邮箱已注册"));

        verifyNoInteractions(registerVerificationService);
    }

    @Test
    void sendRegisterCodeShouldRejectInvalidEmail() throws Exception {
        mockMvc.perform(post("/auth/register/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-an-email"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        verifyNoInteractions(registerVerificationService);
    }

    @Test
    void registerShouldReturnCreatedUserInfo() throws Exception {
        doReturn(0L, 0L, 0L).when(userMapper).selectCount(any());
        doReturn("encoded-password").when(passwordEncoder).encode("123456");
        doReturn(true).when(registerVerificationService).verifyRegistrationCode("user@example.com", "123456");
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(101L);
            user.setRole(Role.CONSUMER);
            user.setStatus(UserStatus.ENABLED);
            return 1;
        }).when(userMapper).insert(any(User.class));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "alice",
                                  "phone": "13800138000",
                                  "password": "123456",
                                  "email": "User@Example.com ",
                                  "nickname": "Alice",
                                  "code": "123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("注册成功"))
                .andExpect(jsonPath("$.data.id").value(101))
                .andExpect(jsonPath("$.data.username").value("alice"))
                .andExpect(jsonPath("$.data.nickname").value("Alice"))
                .andExpect(jsonPath("$.data.email").value("user@example.com"))
                .andExpect(jsonPath("$.data.role").value("CONSUMER"));

        verify(registerVerificationService).verifyRegistrationCode("user@example.com", "123456");
    }

    @Test
    void registerShouldRejectInvalidCode() throws Exception {
        doReturn(0L, 0L, 0L).when(userMapper).selectCount(any());
        doReturn(false).when(registerVerificationService).verifyRegistrationCode("user@example.com", "123456");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "alice",
                                  "phone": "13800138000",
                                  "password": "123456",
                                  "email": "user@example.com",
                                  "nickname": "Alice",
                                  "code": "123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("验证码错误或已过期"));
    }

    @Test
    void registerShouldRejectInvalidEmail() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "alice",
                                  "phone": "13800138000",
                                  "password": "123456",
                                  "email": "not-an-email",
                                  "nickname": "Alice",
                                  "code": "123456"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("邮箱格式不正确"));

        verifyNoInteractions(registerVerificationService);
    }

    @TestConfiguration
    static class NoOpJwtFilterConfig {

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(JwtUtil jwtUtil, StringRedisTemplate redisTemplate) {
            return new JwtAuthenticationFilter(jwtUtil, redisTemplate) {
                @Override
                protected void doFilterInternal(HttpServletRequest request,
                                                HttpServletResponse response,
                                                FilterChain filterChain) throws ServletException, IOException {
                    filterChain.doFilter(request, response);
                }
            };
        }
    }
}
