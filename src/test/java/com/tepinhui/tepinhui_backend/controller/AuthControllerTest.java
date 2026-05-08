package com.tepinhui.tepinhui_backend.controller;

import com.tepinhui.tepinhui_backend.common.Role;
import com.tepinhui.tepinhui_backend.common.UserStatus;
import com.tepinhui.tepinhui_backend.entity.User;
import com.tepinhui.tepinhui_backend.mapper.UserMapper;
import com.tepinhui.tepinhui_backend.security.JwtUtil;
import com.tepinhui.tepinhui_backend.service.RegisterVerificationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @MockBean
    private RegisterVerificationService registerVerificationService;

    @Test
    void sendRegisterCodeShouldReturnSuccess() throws Exception {
        doReturn(0L).when(userMapper).selectCount(any());
        doNothing().when(registerVerificationService).sendRegistrationCode("user@example.com");

        mockMvc.perform(post("/auth/register/code")
                        .with(csrf())
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
                        .with(csrf())
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
    }

    @Test
    void registerShouldRejectInvalidCode() throws Exception {
        doReturn(0L, 0L, 0L).when(userMapper).selectCount(any());
        doReturn(false).when(registerVerificationService).verifyRegistrationCode("user@example.com", "123456");

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
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
    void sendRegisterCodeShouldRejectInvalidEmail() throws Exception {
        mockMvc.perform(post("/auth/register/code")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-an-email"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}
