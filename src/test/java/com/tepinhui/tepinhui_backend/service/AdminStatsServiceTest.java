package com.tepinhui.tepinhui_backend.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.tepinhui.tepinhui_backend.common.OrderStatus;
import com.tepinhui.tepinhui_backend.entity.Merchant;
import com.tepinhui.tepinhui_backend.entity.Orders;
import com.tepinhui.tepinhui_backend.entity.TraceRecord;
import com.tepinhui.tepinhui_backend.entity.User;
import com.tepinhui.tepinhui_backend.mapper.MerchantMapper;
import com.tepinhui.tepinhui_backend.mapper.OrdersMapper;
import com.tepinhui.tepinhui_backend.mapper.ProductMapper;
import com.tepinhui.tepinhui_backend.mapper.TraceRecordMapper;
import com.tepinhui.tepinhui_backend.mapper.UserMapper;
import com.tepinhui.tepinhui_backend.service.impl.AdminStatsServiceImpl;
import com.tepinhui.tepinhui_backend.vo.admin.AdminStatsVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminStatsServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private MerchantMapper merchantMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private OrdersMapper ordersMapper;

    @Mock
    private TraceRecordMapper traceRecordMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private AdminStatsServiceImpl adminStatsService;

    @BeforeEach
    void setUp() {
        adminStatsService = new AdminStatsServiceImpl(
                userMapper, merchantMapper, productMapper, ordersMapper, traceRecordMapper, redisTemplate
        );
        MybatisConfiguration config = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(config, "admin-stats-test");
        TableInfoHelper.initTableInfo(assistant, Merchant.class);
        TableInfoHelper.initTableInfo(assistant, Orders.class);
        TableInfoHelper.initTableInfo(assistant, TraceRecord.class);
    }

    @Test
    void getAdminStatsShouldReturnCachedResultWhenAvailable() {
        AdminStatsVO cached = new AdminStatsVO();
        cached.setUserCount(100L);
        cached.setMerchantCount(20L);
        cached.setPendingMerchantCount(5L);
        cached.setProductCount(300L);
        cached.setOrderCount(500L);
        cached.setTotalSalesAmount(new BigDecimal("88888.00"));
        cached.setTraceCount(1000L);
        cached.setPendingTraceCount(10L);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("admin:stats")).thenReturn(cached);

        AdminStatsVO result = adminStatsService.getAdminStats();

        assertSame(cached, result);
        // 验证没有查询任何数据库表
    }

    @Test
    void getAdminStatsShouldQueryAllTablesAndCacheWhenCacheMiss() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        // 各表返回计数
        when(userMapper.selectCount(null)).thenReturn(100L);
        // approved count (第1次) then pending count (第2次)
        when(merchantMapper.selectCount(any(LambdaQueryWrapper.class)))
                .thenReturn(20L)  // approved
                .thenReturn(5L);  // pending
        when(productMapper.selectCount(null)).thenReturn(300L);
        when(ordersMapper.selectCount(null)).thenReturn(500L);
        when(traceRecordMapper.selectCount(null)).thenReturn(1000L);
        when(traceRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(10L);

        // 订单列表用于计算销售额
        Orders order1 = new Orders();
        order1.setTotalAmount(new BigDecimal("100.00"));
        Orders order2 = new Orders();
        order2.setTotalAmount(new BigDecimal("200.50"));
        Orders order3 = new Orders();
        order3.setTotalAmount(new BigDecimal("50.25"));
        when(ordersMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(order1, order2, order3));

        AdminStatsVO result = adminStatsService.getAdminStats();

        assertEquals(100L, result.getUserCount());
        assertEquals(20L, result.getMerchantCount());
        assertEquals(5L, result.getPendingMerchantCount());
        assertEquals(300L, result.getProductCount());
        assertEquals(500L, result.getOrderCount());
        assertEquals(new BigDecimal("350.75"), result.getTotalSalesAmount());
        assertEquals(1000L, result.getTraceCount());
        assertEquals(10L, result.getPendingTraceCount());

        // 验证缓存写入
        verify(valueOperations).set(eq("admin:stats"), any(AdminStatsVO.class), eq(Duration.ofMinutes(30)));
    }

    @Test
    void getAdminStatsShouldHandleNullTotalAmount() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        when(userMapper.selectCount(null)).thenReturn(10L);
        when(merchantMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(productMapper.selectCount(null)).thenReturn(0L);
        when(ordersMapper.selectCount(null)).thenReturn(0L);
        when(traceRecordMapper.selectCount(null)).thenReturn(0L);
        when(traceRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        // 有一个订单金额为 null
        Orders nullAmountOrder = new Orders();
        nullAmountOrder.setTotalAmount(null);
        Orders normalOrder = new Orders();
        normalOrder.setTotalAmount(new BigDecimal("99.99"));
        when(ordersMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(nullAmountOrder, normalOrder));

        AdminStatsVO result = adminStatsService.getAdminStats();

        assertEquals(new BigDecimal("99.99"), result.getTotalSalesAmount());
    }

    @Test
    void getAdminStatsShouldReturnZeroWhenNoOrders() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        when(userMapper.selectCount(null)).thenReturn(5L);
        when(merchantMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(productMapper.selectCount(null)).thenReturn(0L);
        when(ordersMapper.selectCount(null)).thenReturn(0L);
        when(traceRecordMapper.selectCount(null)).thenReturn(0L);
        when(traceRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(ordersMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        AdminStatsVO result = adminStatsService.getAdminStats();

        assertEquals(BigDecimal.ZERO, result.getTotalSalesAmount());
    }
}
