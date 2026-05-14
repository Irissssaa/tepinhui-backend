package com.tepinhui.tepinhui_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tepinhui.tepinhui_backend.common.UserStatus;
import com.tepinhui.tepinhui_backend.dto.cart.CartAddRequest;
import com.tepinhui.tepinhui_backend.dto.cart.CartUpdateRequest;
import com.tepinhui.tepinhui_backend.entity.CartItem;
import com.tepinhui.tepinhui_backend.entity.Product;
import com.tepinhui.tepinhui_backend.entity.User;
import com.tepinhui.tepinhui_backend.exception.BusinessException;
import com.tepinhui.tepinhui_backend.mapper.CartItemMapper;
import com.tepinhui.tepinhui_backend.mapper.ProductMapper;
import com.tepinhui.tepinhui_backend.mapper.UserMapper;
import com.tepinhui.tepinhui_backend.service.CartService;
import com.tepinhui.tepinhui_backend.vo.cart.CartItemVO;
import com.tepinhui.tepinhui_backend.vo.cart.CartVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    // 设计决策：商品下架/删除时不联动清理 cart_item，由 getCurrentUserCart 在展示层过滤。
    // 后续如需主动清理，可在 ProductServiceImpl 下架/删除路径追加 cartItemMapper.delete(... product_id=...)。

    private final UserMapper userMapper;
    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void addCartItem(CartAddRequest request) {
        User user = getCurrentUser();

        Product product = productMapper.selectById(request.getProductId());
        if (product == null) {
            throw new BusinessException(404, "商品不存在");
        }
        if (!"on".equals(product.getStatus())) {
            throw new BusinessException(400, "商品已下架，无法加入购物车");
        }

        CartItem existing = cartItemMapper.selectOne(
            new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, user.getId())
                .eq(CartItem::getProductId, request.getProductId())
                .last("LIMIT 1")
        );

        int newQuantity = (existing == null ? 0 : existing.getQuantity()) + request.getQuantity();
        if (newQuantity > product.getStock()) {
            throw new BusinessException(400, "购物车数量超过库存");
        }

        if (existing == null) {
            CartItem newItem = new CartItem();
            newItem.setUserId(user.getId());
            newItem.setProductId(request.getProductId());
            newItem.setQuantity(request.getQuantity());
            try {
                cartItemMapper.insert(newItem);
            } catch (DuplicateKeyException e) {
                // 并发场景：另一个请求已为同一 (user_id, product_id) 插入了一行
                // 重试一次「查 + 累加 + update」逻辑
                try {
                    CartItem retryExisting = cartItemMapper.selectOne(
                        new LambdaQueryWrapper<CartItem>()
                            .eq(CartItem::getUserId, user.getId())
                            .eq(CartItem::getProductId, request.getProductId())
                            .last("LIMIT 1")
                    );
                    if (retryExisting == null) {
                        // 极小概率竞态：再次插入
                        CartItem retryItem = new CartItem();
                        retryItem.setUserId(user.getId());
                        retryItem.setProductId(request.getProductId());
                        retryItem.setQuantity(request.getQuantity());
                        cartItemMapper.insert(retryItem);
                    } else {
                        int retryQuantity = retryExisting.getQuantity() + request.getQuantity();
                        if (retryQuantity > product.getStock()) {
                            throw new BusinessException(400, "购物车数量超过库存");
                        }
                        retryExisting.setQuantity(retryQuantity);
                        cartItemMapper.updateById(retryExisting);
                    }
                } catch (DuplicateKeyException ex) {
                    throw new BusinessException(500, "加入购物车失败，请重试");
                }
            }
        } else {
            existing.setQuantity(newQuantity);
            cartItemMapper.updateById(existing);
        }
    }

    @Override
    public CartVO getCurrentUserCart() {
        User user = getCurrentUser();

        List<CartItem> items = cartItemMapper.selectList(
            new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, user.getId())
                .orderByDesc(CartItem::getCreatedAt)
        );

        CartVO cartVO = new CartVO();
        if (items.isEmpty()) {
            cartVO.setItems(List.of());
            cartVO.setTotalQuantity(0);
            cartVO.setTotalAmount(BigDecimal.ZERO);
            return cartVO;
        }

        List<Long> productIds = items.stream()
            .map(CartItem::getProductId)
            .distinct()
            .toList();
        Map<Long, Product> productMap = productMapper.selectBatchIds(productIds)
            .stream()
            .collect(Collectors.toMap(Product::getId, p -> p));

        List<CartItemVO> visibleItems = new ArrayList<>();
        int totalQuantity = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem item : items) {
            Product product = productMap.get(item.getProductId());
            if (product == null) {
                log.warn("购物车项被跳过: cartItemId={}, productId={}, reason=product_deleted",
                    item.getId(), item.getProductId());
                continue;
            }
            if (!"on".equals(product.getStatus())) {
                log.warn("购物车项被跳过: cartItemId={}, productId={}, reason=status_{}",
                    item.getId(), item.getProductId(), product.getStatus());
                continue;
            }

            BigDecimal subtotal = product.getPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));

            CartItemVO vo = new CartItemVO();
            vo.setId(item.getId());
            vo.setProductId(product.getId());
            vo.setProductName(product.getName());
            vo.setPrice(product.getPrice());
            vo.setQuantity(item.getQuantity());
            vo.setSubtotal(subtotal);
            vo.setImageUrl(extractFirstImage(product.getImages()));

            visibleItems.add(vo);
            totalQuantity += item.getQuantity();
            totalAmount = totalAmount.add(subtotal);
        }

        cartVO.setItems(visibleItems);
        cartVO.setTotalQuantity(totalQuantity);
        cartVO.setTotalAmount(totalAmount);
        return cartVO;
    }

    @Override
    public void updateCartItemQuantity(Long id, CartUpdateRequest request) {
        User user = getCurrentUser();

        CartItem item = cartItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(404, "购物车项不存在");
        }
        if (!item.getUserId().equals(user.getId())) {
            throw new BusinessException(403, "无权操作他人购物车");
        }

        Product product = productMapper.selectById(item.getProductId());
        if (product == null) {
            throw new BusinessException(400, "商品已下架，请删除该购物车项");
        }
        if (!"on".equals(product.getStatus())) {
            throw new BusinessException(400, "商品已下架，请删除该购物车项");
        }

        if (request.getQuantity() > product.getStock()) {
            throw new BusinessException(400, "购物车数量超过库存");
        }

        item.setQuantity(request.getQuantity());
        cartItemMapper.updateById(item);
    }

    @Override
    public void deleteCartItem(Long id) {
        User user = getCurrentUser();

        CartItem item = cartItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(404, "购物车项不存在");
        }
        if (!item.getUserId().equals(user.getId())) {
            throw new BusinessException(403, "无权操作他人购物车");
        }

        cartItemMapper.deleteById(id);
    }

    @Override
    public void clearCurrentUserCart() {
        User user = getCurrentUser();

        cartItemMapper.delete(
            new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, user.getId())
        );
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

    private String extractFirstImage(String images) {
        if (images == null) {
            return null;
        }
        String trimmed = images.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        if (trimmed.startsWith("[")) {
            try {
                List<String> parsed = objectMapper.readValue(trimmed, new TypeReference<List<String>>() {});
                if (parsed != null && !parsed.isEmpty()) {
                    String first = parsed.get(0);
                    if (first == null) {
                        return null;
                    }
                    String firstTrimmed = first.trim();
                    return firstTrimmed.isEmpty() ? null : firstTrimmed;
                }
            } catch (JsonProcessingException e) {
                // 解析失败走兜底逻辑
                log.warn("商品 images 字段 JSON 解析失败，走兜底逻辑: images={}, err={}", trimmed, e.getMessage());
            }
        }

        String first = trimmed.split(",")[0].trim();
        return first.isEmpty() ? null : first;
    }
}
