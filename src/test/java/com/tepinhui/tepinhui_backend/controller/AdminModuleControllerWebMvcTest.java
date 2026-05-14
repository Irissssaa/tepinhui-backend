package com.tepinhui.tepinhui_backend.controller;

import com.tepinhui.tepinhui_backend.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tepinhui.tepinhui_backend.config.ResultHttpStatusAdvice;
import com.tepinhui.tepinhui_backend.controller.admin.AdminMerchantController;
import com.tepinhui.tepinhui_backend.controller.admin.AdminProductController;
import com.tepinhui.tepinhui_backend.controller.admin.AdminSpecialtyController;
import com.tepinhui.tepinhui_backend.controller.admin.AdminStatsController;
import com.tepinhui.tepinhui_backend.controller.admin.AdminTraceController;
import com.tepinhui.tepinhui_backend.controller.admin.AdminUserController;
import com.tepinhui.tepinhui_backend.entity.TraceRecord;
import com.tepinhui.tepinhui_backend.exception.GlobalExceptionHandler;
import com.tepinhui.tepinhui_backend.security.JwtAuthenticationFilter;
import com.tepinhui.tepinhui_backend.security.JwtUtil;
import com.tepinhui.tepinhui_backend.service.AdminStatsService;
import com.tepinhui.tepinhui_backend.service.AdminUserService;
import com.tepinhui.tepinhui_backend.service.CultureContentService;
import com.tepinhui.tepinhui_backend.service.MerchantService;
import com.tepinhui.tepinhui_backend.service.ProductService;
import com.tepinhui.tepinhui_backend.service.SpecialtyService;
import com.tepinhui.tepinhui_backend.service.TraceService;
import com.tepinhui.tepinhui_backend.vo.admin.AdminStatsVO;
import com.tepinhui.tepinhui_backend.vo.admin.AdminUserVO;
import com.tepinhui.tepinhui_backend.vo.merchant.MerchantPageVO;
import com.tepinhui.tepinhui_backend.vo.trace.TracePageVO;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({
    AdminProductController.class,
    AdminStatsController.class,
    AdminSpecialtyController.class,
    AdminMerchantController.class,
    AdminUserController.class,
    AdminTraceController.class
})
@ContextConfiguration(classes = {
    AdminProductController.class,
    AdminStatsController.class,
    AdminSpecialtyController.class,
    AdminMerchantController.class,
    AdminUserController.class,
    AdminTraceController.class,
    GlobalExceptionHandler.class,
    ResultHttpStatusAdvice.class,
    SecurityConfig.class,
    AdminModuleControllerWebMvcTest.NoOpJwtFilterConfig.class
})
class AdminModuleControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private AdminStatsService adminStatsService;

    @MockitoBean
    private SpecialtyService specialtyService;

    @MockitoBean
    private CultureContentService cultureContentService;

    @MockitoBean
    private MerchantService merchantService;

    @MockitoBean
    private AdminUserService adminUserService;

    @MockitoBean
    private TraceService traceService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void anonymousShouldBeRejectedFromAdminStats() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(401))
            .andExpect(jsonPath("$.message").value("未登录"));

        verifyNoInteractions(adminStatsService);
    }

    @Test
    @WithMockUser(roles = "MERCHANT")
    void merchantShouldBeRejectedFromAdminStats() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats"))
            .andExpect(status().isForbidden());

        verifyNoInteractions(adminStatsService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminShouldAccessAdminStats() throws Exception {
        AdminStatsVO statsVO = new AdminStatsVO();
        statsVO.setUserCount(12L);
        statsVO.setMerchantCount(4L);
        statsVO.setProductCount(9L);
        statsVO.setOrderCount(20L);
        statsVO.setTotalSalesAmount(new BigDecimal("88.80"));

        when(adminStatsService.getAdminStats()).thenReturn(statsVO);

        mockMvc.perform(get("/api/v1/admin/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.userCount").value(12))
            .andExpect(jsonPath("$.data.totalSalesAmount").value(88.8));

        verify(adminStatsService).getAdminStats();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminShouldAuditProduct() throws Exception {
        doNothing().when(productService).auditProduct(any(), any());

        mockMvc.perform(put("/api/v1/admin/products/8/audit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": "on",
                      "auditRemark": "通过"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("商品审核完成"))
            .andExpect(jsonPath("$.data").doesNotExist());

        verify(productService).auditProduct(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminShouldCreateSpecialty() throws Exception {
        doNothing().when(specialtyService).createSpecialty(any());

        mockMvc.perform(post("/api/v1/admin/specialties")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "西湖龙井",
                      "category": "名茶",
                      "seasonTag": "春季",
                      "coverImg": "https://oss.example.com/longjing.jpg",
                      "isLanding": true,
                      "originId": 1,
                      "culturalInfo": "简介"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("特产新增成功"))
            .andExpect(jsonPath("$.data").doesNotExist());

        verify(specialtyService).createSpecialty(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminShouldUpdateSpecialtyCulture() throws Exception {
        doNothing().when(cultureContentService).updateSpecialtyCulture(any(), any());

        mockMvc.perform(put("/api/v1/admin/specialties/6/culture")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "culturalInfo": "简介",
                      "cultureContents": [
                        {
                          "title": "历史",
                          "content": "内容",
                          "type": "history",
                          "status": "on"
                        }
                      ]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("特产文化内容更新成功"))
            .andExpect(jsonPath("$.data").doesNotExist());

        verify(cultureContentService).updateSpecialtyCulture(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminShouldAccessMerchantPendingList() throws Exception {
        MerchantPageVO pageVO = new MerchantPageVO();
        pageVO.setRecords(List.of());
        pageVO.setTotal(0L);
        pageVO.setPage(1L);
        pageVO.setSize(10L);

        when(merchantService.pagePendingMerchants(1L, 10L)).thenReturn(pageVO);

        mockMvc.perform(get("/api/v1/admin/merchant/pending")
                .param("page", "1")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.total").value(0));

        verify(merchantService).pagePendingMerchants(1L, 10L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminShouldCreateManagedUser() throws Exception {
        AdminUserVO userVO = new AdminUserVO();
        userVO.setId(18L);
        userVO.setUsername("admin2");
        userVO.setRole("ADMIN");

        when(adminUserService.createUser(any())).thenReturn(userVO);

        mockMvc.perform(post("/api/v1/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "admin2",
                      "phone": "13900139009",
                      "password": "Tph@123456",
                      "email": "admin2@example.com",
                      "nickname": "平台运营管理员",
                      "role": "ADMIN"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("账号创建成功"))
            .andExpect(jsonPath("$.data.id").value(18))
            .andExpect(jsonPath("$.data.role").value("ADMIN"));

        verify(adminUserService).createUser(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminShouldAccessTraceAdminList() throws Exception {
        TracePageVO pageVO = new TracePageVO();
        pageVO.setRecords(List.of());
        pageVO.setTotal(0L);
        pageVO.setPage(1L);
        pageVO.setSize(10L);

        when(traceService.getAllList(1L, 10L)).thenReturn(pageVO);

        mockMvc.perform(get("/api/v1/trace/admin/list")
                .param("page", "1")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.total").value(0));

        verify(traceService).getAllList(1L, 10L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminShouldAuditTrace() throws Exception {
        TraceRecord traceRecord = new TraceRecord();
        traceRecord.setId(15L);
        traceRecord.setTraceCode("TP-ZJ-LJ-2024-042138");

        when(traceService.auditTrace(any())).thenReturn(traceRecord);

        mockMvc.perform(put("/api/v1/trace/admin/audit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "id": 15,
                      "status": "pass"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.id").value(15))
            .andExpect(jsonPath("$.data.traceCode").value("TP-ZJ-LJ-2024-042138"));

        verify(traceService).auditTrace(any());
    }

    @TestConfiguration
    static class NoOpJwtFilterConfig {

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(JwtUtil jwtUtil,
                                                        StringRedisTemplate stringRedisTemplate,
                                                        ObjectMapper objectMapper) {
            return new JwtAuthenticationFilter(jwtUtil, stringRedisTemplate, objectMapper) {
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
