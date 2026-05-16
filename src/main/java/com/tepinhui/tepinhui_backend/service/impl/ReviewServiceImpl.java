package com.tepinhui.tepinhui_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        // 1. 分页查询商品评价（按创建时间倒序）
        Page<Review> reviewPage = reviewMapper.selectPage(
            new Page<>(page, size),
            new LambdaQueryWrapper<Review>()
                .eq(Review::getProductId, productId)
                .orderByDesc(Review::getCreatedAt)
        );

        List<Review> reviews = reviewPage.getRecords();
        if (reviews == null || reviews.isEmpty()) {
            // 无数据时直接 convert 返回空 IPage（records 为空、total=0）
            return reviewPage.convert(r -> new ReviewListVO());
        }

        // 2. 批量查询用户信息，避免 N+1
        Set<Long> userIds = reviews.stream()
            .map(Review::getUserId)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty()
            ? Collections.emptyMap()
            : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        // 3. 逐条组装 ReviewListVO
        return reviewPage.convert(review -> toReviewListVO(review, userMap));
    }

    private ReviewListVO toReviewListVO(Review review, Map<Long, User> userMap) {
        ReviewListVO vo = new ReviewListVO();
        vo.setId(review.getId());
        vo.setUserId(review.getUserId());
        vo.setProductId(review.getProductId());
        vo.setOrderId(review.getOrderId());
        vo.setRating(review.getRating());
        vo.setContent(review.getContent());
        vo.setCreatedAt(review.getCreatedAt());

        // 用户信息：优先使用昵称，缺失时回退用户名，再做脱敏
        User user = review.getUserId() == null ? null : userMap.get(review.getUserId());
        if (user != null) {
            String displayName = StringUtils.hasText(user.getNickname())
                ? user.getNickname()
                : user.getUsername();
            vo.setUsername(maskUsername(displayName));
            vo.setAvatarUrl(user.getAvatarUrl());
        } else {
            vo.setUsername("");
        }

        // images 在 DB 中为 JSON 字符串，反序列化为 List<String>
        vo.setImages(parseImages(review.getImages()));
        return vo;
    }

    /**
     * 用户名脱敏：保留首字符 + 三个 *；长度 ≤ 1 原样返回；空值返回空字符串。
     */
    private String maskUsername(String name) {
        if (!StringUtils.hasText(name)) {
            return "";
        }
        if (name.length() <= 1) {
            return name;
        }
        return name.charAt(0) + "***";
    }

    /**
     * 将存储在 review.images 中的 JSON 字符串反序列化为 List<String>。
     * 解析失败或为空时返回空列表，不阻断列表查询。
     */
    private List<String> parseImages(String imagesJson) {
        if (!StringUtils.hasText(imagesJson)) {
            return Collections.emptyList();
        }
        try {
            List<String> images = objectMapper.readValue(imagesJson, new TypeReference<List<String>>() {});
            return images == null ? Collections.emptyList() : images;
        } catch (JsonProcessingException e) {
            log.warn("评价图片反序列化失败，已降级为空列表: imagesJson={}", imagesJson, e);
            return Collections.emptyList();
        }
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
