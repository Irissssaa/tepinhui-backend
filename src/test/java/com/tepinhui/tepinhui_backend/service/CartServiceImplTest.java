package com.tepinhui.tepinhui_backend.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
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
import com.tepinhui.tepinhui_backend.service.impl.CartServiceImpl;
import com.tepinhui.tepinhui_backend.vo.cart.CartVO;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private CartItemMapper cartItemMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ObjectMapper objectMapper;

    private CartServiceImpl cartService;

    private User testUser;

    @BeforeEach
    void setUp() {
        cartService = new CartServiceImpl(userMapper, cartItemMapper, productMapper, objectMapper);

        TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(new MybatisConfiguration(), "cart-service-test"),
            CartItem.class
        );
        TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(new MybatisConfiguration(), "cart-service-test-user"),
            User.class
        );

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

    // ============ addCartItem ============

    @Test
    void addCartItem_productNotFound_throws404() {
        when(productMapper.selectById(1L)).thenReturn(null);

        CartAddRequest request = buildAddRequest(1L, 1);
        BusinessException ex = assertThrows(BusinessException.class, () -> cartService.addCartItem(request));

        assertEquals(404, ex.getCode());
        assertEquals("商品不存在", ex.getMessage());
        verify(cartItemMapper, never()).insert(any(CartItem.class));
        verify(cartItemMapper, never()).updateById(any(CartItem.class));
    }

    @Test
    void addCartItem_productOffline_throws400() {
        Product offlineProduct = buildProduct(2L, "off", new BigDecimal("10.00"), 10);
        when(productMapper.selectById(2L)).thenReturn(offlineProduct);

        CartAddRequest request = buildAddRequest(2L, 1);
        BusinessException ex = assertThrows(BusinessException.class, () -> cartService.addCartItem(request));

        assertEquals(400, ex.getCode());
        assertEquals("商品已下架，无法加入购物车", ex.getMessage());
        verify(cartItemMapper, never()).insert(any(CartItem.class));
        verify(cartItemMapper, never()).updateById(any(CartItem.class));
    }

    @Test
    void addCartItem_overStock_throws400() {
        Product product = buildProduct(3L, "on", new BigDecimal("10.00"), 5);
        when(productMapper.selectById(3L)).thenReturn(product);
        when(cartItemMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        CartAddRequest request = buildAddRequest(3L, 6);
        BusinessException ex = assertThrows(BusinessException.class, () -> cartService.addCartItem(request));

        assertEquals(400, ex.getCode());
        assertEquals("购物车数量超过库存", ex.getMessage());
        verify(cartItemMapper, never()).insert(any(CartItem.class));
        verify(cartItemMapper, never()).updateById(any(CartItem.class));
    }

    @Test
    void addCartItem_newItem_callsInsert() {
        Product product = buildProduct(4L, "on", new BigDecimal("10.00"), 10);
        when(productMapper.selectById(4L)).thenReturn(product);
        when(cartItemMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(cartItemMapper.insert(any(CartItem.class))).thenReturn(1);

        CartAddRequest request = buildAddRequest(4L, 2);
        cartService.addCartItem(request);

        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemMapper).insert(captor.capture());
        CartItem inserted = captor.getValue();
        assertEquals(100L, inserted.getUserId());
        assertEquals(4L, inserted.getProductId());
        assertEquals(2, inserted.getQuantity());
        verify(cartItemMapper, never()).updateById(any(CartItem.class));
    }

    @Test
    void addCartItem_existingItem_mergesQuantityAndCallsUpdate() {
        Product product = buildProduct(5L, "on", new BigDecimal("10.00"), 10);
        when(productMapper.selectById(5L)).thenReturn(product);

        CartItem existing = new CartItem();
        existing.setId(900L);
        existing.setUserId(100L);
        existing.setProductId(5L);
        existing.setQuantity(2);
        when(cartItemMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);
        when(cartItemMapper.updateById(any(CartItem.class))).thenReturn(1);

        CartAddRequest request = buildAddRequest(5L, 3);
        cartService.addCartItem(request);

        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemMapper).updateById(captor.capture());
        CartItem updated = captor.getValue();
        assertEquals(900L, updated.getId());
        assertEquals(5, updated.getQuantity());
        verify(cartItemMapper, never()).insert(any(CartItem.class));
    }

    // ============ getCurrentUserCart ============

    @Test
    void getCurrentUserCart_empty_returnsZero() {
        when(cartItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        CartVO vo = cartService.getCurrentUserCart();

        assertNotNull(vo);
        assertNotNull(vo.getItems());
        assertTrue(vo.getItems().isEmpty());
        assertEquals(0, vo.getTotalQuantity());
        assertEquals(0, BigDecimal.ZERO.compareTo(vo.getTotalAmount()));
        verify(productMapper, never()).selectBatchIds(anyList());
    }

    @Test
    void getCurrentUserCart_skipsDeletedProduct() {
        CartItem item1 = buildCartItem(801L, 100L, 11L, 2);
        CartItem item2 = buildCartItem(802L, 100L, 12L, 3);
        when(cartItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(item1, item2));

        Product onlyProduct = buildProduct(11L, "on", new BigDecimal("10.00"), 100);
        onlyProduct.setName("茶叶A");
        // productId=12 不在 productMap 中（被删除）
        when(productMapper.selectBatchIds(anyList())).thenReturn(List.of(onlyProduct));

        CartVO vo = cartService.getCurrentUserCart();

        assertEquals(1, vo.getItems().size());
        assertEquals(11L, vo.getItems().get(0).getProductId());
        assertEquals(2, vo.getTotalQuantity());
        assertEquals(0, new BigDecimal("20.00").compareTo(vo.getTotalAmount()));
    }

    @Test
    void getCurrentUserCart_skipsOffStatusProduct() {
        CartItem item1 = buildCartItem(811L, 100L, 21L, 2);
        CartItem item2 = buildCartItem(812L, 100L, 22L, 4);
        when(cartItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(item1, item2));

        Product onProduct = buildProduct(21L, "on", new BigDecimal("5.00"), 100);
        onProduct.setName("茶叶ON");
        Product offProduct = buildProduct(22L, "off", new BigDecimal("9.00"), 100);
        offProduct.setName("茶叶OFF");
        when(productMapper.selectBatchIds(anyList())).thenReturn(List.of(onProduct, offProduct));

        CartVO vo = cartService.getCurrentUserCart();

        assertEquals(1, vo.getItems().size());
        assertEquals(21L, vo.getItems().get(0).getProductId());
        assertEquals(2, vo.getTotalQuantity());
        assertEquals(0, new BigDecimal("10.00").compareTo(vo.getTotalAmount()));
    }

    @Test
    void getCurrentUserCart_calculatesSubtotalAndTotal() {
        CartItem item = buildCartItem(821L, 100L, 31L, 3);
        when(cartItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(item));

        Product product = buildProduct(31L, "on", new BigDecimal("10.50"), 100);
        product.setName("龙井");
        // 不设置 images，使 extractFirstImage 走 null 早返回
        when(productMapper.selectBatchIds(anyList())).thenReturn(List.of(product));

        CartVO vo = cartService.getCurrentUserCart();

        assertEquals(1, vo.getItems().size());
        assertEquals(3, vo.getTotalQuantity());
        // BigDecimal.compareTo 比较，不用 equals (避免 31.50 vs 31.5 精度差异)
        assertEquals(0, new BigDecimal("31.50").compareTo(vo.getItems().get(0).getSubtotal()));
        assertEquals(0, new BigDecimal("31.50").compareTo(vo.getTotalAmount()));
        assertEquals(31L, vo.getItems().get(0).getProductId());
        assertEquals("龙井", vo.getItems().get(0).getProductName());
        assertNull(vo.getItems().get(0).getImageUrl());
    }

    // ============ updateCartItemQuantity ============

    @Test
    void updateQuantity_notFound_throws404() {
        when(cartItemMapper.selectById(500L)).thenReturn(null);

        CartUpdateRequest request = buildUpdateRequest(1);
        BusinessException ex = assertThrows(BusinessException.class,
            () -> cartService.updateCartItemQuantity(500L, request));

        assertEquals(404, ex.getCode());
        assertEquals("购物车项不存在", ex.getMessage());
        verify(cartItemMapper, never()).updateById(any(CartItem.class));
    }

    @Test
    void updateQuantity_notOwner_throws403() {
        CartItem item = buildCartItem(501L, 999L, 41L, 1); // userId=999 != 100
        when(cartItemMapper.selectById(501L)).thenReturn(item);

        CartUpdateRequest request = buildUpdateRequest(2);
        BusinessException ex = assertThrows(BusinessException.class,
            () -> cartService.updateCartItemQuantity(501L, request));

        assertEquals(403, ex.getCode());
        assertEquals("无权操作他人购物车", ex.getMessage());
        verify(cartItemMapper, never()).updateById(any(CartItem.class));
    }

    @Test
    void updateQuantity_overStock_throws400() {
        CartItem item = buildCartItem(502L, 100L, 42L, 1);
        when(cartItemMapper.selectById(502L)).thenReturn(item);
        Product product = buildProduct(42L, "on", new BigDecimal("10.00"), 3);
        when(productMapper.selectById(42L)).thenReturn(product);

        CartUpdateRequest request = buildUpdateRequest(5);
        BusinessException ex = assertThrows(BusinessException.class,
            () -> cartService.updateCartItemQuantity(502L, request));

        assertEquals(400, ex.getCode());
        assertEquals("购物车数量超过库存", ex.getMessage());
        verify(cartItemMapper, never()).updateById(any(CartItem.class));
    }

    @Test
    void updateQuantity_success_callsUpdateById() {
        CartItem item = buildCartItem(503L, 100L, 43L, 1);
        when(cartItemMapper.selectById(503L)).thenReturn(item);
        Product product = buildProduct(43L, "on", new BigDecimal("10.00"), 10);
        when(productMapper.selectById(43L)).thenReturn(product);
        when(cartItemMapper.updateById(any(CartItem.class))).thenReturn(1);

        CartUpdateRequest request = buildUpdateRequest(7);
        cartService.updateCartItemQuantity(503L, request);

        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemMapper).updateById(captor.capture());
        assertEquals(503L, captor.getValue().getId());
        assertEquals(7, captor.getValue().getQuantity());
    }

    // ============ deleteCartItem ============

    @Test
    void deleteCartItem_notFound_throws404() {
        when(cartItemMapper.selectById(600L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> cartService.deleteCartItem(600L));

        assertEquals(404, ex.getCode());
        assertEquals("购物车项不存在", ex.getMessage());
        verify(cartItemMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void deleteCartItem_notOwner_throws403() {
        CartItem item = buildCartItem(601L, 999L, 51L, 1); // userId=999 != 100
        when(cartItemMapper.selectById(601L)).thenReturn(item);

        BusinessException ex = assertThrows(BusinessException.class, () -> cartService.deleteCartItem(601L));

        assertEquals(403, ex.getCode());
        assertEquals("无权操作他人购物车", ex.getMessage());
        verify(cartItemMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void deleteCartItem_success_callsDeleteById() {
        CartItem item = buildCartItem(602L, 100L, 52L, 1);
        when(cartItemMapper.selectById(602L)).thenReturn(item);
        when(cartItemMapper.deleteById(602L)).thenReturn(1);

        cartService.deleteCartItem(602L);

        verify(cartItemMapper).deleteById(602L);
    }

    // ============ clearCurrentUserCart ============

    @Test
    void clearCart_empty_doesNotThrow() {
        when(cartItemMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(0);

        // 不抛异常即可
        cartService.clearCurrentUserCart();

        verify(cartItemMapper).delete(any(LambdaQueryWrapper.class));
    }

    @Test
    void clearCart_callsDeleteWithUserIdFilter() {
        when(cartItemMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(3);

        cartService.clearCurrentUserCart();

        ArgumentCaptor<LambdaQueryWrapper<CartItem>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(cartItemMapper).delete(captor.capture());
        assertTrue(captor.getValue().getSqlSegment().contains("user_id"));
    }

    // ============ helpers ============

    private void authenticate(String username) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
            username,
            null,
            List.of()
        ));
        SecurityContextHolder.setContext(context);
    }

    private CartAddRequest buildAddRequest(Long productId, int quantity) {
        CartAddRequest request = new CartAddRequest();
        request.setProductId(productId);
        request.setQuantity(quantity);
        return request;
    }

    private CartUpdateRequest buildUpdateRequest(int quantity) {
        CartUpdateRequest request = new CartUpdateRequest();
        request.setQuantity(quantity);
        return request;
    }

    private Product buildProduct(Long id, String status, BigDecimal price, int stock) {
        Product product = new Product();
        product.setId(id);
        product.setStatus(status);
        product.setPrice(price);
        product.setStock(stock);
        return product;
    }

    private CartItem buildCartItem(Long id, Long userId, Long productId, int quantity) {
        CartItem item = new CartItem();
        item.setId(id);
        item.setUserId(userId);
        item.setProductId(productId);
        item.setQuantity(quantity);
        return item;
    }
}
