package com.tepinhui.tepinhui_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
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
import com.tepinhui.tepinhui_backend.service.ProductService;
import com.tepinhui.tepinhui_backend.vo.product.ProductDetailVO;
import com.tepinhui.tepinhui_backend.vo.product.ProductListVO;
import com.tepinhui.tepinhui_backend.vo.product.ProductPageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final String PRODUCT_STATUS_ON = "on";
    private static final String PRODUCT_STATUS_REVIEW = "review";
    private static final String MERCHANT_STATUS_APPROVED = "approved";
    private static final String PRODUCT_DETAIL_CACHE_PREFIX = "product:detail:";
    private static final String PRODUCT_LIST_CACHE_PREFIX = "product:list:";
    private static final Duration PRODUCT_DETAIL_CACHE_TTL = Duration.ofMinutes(10);
    private static final Duration PRODUCT_LIST_CACHE_TTL = Duration.ofMinutes(5);

    private final ProductMapper productMapper;
    private final UserMapper userMapper;
    private final MerchantMapper merchantMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public ProductPageVO pageProducts(ProductQueryRequest request) {
        ProductQueryRequest query = request == null ? new ProductQueryRequest() : request;
        String cacheKey = buildProductListCacheKey(query);

        ProductPageVO cachedPage = getCachedValue(cacheKey, ProductPageVO.class);
        if (cachedPage != null) {
            return cachedPage;
        }

        IPage<Product> productPage = productMapper.selectPage(
            new Page<>(query.getPage(), query.getSize()),
            buildPublicListQuery(query)
        );

        ProductPageVO pageVO = new ProductPageVO();
        pageVO.setRecords(productPage.getRecords().stream()
            .map(this::toListVO)
            .collect(Collectors.toList()));
        pageVO.setTotal(productPage.getTotal());
        pageVO.setPage(productPage.getCurrent());
        pageVO.setSize(productPage.getSize());

        cacheValue(cacheKey, pageVO, PRODUCT_LIST_CACHE_TTL);
        return pageVO;
    }

    @Override
    public ProductDetailVO getProductDetail(Long id) {
        if (id == null) {
            throw new BusinessException(400, "商品ID不能为空");
        }

        String cacheKey = buildProductDetailCacheKey(id);
        ProductDetailVO cachedDetail = getCachedValue(cacheKey, ProductDetailVO.class);
        if (cachedDetail != null) {
            return cachedDetail;
        }

        Product product = productMapper.selectOne(
            new LambdaQueryWrapper<Product>()
                .eq(Product::getId, id)
                .eq(Product::getStatus, PRODUCT_STATUS_ON)
                .last("LIMIT 1")
        );

        if (product == null) {
            throw new BusinessException(404, "商品不存在或未上架");
        }

        ProductDetailVO detailVO = toDetailVO(product);
        cacheValue(cacheKey, detailVO, PRODUCT_DETAIL_CACHE_TTL);
        return detailVO;
    }

    @Override
    public Long createProduct(ProductCreateRequest request) {
        if (request == null) {
            throw new BusinessException(400, "商品创建参数不能为空");
        }

        Merchant merchant = getApprovedCurrentMerchant();

        Product product = new Product();
        product.setMerchantId(merchant.getId());
        product.setSpecialtyId(request.getSpecialtyId());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setImages(request.getImages());
        product.setStatus(PRODUCT_STATUS_REVIEW);

        int inserted = productMapper.insert(product);
        if (inserted != 1 || product.getId() == null) {
            throw new BusinessException(500, "创建商品失败");
        }

        evictProductListCaches();
        return product.getId();
    }

    @Override
    public void updateProduct(Long id, ProductUpdateRequest request) {
        if (id == null) {
            throw new BusinessException(400, "商品ID不能为空");
        }
        if (request == null) {
            throw new BusinessException(400, "商品更新参数不能为空");
        }

        AuthContext authContext = getRequiredAuthContext();
        Product product = getProductOrThrow(id);
        validateWritePermission(authContext, product);

        product.setSpecialtyId(request.getSpecialtyId());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setImages(request.getImages());
        product.setUpdatedAt(LocalDateTime.now());

        int updated = productMapper.updateById(product);
        if (updated != 1) {
            throw new BusinessException(500, "更新商品失败");
        }

        evictProductCaches(product.getId());
    }

    @Override
    public void deleteProduct(Long id) {
        if (id == null) {
            throw new BusinessException(400, "商品ID不能为空");
        }

        AuthContext authContext = getRequiredAuthContext();
        Product product = getProductOrThrow(id);
        validateWritePermission(authContext, product);

        int deleted = productMapper.deleteById(product.getId());
        if (deleted != 1) {
            throw new BusinessException(500, "删除商品失败");
        }

        evictProductCaches(product.getId());
    }

    private LambdaQueryWrapper<Product> buildPublicListQuery(ProductQueryRequest request) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
            .eq(Product::getStatus, PRODUCT_STATUS_ON)
            .orderByDesc(Product::getUpdatedAt)
            .orderByDesc(Product::getId);

        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.like(Product::getName, request.getKeyword().trim());
        }

        if (request.getSpecialtyId() != null) {
            wrapper.eq(Product::getSpecialtyId, request.getSpecialtyId());
        }

        // ProductQueryRequest 当前没有可安全落地的公开产地/分类筛选字段；
        // 若后续补充 specialty.category 或 specialty.origin_id 查询条件，再扩展联查 SQL。
        return wrapper;
    }

    private ProductListVO toListVO(Product product) {
        ProductListVO vo = new ProductListVO();
        BeanUtils.copyProperties(product, vo);
        return vo;
    }

    private ProductDetailVO toDetailVO(Product product) {
        ProductDetailVO vo = new ProductDetailVO();
        BeanUtils.copyProperties(product, vo);
        return vo;
    }

    private String buildProductDetailCacheKey(Long id) {
        return PRODUCT_DETAIL_CACHE_PREFIX + id;
    }

    private String buildProductListCacheKey(ProductQueryRequest request) {
        long page = request.getPage() == null ? 1L : request.getPage();
        long size = request.getSize() == null ? 10L : request.getSize();
        String keyword = normalizeCacheSegment(request.getKeyword());
        String specialtyId = request.getSpecialtyId() == null
            ? "all"
            : request.getSpecialtyId().toString();

        // 公开列表当前固定只返回 status=on，因此 key 只包含真实影响结果集的筛选条件。
        return PRODUCT_LIST_CACHE_PREFIX
            + "page:" + page
            + ":size:" + size
            + ":keyword:" + keyword
            + ":specialtyId:" + specialtyId;
    }

    private String normalizeCacheSegment(String value) {
        if (!StringUtils.hasText(value)) {
            return "all";
        }
        return URLEncoder.encode(value.trim(), StandardCharsets.UTF_8);
    }

    private <T> T getCachedValue(String cacheKey, Class<T> targetType) {
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (targetType.isInstance(cached)) {
                return targetType.cast(cached);
            }
            if (cached != null) {
                redisTemplate.delete(cacheKey);
                log.warn("Unexpected cache value type for key {}", cacheKey);
            }
        } catch (RuntimeException ex) {
            log.warn("Failed to read cache key {}", cacheKey, ex);
        }
        return null;
    }

    private void cacheValue(String cacheKey, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(cacheKey, value, ttl);
        } catch (RuntimeException ex) {
            log.warn("Failed to write cache key {}", cacheKey, ex);
        }
    }

    private void evictProductCaches(Long productId) {
        evictCacheKey(buildProductDetailCacheKey(productId));
        evictProductListCaches();
    }

    private void evictProductListCaches() {
        try {
            Set<String> keys = redisTemplate.keys(PRODUCT_LIST_CACHE_PREFIX + "*");
            if (keys == null || keys.isEmpty()) {
                return;
            }
            redisTemplate.delete(keys);
        } catch (RuntimeException ex) {
            log.warn("Failed to evict product list caches", ex);
        }
    }

    private void evictCacheKey(String cacheKey) {
        try {
            redisTemplate.delete(cacheKey);
        } catch (RuntimeException ex) {
            log.warn("Failed to evict cache key {}", cacheKey, ex);
        }
    }

    private Product getProductOrThrow(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(404, "商品不存在");
        }
        return product;
    }

    private void validateWritePermission(AuthContext authContext, Product product) {
        if (authContext.admin()) {
            return;
        }

        Merchant merchant = authContext.merchant();
        if (merchant == null) {
            throw new BusinessException(403, "当前用户没有可用的商家身份");
        }
        if (!merchant.getId().equals(product.getMerchantId())) {
            throw new BusinessException(403, "无权操作其他商家的商品");
        }
    }

    private Merchant getApprovedCurrentMerchant() {
        AuthContext authContext = getRequiredAuthContext();
        if (authContext.admin()) {
            throw new BusinessException(403, "管理员不能直接创建商家商品");
        }
        if (authContext.merchant() == null) {
            throw new BusinessException(403, "当前用户不是已审核通过的商家");
        }
        return authContext.merchant();
    }

    private AuthContext getRequiredAuthContext() {
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

        boolean isAdmin = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch("ROLE_ADMIN"::equals);

        if (isAdmin || user.getRole() == Role.ADMIN) {
            return new AuthContext(user, null, true);
        }

        if (user.getRole() != Role.MERCHANT) {
            throw new BusinessException(403, "当前用户无权操作商品");
        }

        Merchant merchant = merchantMapper.selectOne(
            new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getUserId, user.getId())
                .eq(Merchant::getStatus, MERCHANT_STATUS_APPROVED)
                .last("LIMIT 1")
        );
        if (merchant == null) {
            throw new BusinessException(403, "当前用户不是已审核通过的商家");
        }

        return new AuthContext(user, merchant, false);
    }

    private record AuthContext(User user, Merchant merchant, boolean admin) {
    }
}
