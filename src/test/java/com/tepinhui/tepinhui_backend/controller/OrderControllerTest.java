package com.tepinhui.tepinhui_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tepinhui.tepinhui_backend.config.ResultHttpStatusAdvice;
import com.tepinhui.tepinhui_backend.config.SecurityConfig;
import com.tepinhui.tepinhui_backend.dto.order.OrderCreateRequest;
import com.tepinhui.tepinhui_backend.dto.order.OrderShipRequest;
import com.tepinhui.tepinhui_backend.dto.review.ReviewCreateRequest;
import com.tepinhui.tepinhui_backend.exception.BusinessException;
import com.tepinhui.tepinhui_backend.exception.GlobalExceptionHandler;
import com.tepinhui.tepinhui_backend.mapper.MerchantMapper;
import com.tepinhui.tepinhui_backend.mapper.ProductMapper;
import com.tepinhui.tepinhui_backend.mapper.UserMapper;
import com.tepinhui.tepinhui_backend.security.JwtAuthenticationFilter;
import com.tepinhui.tepinhui_backend.security.JwtUtil;
import com.tepinhui.tepinhui_backend.service.OrderService;
import com.tepinhui.tepinhui_backend.service.ReviewService;
import com.tepinhui.tepinhui_backend.vo.order.OrderCalculateVO;
import com.tepinhui.tepinhui_backend.vo.order.OrderDetailVO;
import com.tepinhui.tepinhui_backend.vo.order.OrderItemVO;
import com.tepinhui.tepinhui_backend.vo.order.OrderPageVO;
import com.tepinhui.tepinhui_backend.vo.order.OrderPayVO;
import com.tepinhui.tepinhui_backend.vo.review.ReviewVO;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@ContextConfiguration(classes = {
    OrderController.class,
    GlobalExceptionHandler.class,
    ResultHttpStatusAdvice.class,
    SecurityConfig.class,
    OrderControllerTest.NoOpJwtFilterConfig.class
})
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

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

    // ============ POST /api/v1/orders ============

    @Test
    @WithMockUser(roles = "CONSUMER")
    void createOrder_happyPath_returns200() throws Exception {
        OrderDetailVO detailVO = buildOrderDetailVO(1L, "ORD202405151200001234", "pending", new BigDecimal("100.00"));
        when(orderService.createOrder(any(OrderCreateRequest.class))).thenReturn(detailVO);

        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "addressId": 1,
                      "items": [{"productId": 50, "quantity": 2}]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("订单创建成功"))
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.orderNo").value("ORD202405151200001234"))
            .andExpect(jsonPath("$.data.status").value("pending"));

        verify(orderService).createOrder(any(OrderCreateRequest.class));
    }

    @Test
    @WithMockUser(roles = "CONSUMER")
    void createOrder_addressNotFound_returns404() throws Exception {
        doThrow(new BusinessException(404, "收货地址不存在"))
            .when(orderService).createOrder(any(OrderCreateRequest.class));

        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "addressId": 999,
                      "items": [{"productId": 50, "quantity": 1}]
                    }
                    """))
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value("收货地址不存在"));
    }

    // ============ GET /api/v1/orders ============

    @Test
    @WithMockUser(roles = "CONSUMER")
    void getOrders_happyPath_returns200() throws Exception {
        OrderPageVO pageVO = new OrderPageVO();
        pageVO.setRecords(List.of());
        pageVO.setTotal(0L);
        pageVO.setPage(1);
        pageVO.setSize(10);
        when(orderService.getCurrentUserOrders(any())).thenReturn(pageVO);

        mockMvc.perform(get("/api/v1/orders")
                .param("page", "1")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.total").value(0))
            .andExpect(jsonPath("$.data.records").isArray());

        verify(orderService).getCurrentUserOrders(any());
    }

    // ============ GET /api/v1/orders/{id} ============

    @Test
    @WithMockUser(roles = "CONSUMER")
    void getOrderDetail_happyPath_returns200() throws Exception {
        OrderDetailVO detailVO = buildOrderDetailVO(1L, "ORD202405151200001234", "paid", new BigDecimal("200.00"));
        when(orderService.getOrderDetail(1L)).thenReturn(detailVO);

        mockMvc.perform(get("/api/v1/orders/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.status").value("paid"));

        verify(orderService).getOrderDetail(1L);
    }

    @Test
    @WithMockUser(roles = "CONSUMER")
    void getOrderDetail_notFound_returns404() throws Exception {
        when(orderService.getOrderDetail(999L)).thenThrow(new BusinessException(404, "订单不存在"));

        mockMvc.perform(get("/api/v1/orders/999"))
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value("订单不存在"));
    }

    // ============ PUT /api/v1/orders/{id}/cancel ============

    @Test
    @WithMockUser(roles = "CONSUMER")
    void cancelOrder_happyPath_returns200() throws Exception {
        doNothing().when(orderService).cancelOrder(1L);

        mockMvc.perform(put("/api/v1/orders/1/cancel"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("订单取消成功"));

        verify(orderService).cancelOrder(1L);
    }

    @Test
    @WithMockUser(roles = "CONSUMER")
    void cancelOrder_statusNotAllowed_returns400() throws Exception {
        doThrow(new BusinessException(400, "当前状态不可取消"))
            .when(orderService).cancelOrder(1L);

        mockMvc.perform(put("/api/v1/orders/1/cancel"))
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("当前状态不可取消"));
    }

    // ============ PUT /api/v1/orders/{id}/confirm ============

    @Test
    @WithMockUser(roles = "CONSUMER")
    void confirmOrder_happyPath_returns200() throws Exception {
        doNothing().when(orderService).confirmOrder(1L);

        mockMvc.perform(put("/api/v1/orders/1/confirm"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("订单确认收货成功"));

        verify(orderService).confirmOrder(1L);
    }

    @Test
    @WithMockUser(roles = "CONSUMER")
    void confirmOrder_statusNotAllowed_returns400() throws Exception {
        doThrow(new BusinessException(400, "当前状态不可确认收货"))
            .when(orderService).confirmOrder(1L);

        mockMvc.perform(put("/api/v1/orders/1/confirm"))
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("当前状态不可确认收货"));
    }

    // ============ PUT /api/v1/orders/{id}/ship ============

    @Test
    @WithMockUser(roles = "MERCHANT")
    void shipOrder_happyPath_returns200() throws Exception {
        doNothing().when(orderService).shipOrder(eq(1L), any(OrderShipRequest.class));

        mockMvc.perform(put("/api/v1/orders/1/ship")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "logisticsNo": "SF1234567890"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("订单发货成功"));

        verify(orderService).shipOrder(eq(1L), any(OrderShipRequest.class));
    }

    @Test
    @WithMockUser(roles = "MERCHANT")
    void shipOrder_notAuthorized_returns403() throws Exception {
        doThrow(new BusinessException(403, "无权操作此订单"))
            .when(orderService).shipOrder(eq(1L), any(OrderShipRequest.class));

        mockMvc.perform(put("/api/v1/orders/1/ship")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "logisticsNo": "SF1234567890"
                    }
                    """))
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("无权操作此订单"));
    }

    // ============ POST /api/v1/orders/{id}/review ============

    @Test
    @WithMockUser(roles = "CONSUMER")
    void createOrderReview_happyPath_returns200() throws Exception {
        ReviewVO reviewVO = new ReviewVO();
        reviewVO.setId(10L);
        reviewVO.setOrderId(1L);
        reviewVO.setProductId(50L);
        reviewVO.setRating(5);
        reviewVO.setContent("好评");
        when(reviewService.createOrderReview(eq(1L), any(ReviewCreateRequest.class))).thenReturn(reviewVO);

        mockMvc.perform(post("/api/v1/orders/1/review")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "rating": 5,
                      "content": "好评"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("订单评价提交成功"))
            .andExpect(jsonPath("$.data.id").value(10))
            .andExpect(jsonPath("$.data.rating").value(5));

        verify(reviewService).createOrderReview(eq(1L), any(ReviewCreateRequest.class));
    }

    @Test
    @WithMockUser(roles = "CONSUMER")
    void createOrderReview_alreadyReviewed_returns400() throws Exception {
        doThrow(new BusinessException(400, "该订单已评价"))
            .when(reviewService).createOrderReview(eq(1L), any(ReviewCreateRequest.class));

        mockMvc.perform(post("/api/v1/orders/1/review")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "rating": 5,
                      "content": "好评"
                    }
                    """))
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("该订单已评价"));
    }

    // ============ POST /api/v1/orders/calculate ============

    @Test
    @WithMockUser(roles = "CONSUMER")
    void calculateOrder_happyPath_returns200() throws Exception {
        OrderCalculateVO calculateVO = OrderCalculateVO.builder()
            .items(List.of())
            .subtotal(new BigDecimal("100.00"))
            .shippingFee(BigDecimal.ZERO)
            .discount(BigDecimal.ZERO)
            .totalAmount(new BigDecimal("100.00"))
            .build();
        when(orderService.calculateOrder(any())).thenReturn(calculateVO);

        mockMvc.perform(post("/api/v1/orders/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "addressId": 1,
                      "items": [{"productId": 50, "quantity": 2}]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.totalAmount").value(100.00));

        verify(orderService).calculateOrder(any());
    }

    @Test
    @WithMockUser(roles = "CONSUMER")
    void calculateOrder_productNotFound_returns404() throws Exception {
        doThrow(new BusinessException(404, "商品不存在: 50"))
            .when(orderService).calculateOrder(any());

        mockMvc.perform(post("/api/v1/orders/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "addressId": 1,
                      "items": [{"productId": 50, "quantity": 1}]
                    }
                    """))
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value("商品不存在: 50"));
    }

    // ============ POST /api/v1/orders/{id}/pay ============

    @Test
    @WithMockUser(roles = "CONSUMER")
    void payOrder_happyPath_returns200() throws Exception {
        OrderPayVO payVO = OrderPayVO.builder()
            .orderId(1L)
            .orderNo("ORD202405151200001234")
            .totalAmount(new BigDecimal("100.00"))
            .status("success")
            .build();
        when(orderService.payOrder(eq(1L), any())).thenReturn(payVO);

        mockMvc.perform(post("/api/v1/orders/1/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "payMethod": "mock"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.orderId").value(1))
            .andExpect(jsonPath("$.data.status").value("success"));

        verify(orderService).payOrder(eq(1L), any());
    }

    @Test
    @WithMockUser(roles = "CONSUMER")
    void payOrder_statusNotAllowed_returns400() throws Exception {
        doThrow(new BusinessException(400, "当前状态不可支付"))
            .when(orderService).payOrder(eq(1L), any());

        mockMvc.perform(post("/api/v1/orders/1/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "payMethod": "mock"
                    }
                    """))
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("当前状态不可支付"));
    }

    // ============ helpers ============

    private OrderDetailVO buildOrderDetailVO(Long id, String orderNo, String status, BigDecimal totalAmount) {
        OrderDetailVO vo = new OrderDetailVO();
        vo.setId(id);
        vo.setOrderNo(orderNo);
        vo.setStatus(status);
        vo.setTotalAmount(totalAmount);
        vo.setAddress("{\"consignee\":\"张三\"}");
        vo.setItems(List.of());
        return vo;
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
