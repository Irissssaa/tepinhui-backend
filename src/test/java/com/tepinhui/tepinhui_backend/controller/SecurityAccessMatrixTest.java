package com.tepinhui.tepinhui_backend.controller;

import com.tepinhui.tepinhui_backend.common.Result;
import com.tepinhui.tepinhui_backend.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tepinhui.tepinhui_backend.config.ResultHttpStatusAdvice;
import com.tepinhui.tepinhui_backend.controller.admin.AdminStatsController;
import com.tepinhui.tepinhui_backend.controller.admin.AdminTraceController;
import com.tepinhui.tepinhui_backend.entity.TraceRecord;
import com.tepinhui.tepinhui_backend.exception.GlobalExceptionHandler;
import com.tepinhui.tepinhui_backend.security.JwtAuthenticationFilter;
import com.tepinhui.tepinhui_backend.security.JwtUtil;
import com.tepinhui.tepinhui_backend.service.AdminStatsService;
import com.tepinhui.tepinhui_backend.service.CartService;
import com.tepinhui.tepinhui_backend.service.MapService;
import com.tepinhui.tepinhui_backend.service.MerchantService;
import com.tepinhui.tepinhui_backend.service.TraceService;
import com.tepinhui.tepinhui_backend.vo.admin.AdminStatsVO;
import com.tepinhui.tepinhui_backend.vo.cart.CartVO;
import com.tepinhui.tepinhui_backend.vo.map.SpecialtyMapVO;
import com.tepinhui.tepinhui_backend.vo.merchant.MerchantStatsVO;
import com.tepinhui.tepinhui_backend.vo.trace.TracePageVO;
import com.tepinhui.tepinhui_backend.vo.trace.TraceQueryVO;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({
    MapController.class,
    CartController.class,
    MerchantController.class,
    TraceController.class,
    AdminTraceController.class,
    AdminStatsController.class
})
@ContextConfiguration(classes = {
    MapController.class,
    CartController.class,
    MerchantController.class,
    TraceController.class,
    AdminTraceController.class,
    AdminStatsController.class,
    GlobalExceptionHandler.class,
    ResultHttpStatusAdvice.class,
    SecurityConfig.class,
    SecurityAccessMatrixTest.NoOpJwtFilterConfig.class
})
class SecurityAccessMatrixTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MapService mapService;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private MerchantService merchantService;

    @MockitoBean
    private AdminStatsService adminStatsService;

    @MockitoBean
    private TraceService traceService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void anonymousShouldAccessPublicMapEndpoint() throws Exception {
        when(mapService.listSpecialties()).thenReturn(List.of(new SpecialtyMapVO()));

        mockMvc.perform(get("/api/v1/map/specialties"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        verify(mapService).listSpecialties();
    }

    @Test
    void anonymousShouldAccessPublicTraceQuery() throws Exception {
        TraceQueryVO queryVO = new TraceQueryVO();
        queryVO.setTraceCode("TP-ZJ-LJ-2024-042138");
        when(traceService.getTraceInfo("TP-ZJ-LJ-2024-042138")).thenReturn(queryVO);

        mockMvc.perform(get("/api/v1/trace/TP-ZJ-LJ-2024-042138"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.traceCode").value("TP-ZJ-LJ-2024-042138"));

        verify(traceService).getTraceInfo("TP-ZJ-LJ-2024-042138");
    }

    @Test
    @WithMockUser(roles = "CONSUMER")
    void consumerShouldAccessCartEndpoint() throws Exception {
        CartVO cartVO = new CartVO();
        cartVO.setItems(List.of());
        cartVO.setTotalQuantity(0);
        cartVO.setTotalAmount(BigDecimal.ZERO);
        when(cartService.getCurrentUserCart()).thenReturn(cartVO);

        mockMvc.perform(get("/api/v1/cart"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.totalQuantity").value(0));

        verify(cartService).getCurrentUserCart();
    }

    @Test
    @WithMockUser(roles = "MERCHANT")
    void merchantShouldAccessMerchantStatsEndpoint() throws Exception {
        MerchantStatsVO statsVO = new MerchantStatsVO();
        statsVO.setProductCount(0L);
        statsVO.setOrderCount(0L);
        statsVO.setSalesAmount(BigDecimal.ZERO);
        statsVO.setViewCount(0L);
        when(merchantService.getCurrentMerchantStats()).thenReturn(statsVO);

        mockMvc.perform(get("/api/v1/merchant/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.productCount").value(0));

        verify(merchantService).getCurrentMerchantStats();
    }

    @Test
    @WithMockUser(roles = "MERCHANT")
    void merchantShouldAccessTraceInputEndpoint() throws Exception {
        TraceRecord traceRecord = new TraceRecord();
        traceRecord.setId(9L);
        traceRecord.setTraceCode("TP-ZJ-LJ-2024-042138");
        when(traceService.inputTrace(any(), anyLong())).thenReturn(traceRecord);

        mockMvc.perform(post("/api/v1/trace")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "productId": 1,
                      "batchNo": "BATCH-001"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.traceCode").value("TP-ZJ-LJ-2024-042138"));

        verify(traceService).inputTrace(any(), anyLong());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminShouldAccessAdminStatsEndpoint() throws Exception {
        AdminStatsVO statsVO = new AdminStatsVO();
        statsVO.setUserCount(0L);
        statsVO.setMerchantCount(0L);
        statsVO.setProductCount(0L);
        statsVO.setOrderCount(0L);
        statsVO.setSalesAmount(BigDecimal.ZERO);
        when(adminStatsService.getAdminStats()).thenReturn(statsVO);

        mockMvc.perform(get("/api/v1/admin/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.userCount").value(0));

        verify(adminStatsService).getAdminStats();
    }

    @Test
    void anonymousShouldBeRejectedFromTraceAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/trace/admin/list"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(401))
            .andExpect(jsonPath("$.message").value("未登录"));

        verifyNoInteractions(traceService);
    }

    @Test
    @WithMockUser(roles = "MERCHANT")
    void merchantShouldBeRejectedFromTraceAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/trace/admin/list"))
            .andExpect(status().isForbidden());

        verifyNoInteractions(traceService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminShouldAccessTraceAdminEndpoint() throws Exception {
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
