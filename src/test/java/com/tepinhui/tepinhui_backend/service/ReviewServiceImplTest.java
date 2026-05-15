package com.tepinhui.tepinhui_backend.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tepinhui.tepinhui_backend.common.OrderStatus;
import com.tepinhui.tepinhui_backend.common.UserStatus;
import com.tepinhui.tepinhui_backend.dto.review.ReviewCreateRequest;
import com.tepinhui.tepinhui_backend.entity.OrderItem;
import com.tepinhui.tepinhui_backend.entity.Orders;
import com.tepinhui.tepinhui_backend.entity.Review;
import com.tepinhui.tepinhui_backend.entity.User;
import com.tepinhui.tepinhui_backend.exception.BusinessException;
import com.tepinhui.tepinhui_backend.mapper.OrderItemMapper;
import com.tepinhui.tepinhui_backend.mapper.OrdersMapper;
import com.tepinhui.tepinhui_backend.mapper.ReviewMapper;
import com.tepinhui.tepinhui_backend.mapper.UserMapper;
import com.tepinhui.tepinhui_backend.service.impl.ReviewServiceImpl;
import com.tepinhui.tepinhui_backend.vo.review.ReviewVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private OrdersMapper ordersMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    @Mock
    private UserMapper userMapper;

    private ReviewServiceImpl reviewService;

    private User testUser;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        reviewService = new ReviewServiceImpl(
            reviewMapper, ordersMapper, orderItemMapper, userMapper, objectMapper
        );

        TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(new MybatisConfiguration(), "review-test"), Review.class);
        TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(new MybatisConfiguration(), "review-test-orders"), Orders.class);
        TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(new MybatisConfiguration(), "review-test-order-item"), OrderItem.class);
        TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(new MybatisConfiguration(), "review-test-user"), User.class);

        authenticate("testuser");

        testUser = new User();
        testUser.setId(100L);
        testUser.setUsername("testuser");
        testUser.setStatus(UserStatus.ENABLED);
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createOrderReview_orderNotFound_throws404() {
        when(ordersMapper.selectById(999L)).thenReturn(null);

        ReviewCreateRequest request = buildReviewRequest(5, "好评");
        BusinessException ex = assertThrows(BusinessException.class,
            () -> reviewService.createOrderReview(999L, request));

        assertEquals(404, ex.getCode());
        assertEquals("订单不存在", ex.getMessage());
        verify(orderItemMapper, never()).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void createOrderReview_orderNotOwned_throws403() {
        Orders order = buildOrder(1L, 999L, OrderStatus.DONE); // userId=999 != 100
        when(ordersMapper.selectById(1L)).thenReturn(order);

        ReviewCreateRequest request = buildReviewRequest(5, "好评");
        BusinessException ex = assertThrows(BusinessException.class,
            () -> reviewService.createOrderReview(1L, request));

        assertEquals(403, ex.getCode());
        assertEquals("无权操作此订单", ex.getMessage());
    }

    @Test
    void createOrderReview_statusNotDone_throws400() {
        Orders order = buildOrder(1L, 100L, OrderStatus.PAID); // status != DONE
        when(ordersMapper.selectById(1L)).thenReturn(order);

        ReviewCreateRequest request = buildReviewRequest(5, "好评");
        BusinessException ex = assertThrows(BusinessException.class,
            () -> reviewService.createOrderReview(1L, request));

        assertEquals(400, ex.getCode());
        assertEquals("只有已完成订单可评价", ex.getMessage());
    }

    @Test
    void createOrderReview_alreadyReviewed_throws400() {
        Orders order = buildOrder(1L, 100L, OrderStatus.DONE);
        when(ordersMapper.selectById(1L)).thenReturn(order);

        OrderItem item = buildOrderItem(1L, 1L, 50L, 2);
        when(orderItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(item));

        // Existing review found
        Review existingReview = new Review();
        existingReview.setId(10L);
        when(reviewMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingReview);

        ReviewCreateRequest request = buildReviewRequest(5, "好评");
        BusinessException ex = assertThrows(BusinessException.class,
            () -> reviewService.createOrderReview(1L, request));

        assertEquals(400, ex.getCode());
        assertEquals("该订单已评价", ex.getMessage());
        verify(reviewMapper, never()).insert(any(Review.class));
    }

    @Test
    void createOrderReview_happyPath_createsReviews() {
        Orders order = buildOrder(1L, 100L, OrderStatus.DONE);
        when(ordersMapper.selectById(1L)).thenReturn(order);

        OrderItem item1 = buildOrderItem(1L, 1L, 50L, 2);
        OrderItem item2 = buildOrderItem(2L, 1L, 51L, 1);
        when(orderItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(item1, item2));

        // No existing review
        when(reviewMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        // Simulate id being set on insert
        when(reviewMapper.insert(any(Review.class))).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            review.setId(100L + review.getProductId());
            return 1;
        });

        ReviewCreateRequest request = buildReviewRequest(5, "商品不错，值得购买");
        ReviewVO result = reviewService.createOrderReview(1L, request);

        assertNotNull(result);
        assertEquals(1L, result.getOrderId());
        assertEquals(5, result.getRating());
        assertEquals("商品不错，值得购买", result.getContent());

        // Verify two reviews inserted (one per order item) and capture them
        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewMapper, org.mockito.Mockito.times(2)).insert(captor.capture());

        // Verify the first review's details
        Review firstInserted = captor.getAllValues().get(0);
        assertEquals(100L, firstInserted.getUserId());
        assertEquals(1L, firstInserted.getOrderId());
        assertEquals(5, firstInserted.getRating());

        // Verify second review
        Review secondInserted = captor.getAllValues().get(1);
        assertEquals(100L, secondInserted.getUserId());
        assertEquals(1L, secondInserted.getOrderId());
        assertEquals(51L, secondInserted.getProductId());
    }

    // ============ helpers ============

    private void authenticate(String username) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
            username, null, List.of()));
        SecurityContextHolder.setContext(context);
    }

    private Orders buildOrder(Long id, Long userId, OrderStatus status) {
        Orders order = new Orders();
        order.setId(id);
        order.setUserId(userId);
        order.setStatus(status);
        order.setOrderNo("ORD202405151200001234");
        order.setTotalAmount(new BigDecimal("100.00"));
        return order;
    }

    private OrderItem buildOrderItem(Long id, Long orderId, Long productId, int quantity) {
        OrderItem item = new OrderItem();
        item.setId(id);
        item.setOrderId(orderId);
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setUnitPrice(new BigDecimal("50.00"));
        item.setSubtotal(new BigDecimal("100.00"));
        return item;
    }

    private ReviewCreateRequest buildReviewRequest(int rating, String content) {
        ReviewCreateRequest request = new ReviewCreateRequest();
        request.setRating(rating);
        request.setContent(content);
        return request;
    }
}
