package com.tepinhui.tepinhui_backend.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tepinhui.tepinhui_backend.config.ResultHttpStatusAdvice;
import com.tepinhui.tepinhui_backend.config.SecurityConfig;
import com.tepinhui.tepinhui_backend.exception.BusinessException;
import com.tepinhui.tepinhui_backend.exception.GlobalExceptionHandler;
import com.tepinhui.tepinhui_backend.mapper.MerchantMapper;
import com.tepinhui.tepinhui_backend.mapper.ProductMapper;
import com.tepinhui.tepinhui_backend.mapper.UserMapper;
import com.tepinhui.tepinhui_backend.security.JwtAuthenticationFilter;
import com.tepinhui.tepinhui_backend.security.JwtUtil;
import com.tepinhui.tepinhui_backend.service.ReviewService;
import com.tepinhui.tepinhui_backend.vo.review.ReviewListVO;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReviewController.class)
@ContextConfiguration(classes = {
    ReviewController.class,
    GlobalExceptionHandler.class,
    ResultHttpStatusAdvice.class,
    SecurityConfig.class,
    ReviewControllerTest.NoOpJwtFilterConfig.class
})
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewService reviewService;

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

    // ============ GET /api/v1/products/{productId}/reviews ============

    @Test
    void getProductReviews_happyPath_returns200() throws Exception {
        IPage<ReviewListVO> pageResult = buildPage(
            List.of(buildReviewListVO(10L, 1L, 50L, 5, "好评")),
            1L,
            1L,
            10L
        );
        when(reviewService.getProductReviews(eq(50L), anyInt(), anyInt())).thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/products/50/reviews")
                .param("page", "1")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.records").isArray())
            .andExpect(jsonPath("$.data.records[0].id").value(10))
            .andExpect(jsonPath("$.data.records[0].productId").value(50))
            .andExpect(jsonPath("$.data.records[0].rating").value(5))
            .andExpect(jsonPath("$.data.records[0].content").value("好评"));

        verify(reviewService).getProductReviews(eq(50L), anyInt(), anyInt());
    }

    @Test
    void getProductReviews_noReviews_returns200WithEmptyRecords() throws Exception {
        IPage<ReviewListVO> emptyPage = buildPage(List.of(), 0L, 1L, 10L);
        when(reviewService.getProductReviews(eq(99L), anyInt(), anyInt())).thenReturn(emptyPage);

        mockMvc.perform(get("/api/v1/products/99/reviews")
                .param("page", "1")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.total").value(0))
            .andExpect(jsonPath("$.data.records").isArray())
            .andExpect(jsonPath("$.data.records").isEmpty());

        verify(reviewService).getProductReviews(eq(99L), anyInt(), anyInt());
    }

    // ============ GET /api/v1/user/reviews ============

    @Test
    @WithMockUser(roles = "CONSUMER")
    void getCurrentUserReviews_consumerLoggedIn_returns200() throws Exception {
        IPage<ReviewListVO> pageResult = buildPage(
            List.of(buildReviewListVO(11L, 1L, 50L, 4, "还可以")),
            1L,
            1L,
            10L
        );
        when(reviewService.getCurrentUserReviews(anyInt(), anyInt())).thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/user/reviews")
                .param("page", "1")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.records").isArray())
            .andExpect(jsonPath("$.data.records[0].id").value(11))
            .andExpect(jsonPath("$.data.records[0].rating").value(4))
            .andExpect(jsonPath("$.data.records[0].content").value("还可以"));

        verify(reviewService).getCurrentUserReviews(anyInt(), anyInt());
    }

    @Test
    void getCurrentUserReviews_anonymous_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/user/reviews")
                .param("page", "1")
                .param("size", "10"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(401))
            .andExpect(jsonPath("$.message").value("未登录"));

        verifyNoInteractions(reviewService);
    }

    // ============ DELETE /api/v1/reviews/{id} ============

    @Test
    @WithMockUser(roles = "CONSUMER")
    void deleteReview_owner_returns200() throws Exception {
        doNothing().when(reviewService).deleteReview(10L);

        mockMvc.perform(delete("/api/v1/reviews/10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("评价删除成功"));

        verify(reviewService).deleteReview(10L);
    }

    @Test
    @WithMockUser(roles = "CONSUMER")
    void deleteReview_notOwner_returns403() throws Exception {
        doThrow(new BusinessException(403, "无权删除该评价"))
            .when(reviewService).deleteReview(20L);

        mockMvc.perform(delete("/api/v1/reviews/20"))
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("无权删除该评价"));

        verify(reviewService).deleteReview(20L);
    }

    @Test
    @WithMockUser(roles = "CONSUMER")
    void deleteReview_notFound_returns404() throws Exception {
        doThrow(new BusinessException(404, "评价不存在"))
            .when(reviewService).deleteReview(999L);

        mockMvc.perform(delete("/api/v1/reviews/999"))
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value("评价不存在"));

        verify(reviewService).deleteReview(999L);
    }

    // ============ helpers ============

    private ReviewListVO buildReviewListVO(Long id, Long userId, Long productId, Integer rating, String content) {
        ReviewListVO vo = new ReviewListVO();
        vo.setId(id);
        vo.setUserId(userId);
        vo.setUsername("用户");
        vo.setAvatarUrl(null);
        vo.setProductId(productId);
        vo.setOrderId(100L);
        vo.setRating(rating);
        vo.setContent(content);
        vo.setImages(List.of());
        vo.setCreatedAt(LocalDateTime.now());
        return vo;
    }

    private IPage<ReviewListVO> buildPage(List<ReviewListVO> records, long total, long current, long size) {
        Page<ReviewListVO> page = new Page<>(current, size);
        page.setRecords(records);
        page.setTotal(total);
        return page;
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
