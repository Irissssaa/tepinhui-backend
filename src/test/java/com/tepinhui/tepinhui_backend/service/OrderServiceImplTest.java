package com.tepinhui.tepinhui_backend.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tepinhui.tepinhui_backend.common.OrderStatus;
import com.tepinhui.tepinhui_backend.common.UserStatus;
import com.tepinhui.tepinhui_backend.dto.order.OrderCalculateRequest;
import com.tepinhui.tepinhui_backend.dto.order.OrderCreateRequest;
import com.tepinhui.tepinhui_backend.dto.order.OrderQueryRequest;
import com.tepinhui.tepinhui_backend.dto.order.OrderShipRequest;
import com.tepinhui.tepinhui_backend.entity.Address;
import com.tepinhui.tepinhui_backend.entity.Merchant;
import com.tepinhui.tepinhui_backend.entity.OrderItem;
import com.tepinhui.tepinhui_backend.entity.Orders;
import com.tepinhui.tepinhui_backend.entity.Product;
import com.tepinhui.tepinhui_backend.entity.User;
import com.tepinhui.tepinhui_backend.exception.BusinessException;
import com.tepinhui.tepinhui_backend.mapper.AddressMapper;
import com.tepinhui.tepinhui_backend.mapper.MerchantMapper;
import com.tepinhui.tepinhui_backend.mapper.OrderItemMapper;
import com.tepinhui.tepinhui_backend.mapper.OrdersMapper;
import com.tepinhui.tepinhui_backend.mapper.ProductMapper;
import com.tepinhui.tepinhui_backend.mapper.UserMapper;
import com.tepinhui.tepinhui_backend.service.impl.OrderServiceImpl;
import com.tepinhui.tepinhui_backend.vo.order.OrderCalculateItemVO;
import com.tepinhui.tepinhui_backend.vo.order.OrderCalculateVO;
import com.tepinhui.tepinhui_backend.vo.order.OrderDetailVO;
import com.tepinhui.tepinhui_backend.vo.order.OrderPageVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private AddressMapper addressMapper;

    @Mock
    private OrdersMapper ordersMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    @Mock
    private MerchantMapper merchantMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private SetOperations<String, Object> setOperations;

    private ObjectMapper objectMapper;

    private OrderServiceImpl orderService;

    private User testUser;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        orderService = new OrderServiceImpl(
            userMapper, productMapper, addressMapper,
            ordersMapper, orderItemMapper, merchantMapper,
            objectMapper, redisTemplate
        );

        // Initialize MyBatis-Plus TableInfo for entities used in LambdaQueryWrapper
        TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(new MybatisConfiguration(), "order-service-test"), Orders.class);
        TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(new MybatisConfiguration(), "order-service-test-item"), OrderItem.class);
        TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(new MybatisConfiguration(), "order-service-test-user"), User.class);
        TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(new MybatisConfiguration(), "order-service-test-address"), Address.class);
        TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(new MybatisConfiguration(), "order-service-test-product"), Product.class);
        TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(new MybatisConfiguration(), "order-service-test-merchant"), Merchant.class);

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

    // ============ calculateOrder ============

    @Test
    void calculateOrder_addressNotFound_throws404() {
        when(addressMapper.selectById(999L)).thenReturn(null);

        OrderCalculateRequest request = buildCalculateRequest(999L, List.of(itemReq(1L, 1)));
        BusinessException ex = assertThrows(BusinessException.class,
            () -> orderService.calculateOrder(request));

        assertEquals(404, ex.getCode());
        assertEquals("收货地址不存在", ex.getMessage());
    }

    @Test
    void calculateOrder_addressNotOwned_throws403() {
        Address address = buildAddress(1L, 999L); // userId=999 != 100
        when(addressMapper.selectById(1L)).thenReturn(address);

        OrderCalculateRequest request = buildCalculateRequest(1L, List.of(itemReq(1L, 1)));
        BusinessException ex = assertThrows(BusinessException.class,
            () -> orderService.calculateOrder(request));

        assertEquals(403, ex.getCode());
        assertEquals("无权使用此收货地址", ex.getMessage());
    }

    @Test
    void calculateOrder_productNotFound_throws404() {
        Address address = buildAddress(1L, 100L);
        when(addressMapper.selectById(1L)).thenReturn(address);
        when(productMapper.selectById(50L)).thenReturn(null);

        OrderCalculateRequest request = buildCalculateRequest(1L, List.of(itemReq(50L, 1)));
        BusinessException ex = assertThrows(BusinessException.class,
            () -> orderService.calculateOrder(request));

        assertEquals(404, ex.getCode());
        assertTrue(ex.getMessage().contains("商品不存在"));
    }

    @Test
    void calculateOrder_productOffline_throws400() {
        Address address = buildAddress(1L, 100L);
        when(addressMapper.selectById(1L)).thenReturn(address);
        Product offlineProduct = buildProduct(50L, "off", new BigDecimal("10.00"), 10, "下架商品");
        when(productMapper.selectById(50L)).thenReturn(offlineProduct);

        OrderCalculateRequest request = buildCalculateRequest(1L, List.of(itemReq(50L, 1)));
        BusinessException ex = assertThrows(BusinessException.class,
            () -> orderService.calculateOrder(request));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("商品已下架"));
    }

    @Test
    void calculateOrder_stockInsufficient_throws400() {
        Address address = buildAddress(1L, 100L);
        when(addressMapper.selectById(1L)).thenReturn(address);
        Product lowStockProduct = buildProduct(50L, "on", new BigDecimal("10.00"), 2, "测试商品");
        when(productMapper.selectById(50L)).thenReturn(lowStockProduct);

        OrderCalculateRequest request = buildCalculateRequest(1L, List.of(itemReq(50L, 5)));
        BusinessException ex = assertThrows(BusinessException.class,
            () -> orderService.calculateOrder(request));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("商品库存不足"));
    }

    @Test
    void calculateOrder_happyPath_returnsCorrectAmounts() {
        Address address = buildAddress(1L, 100L);
        when(addressMapper.selectById(1L)).thenReturn(address);

        Product productA = buildProduct(10L, "on", new BigDecimal("50.00"), 100, "商品A");
        Product productB = buildProduct(11L, "on", new BigDecimal("30.00"), 100, "商品B");
        when(productMapper.selectById(10L)).thenReturn(productA);
        when(productMapper.selectById(11L)).thenReturn(productB);

        OrderCalculateRequest request = buildCalculateRequest(1L, List.of(itemReq(10L, 2), itemReq(11L, 3)));
        OrderCalculateVO result = orderService.calculateOrder(request);

        assertNotNull(result);
        assertEquals(2, result.getItems().size());
        assertEquals(0, new BigDecimal("190.00").compareTo(result.getSubtotal()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getShippingFee()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getDiscount()));
        assertEquals(0, new BigDecimal("190.00").compareTo(result.getTotalAmount()));

        OrderCalculateItemVO item0 = result.getItems().get(0);
        assertEquals(10L, item0.getProductId());
        assertEquals(0, new BigDecimal("100.00").compareTo(item0.getSubtotal()));

        OrderCalculateItemVO item1 = result.getItems().get(1);
        assertEquals(11L, item1.getProductId());
        assertEquals(0, new BigDecimal("90.00").compareTo(item1.getSubtotal()));
    }

    // ============ createOrder ============

    @Test
    void createOrder_addressNotFound_throws404() {
        when(addressMapper.selectById(999L)).thenReturn(null);

        OrderCreateRequest request = buildCreateRequest(999L, List.of(createItemReq(1L, 1)));
        BusinessException ex = assertThrows(BusinessException.class,
            () -> orderService.createOrder(request));

        assertEquals(404, ex.getCode());
        assertEquals("收货地址不存在", ex.getMessage());
        verify(productMapper, never()).selectById(anyLong());
    }

    @Test
    void createOrder_productOffline_throws400() {
        Address address = buildAddress(1L, 100L);
        when(addressMapper.selectById(1L)).thenReturn(address);

        Product offlineProduct = buildProduct(50L, "off", new BigDecimal("10.00"), 10, "下架商品");
        when(productMapper.selectById(50L)).thenReturn(offlineProduct);

        OrderCreateRequest request = buildCreateRequest(1L, List.of(createItemReq(50L, 1)));
        BusinessException ex = assertThrows(BusinessException.class,
            () -> orderService.createOrder(request));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("商品已下架"));
        verify(productMapper, never()).deductStock(anyLong(), any());
    }

    @Test
    void createOrder_stockInsufficient_throws400() {
        Address address = buildAddress(1L, 100L);
        when(addressMapper.selectById(1L)).thenReturn(address);

        Product product = buildProduct(50L, "on", new BigDecimal("10.00"), 10, "测试商品");
        when(productMapper.selectById(50L)).thenReturn(product);
        when(productMapper.deductStock(50L, 5)).thenReturn(0); // stock insufficient

        OrderCreateRequest request = buildCreateRequest(1L, List.of(createItemReq(50L, 5)));
        BusinessException ex = assertThrows(BusinessException.class,
            () -> orderService.createOrder(request));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("商品库存不足"));
        verify(ordersMapper, never()).insert(any(Orders.class));
    }

    @Test
    void createOrder_happyPath_createsOrderSuccessfully() {
        Address address = buildAddress(1L, 100L);
        when(addressMapper.selectById(1L)).thenReturn(address);

        Product product = buildProduct(50L, "on", new BigDecimal("100.00"), 50, "龙井茶");
        when(productMapper.selectById(50L)).thenReturn(product);
        when(productMapper.deductStock(50L, 2)).thenReturn(1);

        // Simulate MyBatis-Plus setting auto-generated id on insert
        when(ordersMapper.insert(any(Orders.class))).thenAnswer(invocation -> {
            Orders order = invocation.getArgument(0);
            order.setId(1001L);
            return 1;
        });
        when(orderItemMapper.insert(any(OrderItem.class))).thenReturn(1);

        OrderCreateRequest request = buildCreateRequest(1L, List.of(createItemReq(50L, 2)));
        OrderDetailVO result = orderService.createOrder(request);

        assertNotNull(result);
        assertNotNull(result.getOrderNo());
        assertTrue(result.getOrderNo().startsWith("ORD"));
        assertEquals("pending", result.getStatus());
        assertEquals(0, new BigDecimal("200.00").compareTo(result.getTotalAmount()));
        assertEquals(1001L, result.getId());
        assertNotNull(result.getItems());
        assertEquals(1, result.getItems().size());
        assertEquals(50L, result.getItems().get(0).getProductId());
        assertEquals(2, result.getItems().get(0).getQuantity());
        assertEquals(0, new BigDecimal("200.00").compareTo(result.getItems().get(0).getSubtotal()));

        // Verify stock deduction
        verify(productMapper).deductStock(50L, 2);

        // Verify order inserted
        ArgumentCaptor<Orders> orderCaptor = ArgumentCaptor.forClass(Orders.class);
        verify(ordersMapper).insert(orderCaptor.capture());
        assertEquals(100L, orderCaptor.getValue().getUserId());
        assertEquals(OrderStatus.PENDING, orderCaptor.getValue().getStatus());

        // Verify order items inserted
        ArgumentCaptor<OrderItem> itemCaptor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemMapper).insert(itemCaptor.capture());
        assertEquals(1001L, itemCaptor.getValue().getOrderId());
        assertEquals(50L, itemCaptor.getValue().getProductId());
        assertEquals(2, itemCaptor.getValue().getQuantity());
    }

    // ============ getCurrentUserOrders ============

    @Test
    @SuppressWarnings("unchecked")
    void getCurrentUserOrders_emptyList_returnsEmptyPage() {
        when(ordersMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(new Page<>(1, 10, 0));

        OrderQueryRequest request = new OrderQueryRequest();
        request.setPage(1);
        request.setSize(10);

        OrderPageVO result = orderService.getCurrentUserOrders(request);

        assertNotNull(result);
        assertNotNull(result.getRecords());
        assertTrue(result.getRecords().isEmpty());
        assertEquals(0L, result.getTotal());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getCurrentUserOrders_withStatusFilter_returnsFilteredResults() {
        Orders order = buildOrder(1L, 100L, "ORD202405151200001234", new BigDecimal("100.00"), OrderStatus.PENDING);
        Page<Orders> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(order));

        when(ordersMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        OrderItem orderItem = buildOrderItem(1L, 1L, 50L, 2, new BigDecimal("50.00"), new BigDecimal("100.00"), "{\"name\":\"龙井茶\"}");
        when(orderItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(orderItem));

        OrderQueryRequest request = new OrderQueryRequest();
        request.setPage(1);
        request.setSize(10);
        request.setStatus("pending");

        OrderPageVO result = orderService.getCurrentUserOrders(request);

        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals(1L, result.getTotal());

        OrderDetailVO detail = result.getRecords().get(0);
        assertEquals(1L, detail.getId());
        assertEquals("ORD202405151200001234", detail.getOrderNo());
        assertEquals("pending", detail.getStatus());
        assertEquals(0, new BigDecimal("100.00").compareTo(detail.getTotalAmount()));
    }

    // ============ getOrderDetail ============

    @Test
    void getOrderDetail_notFound_throws404() {
        when(ordersMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> orderService.getOrderDetail(999L));

        assertEquals(404, ex.getCode());
        assertEquals("订单不存在", ex.getMessage());
    }

    @Test
    void getOrderDetail_notOwned_throws403() {
        Orders order = buildOrder(1L, 999L, "ORD202405151200001234", new BigDecimal("100.00"), OrderStatus.PENDING);
        when(ordersMapper.selectById(1L)).thenReturn(order);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> orderService.getOrderDetail(1L));

        assertEquals(403, ex.getCode());
        assertEquals("无权访问此订单", ex.getMessage());
    }

    @Test
    void getOrderDetail_happyPath_returnsDetail() {
        Orders order = buildOrder(1L, 100L, "ORD202405151200001234", new BigDecimal("100.00"), OrderStatus.PAID);
        when(ordersMapper.selectById(1L)).thenReturn(order);

        OrderItem item = buildOrderItem(1L, 1L, 50L, 2, new BigDecimal("50.00"), new BigDecimal("100.00"), "{\"name\":\"龙井茶\"}");
        when(orderItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(item));

        OrderDetailVO result = orderService.getOrderDetail(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("ORD202405151200001234", result.getOrderNo());
        assertEquals("paid", result.getStatus());
        assertEquals(0, new BigDecimal("100.00").compareTo(result.getTotalAmount()));
        assertEquals(1, result.getItems().size());
        assertEquals(50L, result.getItems().get(0).getProductId());
        assertEquals("龙井茶", result.getItems().get(0).getProductName());
    }

    // ============ cancelOrder ============

    @Test
    void cancelOrder_notFound_throws404() {
        when(ordersMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> orderService.cancelOrder(999L));

        assertEquals(404, ex.getCode());
        assertEquals("订单不存在", ex.getMessage());
    }

    @Test
    void cancelOrder_statusNotCancellable_throws400() {
        Orders shippedOrder = buildOrder(1L, 100L, "ORD202405151200001234", new BigDecimal("100.00"), OrderStatus.SHIPPED);
        when(ordersMapper.selectById(1L)).thenReturn(shippedOrder);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> orderService.cancelOrder(1L));

        assertEquals(400, ex.getCode());
        assertEquals("当前状态不可取消", ex.getMessage());
    }

    @Test
    void cancelOrder_happyPath_rollsBackStock() {
        Orders pendingOrder = buildOrder(1L, 100L, "ORD202405151200001234", new BigDecimal("100.00"), OrderStatus.PENDING);
        when(ordersMapper.selectById(1L)).thenReturn(pendingOrder);

        OrderItem item = buildOrderItem(1L, 1L, 50L, 3, new BigDecimal("50.00"), new BigDecimal("150.00"), null);
        when(orderItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(item));
        when(productMapper.rollbackStock(50L, 3)).thenReturn(1);
        when(ordersMapper.updateById(any(Orders.class))).thenReturn(1);

        orderService.cancelOrder(1L);

        // Verify stock rollback
        verify(productMapper).rollbackStock(50L, 3);

        // Verify order status updated to CANCELLED
        ArgumentCaptor<Orders> captor = ArgumentCaptor.forClass(Orders.class);
        verify(ordersMapper).updateById(captor.capture());
        assertEquals(OrderStatus.CANCELLED, captor.getValue().getStatus());
    }

    // ============ confirmOrder ============

    @Test
    void confirmOrder_notFound_throws404() {
        when(ordersMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> orderService.confirmOrder(999L));

        assertEquals(404, ex.getCode());
        assertEquals("订单不存在", ex.getMessage());
    }

    @Test
    void confirmOrder_statusNotConfirmable_throws400() {
        Orders pendingOrder = buildOrder(1L, 100L, "ORD202405151200001234", new BigDecimal("100.00"), OrderStatus.PENDING);
        when(ordersMapper.selectById(1L)).thenReturn(pendingOrder);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> orderService.confirmOrder(1L));

        assertEquals(400, ex.getCode());
        assertEquals("当前状态不可确认收货", ex.getMessage());
    }

    @Test
    void confirmOrder_happyPath_updatesStatus() {
        Orders shippedOrder = buildOrder(1L, 100L, "ORD202405151200001234", new BigDecimal("100.00"), OrderStatus.SHIPPED);
        when(ordersMapper.selectById(1L)).thenReturn(shippedOrder);
        when(ordersMapper.updateById(any(Orders.class))).thenReturn(1);

        orderService.confirmOrder(1L);

        ArgumentCaptor<Orders> captor = ArgumentCaptor.forClass(Orders.class);
        verify(ordersMapper).updateById(captor.capture());
        assertEquals(OrderStatus.DONE, captor.getValue().getStatus());
    }

    // ============ shipOrder ============

    @Test
    void shipOrder_notFound_throws404() {
        when(ordersMapper.selectById(999L)).thenReturn(null);

        OrderShipRequest request = new OrderShipRequest();
        request.setLogisticsNo("SF1234567890");

        BusinessException ex = assertThrows(BusinessException.class,
            () -> orderService.shipOrder(999L, request));

        assertEquals(404, ex.getCode());
        assertEquals("订单不存在", ex.getMessage());
    }

    @Test
    void shipOrder_merchantNotAuthorized_throws403() {
        // Set up merchant user (not admin)
        authenticateWithAuthority("testuser", "ROLE_MERCHANT");

        Orders paidOrder = buildOrder(1L, 200L, "ORD202405151200001234", new BigDecimal("100.00"), OrderStatus.PAID);
        when(ordersMapper.selectById(1L)).thenReturn(paidOrder);

        OrderItem item = buildOrderItem(1L, 1L, 50L, 1, new BigDecimal("50.00"), new BigDecimal("50.00"), null);
        when(orderItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(item));

        Product product = buildProduct(50L, "on", new BigDecimal("50.00"), 10, "商品");
        product.setMerchantId(10L);
        when(productMapper.selectBatchIds(any())).thenReturn(List.of(product));

        // Current user's merchant does not match product's merchant
        Merchant merchant = new Merchant();
        merchant.setId(99L); // merchantId=99, not matching product.merchantId=10
        merchant.setUserId(100L);
        merchant.setStatus("approved");
        when(merchantMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(merchant);

        OrderShipRequest request = new OrderShipRequest();
        request.setLogisticsNo("SF1234567890");

        BusinessException ex = assertThrows(BusinessException.class,
            () -> orderService.shipOrder(1L, request));

        assertEquals(403, ex.getCode());
        assertEquals("无权操作此订单", ex.getMessage());
    }

    @Test
    void shipOrder_statusNotShippable_throws400() {
        // Admin can bypass merchant check
        authenticateWithAuthority("testuser", "ROLE_ADMIN");

        Orders pendingOrder = buildOrder(1L, 100L, "ORD202405151200001234", new BigDecimal("100.00"), OrderStatus.PENDING);
        when(ordersMapper.selectById(1L)).thenReturn(pendingOrder);

        OrderShipRequest request = new OrderShipRequest();
        request.setLogisticsNo("SF1234567890");

        BusinessException ex = assertThrows(BusinessException.class,
            () -> orderService.shipOrder(1L, request));

        assertEquals(400, ex.getCode());
        assertEquals("当前状态不可发货", ex.getMessage());
    }

    @Test
    void shipOrder_happyPath_admin_updatesLogisticsAndStatus() {
        authenticateWithAuthority("testuser", "ROLE_ADMIN");

        Orders paidOrder = buildOrder(1L, 100L, "ORD202405151200001234", new BigDecimal("100.00"), OrderStatus.PAID);
        when(ordersMapper.selectById(1L)).thenReturn(paidOrder);
        when(ordersMapper.updateById(any(Orders.class))).thenReturn(1);

        OrderShipRequest request = new OrderShipRequest();
        request.setLogisticsNo("SF1234567890");

        orderService.shipOrder(1L, request);

        ArgumentCaptor<Orders> captor = ArgumentCaptor.forClass(Orders.class);
        verify(ordersMapper).updateById(captor.capture());
        assertEquals(OrderStatus.SHIPPED, captor.getValue().getStatus());
        assertEquals("SF1234567890", captor.getValue().getLogisticsNo());
    }

    // ============ helpers ============

    private void authenticate(String username) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
            username, null, List.of()));
        SecurityContextHolder.setContext(context);
    }

    private void authenticateWithAuthority(String username, String authority) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
            username, null, List.of(new SimpleGrantedAuthority(authority))));
        SecurityContextHolder.setContext(context);
    }

    private Address buildAddress(Long id, Long userId) {
        Address address = new Address();
        address.setId(id);
        address.setUserId(userId);
        address.setConsignee("张三");
        address.setPhone("13800000000");
        address.setProvince("浙江");
        address.setCity("杭州");
        address.setCounty("西湖区");
        address.setDetail("文三路123号");
        return address;
    }

    private Product buildProduct(Long id, String status, BigDecimal price, int stock, String name) {
        Product product = new Product();
        product.setId(id);
        product.setStatus(status);
        product.setPrice(price);
        product.setStock(stock);
        product.setName(name);
        return product;
    }

    private Orders buildOrder(Long id, Long userId, String orderNo, BigDecimal totalAmount, OrderStatus status) {
        Orders order = new Orders();
        order.setId(id);
        order.setUserId(userId);
        order.setOrderNo(orderNo);
        order.setTotalAmount(totalAmount);
        order.setStatus(status);
        order.setAddress("{\"consignee\":\"张三\"}");
        return order;
    }

    private OrderItem buildOrderItem(Long id, Long orderId, Long productId, int quantity,
                                     BigDecimal unitPrice, BigDecimal subtotal, String snapshot) {
        OrderItem item = new OrderItem();
        item.setId(id);
        item.setOrderId(orderId);
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        item.setSubtotal(subtotal);
        item.setSnapshot(snapshot);
        return item;
    }

    private OrderCalculateRequest buildCalculateRequest(Long addressId, List<OrderCalculateRequest.OrderItemRequest> items) {
        OrderCalculateRequest request = new OrderCalculateRequest();
        request.setAddressId(addressId);
        request.setItems(items);
        return request;
    }

    private OrderCalculateRequest.OrderItemRequest itemReq(Long productId, int quantity) {
        OrderCalculateRequest.OrderItemRequest req = new OrderCalculateRequest.OrderItemRequest();
        req.setProductId(productId);
        req.setQuantity(quantity);
        return req;
    }

    private OrderCreateRequest buildCreateRequest(Long addressId, List<OrderCreateRequest.OrderItemRequest> items) {
        OrderCreateRequest request = new OrderCreateRequest();
        request.setAddressId(addressId);
        request.setItems(items);
        return request;
    }

    private OrderCreateRequest.OrderItemRequest createItemReq(Long productId, int quantity) {
        OrderCreateRequest.OrderItemRequest req = new OrderCreateRequest.OrderItemRequest();
        req.setProductId(productId);
        req.setQuantity(quantity);
        return req;
    }
}
