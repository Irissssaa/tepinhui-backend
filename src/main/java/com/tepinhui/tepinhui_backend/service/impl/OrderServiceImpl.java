package com.tepinhui.tepinhui_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tepinhui.tepinhui_backend.common.OrderStatus;
import com.tepinhui.tepinhui_backend.common.UserStatus;
import com.tepinhui.tepinhui_backend.dto.order.OrderCalculateRequest;
import com.tepinhui.tepinhui_backend.dto.order.OrderCreateRequest;
import com.tepinhui.tepinhui_backend.dto.order.OrderPayRequest;
import com.tepinhui.tepinhui_backend.dto.order.OrderQueryRequest;
import com.tepinhui.tepinhui_backend.dto.order.OrderShipRequest;
import com.tepinhui.tepinhui_backend.entity.Address;
import com.tepinhui.tepinhui_backend.entity.OrderItem;
import com.tepinhui.tepinhui_backend.entity.Orders;
import com.tepinhui.tepinhui_backend.entity.Product;
import com.tepinhui.tepinhui_backend.entity.User;
import com.tepinhui.tepinhui_backend.exception.BusinessException;
import com.tepinhui.tepinhui_backend.mapper.AddressMapper;
import com.tepinhui.tepinhui_backend.mapper.OrderItemMapper;
import com.tepinhui.tepinhui_backend.mapper.OrdersMapper;
import com.tepinhui.tepinhui_backend.mapper.ProductMapper;
import com.tepinhui.tepinhui_backend.mapper.UserMapper;
import com.tepinhui.tepinhui_backend.mapper.MerchantMapper;
import com.tepinhui.tepinhui_backend.entity.Merchant;
import com.tepinhui.tepinhui_backend.common.Role;
import com.tepinhui.tepinhui_backend.service.OrderService;
import com.tepinhui.tepinhui_backend.vo.order.OrderCalculateItemVO;
import com.tepinhui.tepinhui_backend.vo.order.OrderCalculateVO;
import com.tepinhui.tepinhui_backend.vo.order.OrderDetailVO;
import com.tepinhui.tepinhui_backend.vo.order.OrderItemVO;
import com.tepinhui.tepinhui_backend.vo.order.OrderPageVO;
import com.tepinhui.tepinhui_backend.vo.order.OrderPayVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final String PRODUCT_DETAIL_CACHE_PREFIX = "product:detail:";
    private static final String PRODUCT_LIST_CACHE_PREFIX = "product:list:";
    private static final String MERCHANT_STATUS_APPROVED = "approved";
    private static final DateTimeFormatter ORDER_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final UserMapper userMapper;
    private final ProductMapper productMapper;
    private final AddressMapper addressMapper;
    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;
    private final MerchantMapper merchantMapper;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDetailVO createOrder(OrderCreateRequest request) {
        // 1. 获取当前登录用户
        User user = getCurrentUser();

        // 2. 校验收货地址存在且属于当前用户
        Address address = addressMapper.selectById(request.getAddressId());
        if (address == null) {
            throw new BusinessException(404, "收货地址不存在");
        }
        if (!address.getUserId().equals(user.getId())) {
            throw new BusinessException(403, "无权使用此收货地址");
        }

        // 3. 遍历商品项，校验并扣减库存
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<Long> productIds = new ArrayList<>();

        for (OrderCreateRequest.OrderItemRequest item : request.getItems()) {
            Product product = productMapper.selectById(item.getProductId());
            if (product == null) {
                throw new BusinessException(404, "商品不存在: " + item.getProductId());
            }
            if (!"on".equals(product.getStatus())) {
                throw new BusinessException(400, "商品已下架: " + product.getName());
            }

            // 乐观锁扣减库存
            int affected = productMapper.deductStock(product.getId(), item.getQuantity());
            if (affected == 0) {
                throw new BusinessException(400, "商品库存不足（可能并发冲突）: " + product.getName());
            }

            BigDecimal unitPrice = product.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(subtotal);
            productIds.add(product.getId());

            // 组装商品快照
            Map<String, Object> productSnapshot = new HashMap<>();
            productSnapshot.put("name", product.getName());
            productSnapshot.put("price", product.getPrice());
            productSnapshot.put("images", product.getImages());
            String snapshotJson;
            try {
                snapshotJson = objectMapper.writeValueAsString(productSnapshot);
            } catch (JsonProcessingException e) {
                log.warn("商品快照序列化失败: productId={}", product.getId(), e);
                snapshotJson = "{}";
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(product.getId());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setUnitPrice(unitPrice);
            orderItem.setSubtotal(subtotal);
            orderItem.setSnapshot(snapshotJson);
            orderItems.add(orderItem);
        }

        // 4. 生成订单号：ORD + 时间戳 + 4位随机数
        String orderNo = generateOrderNo();

        // 5. 组装地址快照
        Map<String, Object> addressInfo = new HashMap<>();
        addressInfo.put("consignee", address.getConsignee());
        addressInfo.put("phone", address.getPhone());
        addressInfo.put("province", address.getProvince());
        addressInfo.put("city", address.getCity());
        addressInfo.put("county", address.getCounty());
        addressInfo.put("detail", address.getDetail());
        String addressJson;
        try {
            addressJson = objectMapper.writeValueAsString(addressInfo);
        } catch (JsonProcessingException e) {
            log.warn("地址快照序列化失败: addressId={}", address.getId(), e);
            addressJson = "{}";
        }

        // 6. 插入 orders 记录
        Orders order = new Orders();
        order.setUserId(user.getId());
        order.setOrderNo(orderNo);
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.PENDING);
        order.setAddress(addressJson);
        order.setRemark(request.getRemark());
        ordersMapper.insert(order);
        if (order.getId() == null) {
            throw new BusinessException(500, "创建订单失败");
        }

        // 7. 插入 order_item 记录
        for (OrderItem orderItem : orderItems) {
            orderItem.setOrderId(order.getId());
            orderItemMapper.insert(orderItem);
        }

        // 8. 清除相关商品缓存
        for (Long productId : productIds) {
            evictCacheKey(PRODUCT_DETAIL_CACHE_PREFIX + productId);
        }
        evictProductListCaches();

        // 9. 组装返回 OrderDetailVO
        List<OrderItemVO> itemVOs = new ArrayList<>();
        for (OrderItem orderItem : orderItems) {
            OrderItemVO itemVO = new OrderItemVO();
            itemVO.setProductId(orderItem.getProductId());
            itemVO.setProductName(extractProductName(orderItem.getSnapshot()));
            itemVO.setQuantity(orderItem.getQuantity());
            itemVO.setUnitPrice(orderItem.getUnitPrice());
            itemVO.setSubtotal(orderItem.getSubtotal());
            itemVO.setSnapshot(orderItem.getSnapshot());
            itemVOs.add(itemVO);
        }

        OrderDetailVO detailVO = new OrderDetailVO();
        detailVO.setId(order.getId());
        detailVO.setOrderNo(orderNo);
        detailVO.setStatus(order.getStatus().getValue());
        detailVO.setTotalAmount(totalAmount);
        detailVO.setAddress(addressJson);
        detailVO.setRemark(request.getRemark());
        detailVO.setCreatedAt(order.getCreatedAt());
        detailVO.setUpdatedAt(order.getUpdatedAt());
        detailVO.setItems(itemVOs);
        return detailVO;
    }

    @Override
    public OrderPageVO getCurrentUserOrders(OrderQueryRequest request) {
        // 1. 获取当前登录用户
        User user = getCurrentUser();

        // 2. 构建查询条件
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<Orders>()
            .eq(Orders::getUserId, user.getId());
        if (StringUtils.hasText(request.getStatus())) {
            wrapper.eq(Orders::getStatus, OrderStatus.fromValue(request.getStatus()));
        }
        wrapper.orderByDesc(Orders::getCreatedAt);

        // 3. 分页查询
        IPage<Orders> page = ordersMapper.selectPage(
            new Page<>(request.getPage(), request.getSize()),
            wrapper
        );

        // 4. 批量查询订单明细，避免 N+1
        List<Orders> ordersList = page.getRecords();
        Map<Long, List<OrderItem>> orderItemsMap = new HashMap<>();
        if (!ordersList.isEmpty()) {
            List<Long> orderIds = ordersList.stream()
                .map(Orders::getId)
                .collect(Collectors.toList());
            List<OrderItem> allItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                    .in(OrderItem::getOrderId, orderIds)
            );
            orderItemsMap = allItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));
        }

        // 5. 组装 OrderDetailVO 列表
        List<OrderDetailVO> detailVOs = new ArrayList<>();
        for (Orders order : ordersList) {
            List<OrderItem> items = orderItemsMap.getOrDefault(order.getId(), List.of());
            List<OrderItemVO> itemVOs = items.stream().map(item -> {
                OrderItemVO vo = new OrderItemVO();
                vo.setProductId(item.getProductId());
                vo.setProductName(extractProductName(item.getSnapshot()));
                vo.setQuantity(item.getQuantity());
                vo.setUnitPrice(item.getUnitPrice());
                vo.setSubtotal(item.getSubtotal());
                vo.setSnapshot(item.getSnapshot());
                return vo;
            }).collect(Collectors.toList());

            OrderDetailVO detailVO = new OrderDetailVO();
            detailVO.setId(order.getId());
            detailVO.setOrderNo(order.getOrderNo());
            detailVO.setStatus(order.getStatus().getValue());
            detailVO.setTotalAmount(order.getTotalAmount());
            detailVO.setAddress(order.getAddress());
            detailVO.setLogisticsNo(order.getLogisticsNo());
            detailVO.setRemark(order.getRemark());
            detailVO.setCreatedAt(order.getCreatedAt());
            detailVO.setUpdatedAt(order.getUpdatedAt());
            detailVO.setItems(itemVOs);
            detailVOs.add(detailVO);
        }

        // 6. 组装分页响应
        OrderPageVO pageVO = new OrderPageVO();
        pageVO.setRecords(detailVOs);
        pageVO.setTotal(page.getTotal());
        pageVO.setPage(request.getPage());
        pageVO.setSize(request.getSize());
        return pageVO;
    }

    @Override
    public OrderDetailVO getOrderDetail(Long id) {
        // 1. 获取当前登录用户
        User user = getCurrentUser();

        // 2. 查询订单
        Orders order = ordersMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }

        // 3. 校验订单归属
        if (!order.getUserId().equals(user.getId())) {
            throw new BusinessException(403, "无权访问此订单");
        }

        // 4. 查询订单明细
        List<OrderItem> items = orderItemMapper.selectList(
            new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId())
        );

        // 5. 组装 OrderItemVO 列表
        List<OrderItemVO> itemVOs = items.stream().map(item -> {
            OrderItemVO vo = new OrderItemVO();
            vo.setProductId(item.getProductId());
            vo.setProductName(extractProductName(item.getSnapshot()));
            vo.setQuantity(item.getQuantity());
            vo.setUnitPrice(item.getUnitPrice());
            vo.setSubtotal(item.getSubtotal());
            vo.setSnapshot(item.getSnapshot());
            return vo;
        }).collect(Collectors.toList());

        // 6. 组装 OrderDetailVO
        OrderDetailVO detailVO = new OrderDetailVO();
        detailVO.setId(order.getId());
        detailVO.setOrderNo(order.getOrderNo());
        detailVO.setStatus(order.getStatus().getValue());
        detailVO.setTotalAmount(order.getTotalAmount());
        detailVO.setAddress(order.getAddress());
        detailVO.setLogisticsNo(order.getLogisticsNo());
        detailVO.setRemark(order.getRemark());
        detailVO.setCreatedAt(order.getCreatedAt());
        detailVO.setUpdatedAt(order.getUpdatedAt());
        detailVO.setItems(itemVOs);
        return detailVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long id) {
        // 1. 获取当前登录用户
        User user = getCurrentUser();

        // 2. 查询订单
        Orders order = ordersMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }

        // 3. 校验订单归属
        if (!order.getUserId().equals(user.getId())) {
            throw new BusinessException(403, "无权操作此订单");
        }

        // 4. 校验状态转移
        if (!order.getStatus().canTransitionTo(OrderStatus.CANCELLED)) {
            throw new BusinessException(400, "当前状态不可取消");
        }

        // 5. 库存回滚：遍历 order_item，将库存加回
        List<OrderItem> items = orderItemMapper.selectList(
            new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId())
        );
        for (OrderItem item : items) {
            productMapper.rollbackStock(item.getProductId(), item.getQuantity());
        }

        // 6. 更新订单状态为 CANCELLED
        order.setStatus(OrderStatus.CANCELLED);
        ordersMapper.updateById(order);

        // 7. 清除相关商品缓存
        for (OrderItem item : items) {
            evictCacheKey(PRODUCT_DETAIL_CACHE_PREFIX + item.getProductId());
        }
        evictProductListCaches();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmOrder(Long id) {
        // 1. 获取当前登录用户
        User user = getCurrentUser();

        // 2. 查询订单
        Orders order = ordersMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }

        // 3. 校验订单归属
        if (!order.getUserId().equals(user.getId())) {
            throw new BusinessException(403, "无权操作此订单");
        }

        // 4. 校验状态转移
        if (!order.getStatus().canTransitionTo(OrderStatus.DONE)) {
            throw new BusinessException(400, "当前状态不可确认收货");
        }

        // 5. 更新订单状态为 DONE
        order.setStatus(OrderStatus.DONE);
        ordersMapper.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shipOrder(Long id, OrderShipRequest request) {
        // 1. 获取当前登录用户
        User user = getCurrentUser();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 2. 查询订单
        Orders order = ordersMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }

        // 3. 校验商家权限：当前用户必须是该订单关联商品的商家，或 ADMIN 角色
        boolean isAdmin = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch("ROLE_ADMIN"::equals);

        if (!isAdmin) {
            // 查询订单关联的所有商品，获取 merchantId 集合
            List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                    .eq(OrderItem::getOrderId, order.getId())
            );
            if (items.isEmpty()) {
                throw new BusinessException(400, "订单无商品项");
            }

            List<Long> productIds = items.stream()
                .map(OrderItem::getProductId)
                .distinct()
                .collect(Collectors.toList());
            List<Product> products = productMapper.selectBatchIds(productIds);
            Set<Long> merchantIds = products.stream()
                .map(Product::getMerchantId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

            // 查询当前用户关联的已审核商家
            Merchant merchant = merchantMapper.selectOne(
                new LambdaQueryWrapper<Merchant>()
                    .eq(Merchant::getUserId, user.getId())
                    .eq(Merchant::getStatus, MERCHANT_STATUS_APPROVED)
                    .last("LIMIT 1")
            );
            if (merchant == null || !merchantIds.contains(merchant.getId())) {
                throw new BusinessException(403, "无权操作此订单");
            }
        }

        // 4. 校验状态转移
        if (!order.getStatus().canTransitionTo(OrderStatus.SHIPPED)) {
            throw new BusinessException(400, "当前状态不可发货");
        }

        // 5. 更新订单 logistics_no 和状态为 SHIPPED
        order.setLogisticsNo(request.getLogisticsNo());
        order.setStatus(OrderStatus.SHIPPED);
        ordersMapper.updateById(order);
    }

    @Override
    public OrderCalculateVO calculateOrder(OrderCalculateRequest request) {
        // 1. 获取当前登录用户
        User user = getCurrentUser();

        // 2. 校验收货地址存在且属于当前用户
        Address address = addressMapper.selectById(request.getAddressId());
        if (address == null) {
            throw new BusinessException(404, "收货地址不存在");
        }
        if (!address.getUserId().equals(user.getId())) {
            throw new BusinessException(403, "无权使用此收货地址");
        }

        // 3. 遍历商品项，校验并计算
        List<OrderCalculateItemVO> itemVOs = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderCalculateRequest.OrderItemRequest item : request.getItems()) {
            Product product = productMapper.selectById(item.getProductId());
            if (product == null) {
                throw new BusinessException(404, "商品不存在: " + item.getProductId());
            }
            if (!"on".equals(product.getStatus())) {
                throw new BusinessException(400, "商品已下架: " + product.getName());
            }
            if (item.getQuantity() > product.getStock()) {
                throw new BusinessException(400, "商品库存不足: " + product.getName());
            }

            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            OrderCalculateItemVO itemVO = OrderCalculateItemVO.builder()
                .productId(product.getId())
                .productName(product.getName())
                .price(product.getPrice())
                .quantity(item.getQuantity())
                .subtotal(subtotal)
                .build();
            itemVOs.add(itemVO);
        }

        // 4. 组装返回（运费和优惠第一版固定为0）
        return OrderCalculateVO.builder()
            .items(itemVOs)
            .subtotal(totalAmount)
            .shippingFee(BigDecimal.ZERO)
            .discount(BigDecimal.ZERO)
            .totalAmount(totalAmount)
            .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderPayVO payOrder(Long orderId, OrderPayRequest request) {
        // 1. 获取当前登录用户
        User user = getCurrentUser();

        // 2. 查询订单
        Orders order = ordersMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }

        // 3. 校验订单归属
        if (!order.getUserId().equals(user.getId())) {
            throw new BusinessException(403, "无权操作此订单");
        }

        // 4. 校验状态转移：仅 PENDING 可支付
        if (!order.getStatus().canTransitionTo(OrderStatus.PAID)) {
            throw new BusinessException(400, "当前状态不可支付");
        }

        // 5. 模拟支付：直接更新订单状态为 PAID
        order.setStatus(OrderStatus.PAID);
        ordersMapper.updateById(order);

        // 6. 组装返回
        return OrderPayVO.builder()
            .orderId(order.getId())
            .orderNo(order.getOrderNo())
            .totalAmount(order.getTotalAmount())
            .status("success")
            .build();
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

    /**
     * 生成订单号：ORD + yyyyMMddHHmmss + 4位随机数
     */
    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(ORDER_NO_FORMATTER);
        int random = new Random().nextInt(10000); // 0 ~ 9999
        return "ORD" + timestamp + String.format("%04d", random);
    }

    /**
     * 从商品快照JSON中提取商品名称（用于 OrderItemVO.productName）
     */
    private String extractProductName(String snapshotJson) {
        if (snapshotJson == null) {
            return null;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(snapshotJson, Map.class);
            Object name = map.get("name");
            return name != null ? name.toString() : null;
        } catch (JsonProcessingException e) {
            log.warn("解析商品快照失败: {}", snapshotJson, e);
            return null;
        }
    }

    private void evictCacheKey(String cacheKey) {
        try {
            redisTemplate.delete(cacheKey);
        } catch (RuntimeException ex) {
            log.warn("Failed to evict cache key {}", cacheKey, ex);
        }
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
}
