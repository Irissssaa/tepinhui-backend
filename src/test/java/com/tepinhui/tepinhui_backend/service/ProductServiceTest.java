package com.tepinhui.tepinhui_backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tepinhui.tepinhui_backend.common.Role;
import com.tepinhui.tepinhui_backend.dto.product.ProductCreateRequest;
import com.tepinhui.tepinhui_backend.dto.product.ProductQueryRequest;
import com.tepinhui.tepinhui_backend.dto.product.ProductUpdateRequest;
import com.tepinhui.tepinhui_backend.entity.Merchant;
import com.tepinhui.tepinhui_backend.entity.Product;
import com.tepinhui.tepinhui_backend.entity.User;
import com.tepinhui.tepinhui_backend.exception.BusinessException;
import com.tepinhui.tepinhui_backend.mapper.MerchantMapper;
import com.tepinhui.tepinhui_backend.mapper.ProductMapper;
import com.tepinhui.tepinhui_backend.mapper.UserMapper;
import com.tepinhui.tepinhui_backend.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private MerchantMapper merchantMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        productService = new ProductServiceImpl(productMapper, userMapper, merchantMapper, redisTemplate);
        TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(new MybatisConfiguration(), "product-service-test"),
            Product.class
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void pageProductsShouldQueryOnlyOnShelfProducts() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        Product product = new Product();
        product.setId(101L);
        product.setName("西湖龙井");
        product.setStatus("on");
        product.setPrice(new BigDecimal("88.00"));

        Page<Product> mapperPage = new Page<>(2, 5);
        mapperPage.setRecords(List.of(product));
        mapperPage.setTotal(1L);

        when(valueOperations.get(anyString())).thenReturn(null);
        when(productMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mapperPage);

        ProductQueryRequest request = new ProductQueryRequest();
        request.setPage(2L);
        request.setSize(5L);
        request.setKeyword("龙井");
        request.setSpecialtyId(9L);
        request.setStatus("off");

        var result = productService.pageProducts(request);

        assertEquals(1, result.getRecords().size());
        assertEquals("on", result.getRecords().get(0).getStatus());
        assertEquals(2L, result.getPage());
        assertEquals(5L, result.getSize());

        ArgumentCaptor<LambdaQueryWrapper<Product>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(productMapper).selectPage(any(Page.class), wrapperCaptor.capture());
        assertTrue(wrapperCaptor.getValue().getSqlSegment().contains("status"));
        assertTrue(wrapperCaptor.getValue().getSqlSegment().contains("name"));
        assertTrue(wrapperCaptor.getValue().getSqlSegment().contains("specialty"));
        verify(valueOperations).set(eq("product:list:page:2:size:5:keyword:%E9%BE%99%E4%BA%95:specialtyId:9"), any(), eq(java.time.Duration.ofMinutes(5)));
    }

    @Test
    void getProductDetailShouldRejectMissingOrOffShelfProduct() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("product:detail:7")).thenReturn(null);
        when(productMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> productService.getProductDetail(7L));

        assertEquals(404, exception.getCode());
        assertEquals("商品不存在或未上架", exception.getMessage());

        ArgumentCaptor<LambdaQueryWrapper<Product>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(productMapper).selectOne(wrapperCaptor.capture());
        assertTrue(wrapperCaptor.getValue().getSqlSegment().contains("id"));
        assertTrue(wrapperCaptor.getValue().getSqlSegment().contains("status"));
    }

    @Test
    void createProductShouldUseReviewStatusAndEvictListCache() {
        mockMerchantAuth("merchantA", 300L, 900L);
        when(redisTemplate.keys("product:list:*")).thenReturn(Set.of("product:list:page:1:size:10:keyword:all:specialtyId:all"));
        when(productMapper.insert(any(Product.class))).thenAnswer(invocation -> {
            Product inserted = invocation.getArgument(0);
            inserted.setId(501L);
            return 1;
        });

        ProductCreateRequest request = new ProductCreateRequest();
        request.setSpecialtyId(19L);
        request.setName("宣莲礼盒");
        request.setDescription("新上架");
        request.setPrice(new BigDecimal("59.90"));
        request.setStock(20);
        request.setImages("[\"cover.png\"]");

        Long productId = productService.createProduct(request);

        assertEquals(501L, productId);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).insert(productCaptor.capture());
        assertEquals(900L, productCaptor.getValue().getMerchantId());
        assertEquals("review", productCaptor.getValue().getStatus());
        verify(redisTemplate).keys("product:list:*");
        verify(redisTemplate).delete(Set.of("product:list:page:1:size:10:keyword:all:specialtyId:all"));
    }

    @Test
    void createProductShouldRejectConsumerWrite() {
        authenticate("consumerA", "ROLE_CONSUMER");
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(buildUser(701L, "consumerA", Role.CONSUMER));

        ProductCreateRequest request = new ProductCreateRequest();
        request.setSpecialtyId(1L);
        request.setName("测试商品");
        request.setPrice(new BigDecimal("12.50"));
        request.setStock(3);

        BusinessException exception = assertThrows(BusinessException.class, () -> productService.createProduct(request));

        assertEquals(403, exception.getCode());
        assertEquals("当前用户无权操作商品", exception.getMessage());
        verify(productMapper, never()).insert(any(Product.class));
    }

    @Test
    void updateProductShouldRejectOtherMerchant() {
        mockMerchantAuth("merchantB", 301L, 901L);
        when(productMapper.selectById(77L)).thenReturn(buildProduct(77L, 999L, "on"));

        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setSpecialtyId(2L);
        request.setName("更新后商品");
        request.setPrice(new BigDecimal("99.00"));
        request.setStock(5);

        BusinessException exception = assertThrows(BusinessException.class, () -> productService.updateProduct(77L, request));

        assertEquals(403, exception.getCode());
        assertEquals("无权操作其他商家的商品", exception.getMessage());
        verify(productMapper, never()).updateById(any(Product.class));
    }

    @Test
    void updateProductShouldEvictDetailAndListCaches() {
        mockMerchantAuth("merchantC", 302L, 902L);
        when(redisTemplate.keys("product:list:*")).thenReturn(Set.of("product:list:page:1:size:10:keyword:all:specialtyId:all"));
        when(productMapper.selectById(88L)).thenReturn(buildProduct(88L, 902L, "review"));
        when(productMapper.updateById(any(Product.class))).thenReturn(1);

        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setSpecialtyId(8L);
        request.setName("山核桃");
        request.setDescription("更新描述");
        request.setPrice(new BigDecimal("108.00"));
        request.setStock(9);
        request.setImages("[\"new.png\"]");

        productService.updateProduct(88L, request);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).updateById(productCaptor.capture());
        assertEquals("山核桃", productCaptor.getValue().getName());
        verify(redisTemplate).delete("product:detail:88");
        verify(redisTemplate).keys("product:list:*");
        verify(redisTemplate).delete(Set.of("product:list:page:1:size:10:keyword:all:specialtyId:all"));
    }

    @Test
    void deleteProductShouldEvictDetailAndListCaches() {
        mockMerchantAuth("merchantD", 303L, 903L);
        when(redisTemplate.keys("product:list:*")).thenReturn(Set.of("product:list:page:2:size:5:keyword:all:specialtyId:6"));
        when(productMapper.selectById(99L)).thenReturn(buildProduct(99L, 903L, "review"));
        when(productMapper.deleteById(99L)).thenReturn(1);

        productService.deleteProduct(99L);

        verify(productMapper).deleteById(99L);
        verify(redisTemplate).delete("product:detail:99");
        verify(redisTemplate).keys("product:list:*");
        verify(redisTemplate).delete(Set.of("product:list:page:2:size:5:keyword:all:specialtyId:6"));
    }

    private void mockMerchantAuth(String username, Long userId, Long merchantId) {
        authenticate(username, "ROLE_MERCHANT");
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(buildUser(userId, username, Role.MERCHANT));
        Merchant merchant = new Merchant();
        merchant.setId(merchantId);
        merchant.setUserId(userId);
        merchant.setStatus("approved");
        when(merchantMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(merchant);
    }

    private void authenticate(String username, String authority) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
            username,
            null,
            List.of(new SimpleGrantedAuthority(authority))
        ));
        SecurityContextHolder.setContext(context);
    }

    private User buildUser(Long id, String username, Role role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRole(role);
        return user;
    }

    private Product buildProduct(Long id, Long merchantId, String status) {
        Product product = new Product();
        product.setId(id);
        product.setMerchantId(merchantId);
        product.setStatus(status);
        return product;
    }
}
