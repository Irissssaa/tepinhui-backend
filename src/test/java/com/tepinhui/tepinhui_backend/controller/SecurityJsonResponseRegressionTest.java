package com.tepinhui.tepinhui_backend.controller;

import com.tepinhui.tepinhui_backend.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tepinhui.tepinhui_backend.config.ResultHttpStatusAdvice;
import com.tepinhui.tepinhui_backend.controller.admin.AdminStatsController;
import com.tepinhui.tepinhui_backend.exception.GlobalExceptionHandler;
import com.tepinhui.tepinhui_backend.security.JwtAuthenticationFilter;
import com.tepinhui.tepinhui_backend.security.JwtUtil;
import com.tepinhui.tepinhui_backend.service.AdminStatsService;
import com.tepinhui.tepinhui_backend.service.UserProfileService;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({
    UserController.class,
    AdminStatsController.class
})
@ContextConfiguration(classes = {
    UserController.class,
    AdminStatsController.class,
    GlobalExceptionHandler.class,
    ResultHttpStatusAdvice.class,
    SecurityConfig.class,
    SecurityJsonResponseRegressionTest.JwtFilterConfig.class
})
class SecurityJsonResponseRegressionTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserProfileService userProfileService;

    @MockitoBean
    private AdminStatsService adminStatsService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void anonymousShouldReceiveUnauthorizedJsonBody() throws Exception {
        mockMvc.perform(get("/api/v1/user/profile"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith("application/json"))
            .andExpect(jsonPath("$.code").value(401))
            .andExpect(jsonPath("$.message").value("未登录"));

        verifyNoInteractions(userProfileService);
    }

    @Test
    @WithMockUser(roles = "CONSUMER")
    void insufficientRoleShouldReceiveForbiddenJsonBody() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats"))
            .andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith("application/json"))
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("权限不足"));

        verifyNoInteractions(adminStatsService);
    }

    @Test
    void invalidBearerTokenShouldReceiveUnauthorizedJsonBody() throws Exception {
        when(jwtUtil.validateToken("invalid-token")).thenReturn(false);

        mockMvc.perform(get("/api/v1/user/profile")
                .header("Authorization", "Bearer invalid-token"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith("application/json"))
            .andExpect(jsonPath("$.code").value(401));

        verifyNoInteractions(userProfileService);
    }

    @TestConfiguration
    static class JwtFilterConfig {

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(JwtUtil jwtUtil,
                                                        StringRedisTemplate stringRedisTemplate,
                                                        ObjectMapper objectMapper) {
            return new JwtAuthenticationFilter(jwtUtil, stringRedisTemplate, objectMapper) {
                @Override
                protected void doFilterInternal(HttpServletRequest request,
                                                HttpServletResponse response,
                                                FilterChain filterChain) throws ServletException, IOException {
                    super.doFilterInternal(request, response, filterChain);
                }
            };
        }
    }
}
