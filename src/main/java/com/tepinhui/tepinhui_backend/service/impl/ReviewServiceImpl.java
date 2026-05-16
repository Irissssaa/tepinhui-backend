package com.tepinhui.tepinhui_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import com.tepinhui.tepinhui_backend.service.ReviewService;
import com.tepinhui.tepinhui_backend.vo.review.ReviewListVO;
import com.tepinhui.tepinhui_backend.vo.review.ReviewVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewMapper reviewMapper;
    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    @Override
    public ReviewVO createOrderReview(Long orderId, ReviewCreateRequest request) {
        // 1. 获取当前登录用户
        User user = getCurrentUser();
        Long userId = user.getId();

        // 2. 查询订单（null -> 404）
        Orders order = ordersMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }

        // 3. 校验订单归属（userId 不匹配 -> 403）
        if (!userId.equals(order.getUserId())) {
            throw new BusinessException(403, "无权操作此订单");
        }

        // 4. 校验订单状态为 DONE
        if (order.getStatus() != OrderStatus.DONE) {
            throw new BusinessException(400, "只有已完成订单可评价");
        }

        // 5. 查询订单明细（获取 product_id 列表）
        List<OrderItem> orderItems = orderItemMapper.selectList(
            new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId)
        );
        if (orderItems.isEmpty()) {
            throw new BusinessException(400, "订单无商品明细");
        }

        // 6. 校验该订单是否已评价
        Review existingReview = reviewMapper.selectOne(
            new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderId, orderId)
                .eq(Review::getUserId, userId)
                .last("LIMIT 1")
        );
        if (existingReview != null) {
            throw new BusinessException(400, "该订单已评价");
        }

        // 7. 序列化 images
        String imagesJson = null;
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            try {
                imagesJson = objectMapper.writeValueAsString(request.getImages());
            } catch (JsonProcessingException e) {
                log.error("评价图片序列化失败", e);
                throw new BusinessException(500, "评价图片数据处理失败");
            }
        }

        // 8. 为每个商品创建评价记录
        List<Review> reviews = new ArrayList<>();
        for (OrderItem item : orderItems) {
            Review review = new Review();
            review.setUserId(userId);
            review.setProductId(item.getProductId());
            review.setOrderId(orderId);
            review.setRating(request.getRating());
            review.setContent(request.getContent());
            review.setImages(imagesJson);
            reviews.add(review);
        }
        for (Review review : reviews) {
            reviewMapper.insert(review);
        }

        // 9. 组装 ReviewVO 返回（取第一个商品的评价）
        Review firstReview = reviews.get(0);
        ReviewVO vo = new ReviewVO();
        vo.setId(firstReview.getId());
        vo.setOrderId(firstReview.getOrderId());
        vo.setProductId(firstReview.getProductId());
        vo.setRating(firstReview.getRating());
        vo.setContent(firstReview.getContent());
        vo.setImages(request.getImages());
        vo.setCreatedAt(firstReview.getCreatedAt());
        return vo;
    }

    @Override
    public IPage<ReviewListVO> getProductReviews(Long productId, int page, int size) {
        throw new UnsupportedOperationException("待 T03 实现");
    }

    @Override
    public IPage<ReviewListVO> getCurrentUserReviews(int page, int size) {
        throw new UnsupportedOperationException("待 T04 实现");
    }

    @Override
    public void deleteReview(Long reviewId) {
        throw new UnsupportedOperationException("待 T05 实现");
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(401, "未登录");
        }
        if (!(authentication.getPrincipal() instanceof String username) || !StringUtils.hasText(username)) {
            throw new BusinessException(401, "登录状态无效");
        }

        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .last("LIMIT 1")
        );
        if (user == null) {
            throw new BusinessException(404, "当前用户不存在");
        }
        if (user.getStatus() != null && user.getStatus() == UserStatus.DISABLED) {
            throw new BusinessException(403, "账号已被禁用");
        }
        return user;
    }
}
