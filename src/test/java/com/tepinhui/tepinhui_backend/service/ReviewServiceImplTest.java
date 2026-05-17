package com.tepinhui.tepinhui_backend.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tepinhui.tepinhui_backend.common.OrderStatus;
import com.tepinhui.tepinhui_backend.common.Role;
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
import com.tepinhui.tepinhui_backend.vo.review.ReviewListVO;
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
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
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
        testUser.setNickname("测试用户");
        testUser.setAvatarUrl("https://cdn.example.com/avatar/100.png");
        testUser.setStatus(UserStatus.ENABLED);
        testUser.setRole(Role.CONSUMER);
        lenient().when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);
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

    // ============ getProductReviews ============

    @Test
    void getProductReviews_hasReviews_returnsPage() {
        Long productId = 50L;

        Review r1 = buildReview(1L, 200L, productId, 1L, 5, "好评");
        Review r2 = buildReview(2L, 201L, productId, 2L, 4, "不错");

        Page<Review> mockPage = new Page<>(1, 10);
        mockPage.setTotal(2);
        mockPage.setRecords(List.of(r1, r2));
        when(reviewMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        User u1 = buildUser(200L, "alice", "Alice昵称");
        User u2 = buildUser(201L, "bob", null);
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(u1, u2));

        IPage<ReviewListVO> result = reviewService.getProductReviews(productId, 1, 10);

        assertNotNull(result);
        assertEquals(2L, result.getTotal());
        assertEquals(2, result.getRecords().size());

        ReviewListVO first = result.getRecords().get(0);
        assertEquals(1L, first.getId());
        assertEquals(productId, first.getProductId());
        assertEquals(5, first.getRating());
        assertEquals("好评", first.getContent());
        // 昵称优先：Alice昵称 -> 脱敏后保留首字符 + ***
        assertEquals("A***", first.getUsername());

        ReviewListVO second = result.getRecords().get(1);
        // 无昵称回退到 username -> bob -> b***
        assertEquals("b***", second.getUsername());
    }

    @Test
    void getProductReviews_noReviews_returnsEmpty() {
        Long productId = 51L;

        Page<Review> emptyPage = new Page<>(1, 10);
        emptyPage.setTotal(0);
        emptyPage.setRecords(Collections.emptyList());
        when(reviewMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(emptyPage);

        IPage<ReviewListVO> result = reviewService.getProductReviews(productId, 1, 10);

        assertNotNull(result);
        assertEquals(0L, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
        // 无数据不应触发用户批量查询
        verify(userMapper, never()).selectBatchIds(any());
    }

    @Test
    void getProductReviews_pagination() {
        Long productId = 52L;

        Review r3 = buildReview(3L, 202L, productId, 3L, 3, "一般");
        Page<Review> mockPage = new Page<>(2, 1);
        mockPage.setTotal(2);
        mockPage.setRecords(List.of(r3));
        when(reviewMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        User u3 = buildUser(202L, "carol", null);
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(u3));

        IPage<ReviewListVO> result = reviewService.getProductReviews(productId, 2, 1);

        assertNotNull(result);
        assertEquals(2L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals(3L, result.getRecords().get(0).getId());
        assertEquals(2, result.getCurrent());
        assertEquals(1, result.getSize());
    }

    // ============ getCurrentUserReviews ============

    @Test
    void getCurrentUserReviews_hasReviews() {
        Review r1 = buildReview(10L, 100L, 50L, 1L, 5, "好");
        Review r2 = buildReview(11L, 100L, 51L, 2L, 4, "中");

        Page<Review> mockPage = new Page<>(1, 10);
        mockPage.setTotal(2);
        mockPage.setRecords(List.of(r1, r2));
        when(reviewMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        IPage<ReviewListVO> result = reviewService.getCurrentUserReviews(1, 10);

        assertNotNull(result);
        assertEquals(2L, result.getTotal());
        assertEquals(2, result.getRecords().size());

        ReviewListVO first = result.getRecords().get(0);
        assertEquals(10L, first.getId());
        assertEquals(100L, first.getUserId());
        assertEquals(50L, first.getProductId());
        // 当前用户昵称“测试用户”脱敏：保留首字符 + ***
        assertEquals("测***", first.getUsername());
        assertEquals("https://cdn.example.com/avatar/100.png", first.getAvatarUrl());

        // 不应再次批量查询用户（直接使用 currentUser）
        verify(userMapper, never()).selectBatchIds(any());
    }

    @Test
    void getCurrentUserReviews_noReviews() {
        Page<Review> emptyPage = new Page<>(1, 10);
        emptyPage.setTotal(0);
        emptyPage.setRecords(Collections.emptyList());
        when(reviewMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(emptyPage);

        IPage<ReviewListVO> result = reviewService.getCurrentUserReviews(1, 10);

        assertNotNull(result);
        assertEquals(0L, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }

    // ============ deleteReview ============

    @Test
    void deleteReview_ownByCurrentUser_success() {
        Review review = buildReview(20L, 100L, 50L, 1L, 5, "好");
        when(reviewMapper.selectById(20L)).thenReturn(review);
        // testUser 在 setUp 中已为 CONSUMER 且 id=100，与 review.userId 一致
        when(reviewMapper.deleteById(20L)).thenReturn(1);

        reviewService.deleteReview(20L);

        verify(reviewMapper).deleteById(20L);
    }

    @Test
    void deleteReview_ownByOther_throws403() {
        // review.userId=999 != currentUser.id=100，应抛 403
        Review review = buildReview(21L, 999L, 50L, 1L, 5, "好");
        when(reviewMapper.selectById(21L)).thenReturn(review);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> reviewService.deleteReview(21L));

        assertEquals(403, ex.getCode());
        assertEquals("无权删除该评价", ex.getMessage());
        verify(reviewMapper, never()).deleteById(anyLong());
    }

    @Test
    void deleteReview_adminDeleteAny_success() {
        // 切换当前用户为管理员
        testUser.setRole(Role.ADMIN);

        Review review = buildReview(22L, 999L, 50L, 1L, 5, "好");
        when(reviewMapper.selectById(22L)).thenReturn(review);
        when(reviewMapper.deleteById(22L)).thenReturn(1);

        reviewService.deleteReview(22L);

        verify(reviewMapper).deleteById(22L);
    }

    @Test
    void deleteReview_notFound_throws404() {
        when(reviewMapper.selectById(404L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> reviewService.deleteReview(404L));

        assertEquals(404, ex.getCode());
        assertEquals("评价不存在", ex.getMessage());
        verify(reviewMapper, never()).deleteById(anyLong());
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

    private Review buildReview(Long id, Long userId, Long productId, Long orderId, int rating, String content) {
        Review review = new Review();
        review.setId(id);
        review.setUserId(userId);
        review.setProductId(productId);
        review.setOrderId(orderId);
        review.setRating(rating);
        review.setContent(content);
        review.setCreatedAt(LocalDateTime.now());
        return review;
    }

    private User buildUser(Long id, String username, String nickname) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setNickname(nickname);
        user.setStatus(UserStatus.ENABLED);
        return user;
    }
}
