package com.tepinhui.tepinhui_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tepinhui.tepinhui_backend.config.ResultHttpStatusAdvice;
import com.tepinhui.tepinhui_backend.config.SecurityConfig;
import com.tepinhui.tepinhui_backend.dto.cart.CartUpdateRequest;
import com.tepinhui.tepinhui_backend.exception.BusinessException;
import com.tepinhui.tepinhui_backend.exception.GlobalExceptionHandler;
import com.tepinhui.tepinhui_backend.mapper.MerchantMapper;
import com.tepinhui.tepinhui_backend.mapper.ProductMapper;
import com.tepinhui.tepinhui_backend.mapper.UserMapper;
import com.tepinhui.tepinhui_backend.security.JwtAuthenticationFilter;
import com.tepinhui.tepinhui_backend.security.JwtUtil;
import com.tepinhui.tepinhui_backend.service.CartService;
import com.tepinhui.tepinhui_backend.vo.cart.CartItemVO;
import com.tepinhui.tepinhui_backend.vo.cart.CartVO;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
@ContextConfiguration(classes = {
    CartController.class,
    GlobalExceptionHandler.class,
    ResultHttpStatusAdvice.class,
    SecurityConfig.class,
    CartControllerTest.NoOpJwtFilterConfig.class
})
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private MerchantMapper merchantMapper;

    @MockitoBean
    private ProductMapper productMapper;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @Test
    @WithMockUser(roles = "CONSUMER")
    void addCart_quantityZero_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/cart")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "productId": 1,
                          "quantity": 0
                        }
                        """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("数量")));

        verifyNoInteractions(cartService);
    }

    @Test
    @WithMockUser(roles = "CONSUMER")
    void addCart_productIdNull_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/cart")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "quantity": 1
                        }
                        """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("商品ID不能为空")));

        verifyNoInteractions(cartService);
    }

    @Test
    @WithMockUser(roles = "CONSUMER")
    void getCart_happyPath_returns200() throws Exception {
        CartItemVO itemVO = new CartItemVO();
        itemVO.setId(11L);
        itemVO.setProductId(101L);
        itemVO.setProductName("龙井");
        itemVO.setPrice(new BigDecimal("10.50"));
        itemVO.setQuantity(3);
        itemVO.setSubtotal(new BigDecimal("31.50"));

        CartVO cartVO = new CartVO();
        cartVO.setItems(List.of(itemVO));
        cartVO.setTotalQuantity(3);
        cartVO.setTotalAmount(new BigDecimal("31.50"));

        when(cartService.getCurrentUserCart()).thenReturn(cartVO);

        mockMvc.perform(get("/api/v1/cart"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.totalQuantity").value(3))
            .andExpect(jsonPath("$.data.items[0].id").value(11))
            .andExpect(jsonPath("$.data.items[0].productName").value("龙井"));

        verify(cartService).getCurrentUserCart();
    }

    @Test
    @WithMockUser(roles = "CONSUMER")
    void updateCart_serviceThrowsBusinessException_returnsMatchingStatus() throws Exception {
        doThrow(new BusinessException(403, "无权操作他人购物车"))
            .when(cartService).updateCartItemQuantity(eq(99L), any(CartUpdateRequest.class));

        mockMvc.perform(put("/api/v1/cart/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "quantity": 2
                        }
                        """))
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("无权操作他人购物车"));

        verify(cartService).updateCartItemQuantity(eq(99L), any(CartUpdateRequest.class));
    }

    @Test
    @WithMockUser(roles = "CONSUMER")
    void clearCart_returns200() throws Exception {
        doNothing().when(cartService).clearCurrentUserCart();

        mockMvc.perform(delete("/api/v1/cart"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("购物车已清空"));

        verify(cartService).clearCurrentUserCart();
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
