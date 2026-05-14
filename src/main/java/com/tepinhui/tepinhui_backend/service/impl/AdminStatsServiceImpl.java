package com.tepinhui.tepinhui_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tepinhui.tepinhui_backend.common.OrderStatus;
import com.tepinhui.tepinhui_backend.entity.Merchant;
import com.tepinhui.tepinhui_backend.entity.Orders;
import com.tepinhui.tepinhui_backend.entity.Product;
import com.tepinhui.tepinhui_backend.entity.TraceRecord;
import com.tepinhui.tepinhui_backend.entity.User;
import com.tepinhui.tepinhui_backend.mapper.MerchantMapper;
import com.tepinhui.tepinhui_backend.mapper.OrdersMapper;
import com.tepinhui.tepinhui_backend.mapper.ProductMapper;
import com.tepinhui.tepinhui_backend.mapper.TraceRecordMapper;
import com.tepinhui.tepinhui_backend.mapper.UserMapper;
import com.tepinhui.tepinhui_backend.service.AdminStatsService;
import com.tepinhui.tepinhui_backend.vo.admin.AdminStatsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminStatsServiceImpl implements AdminStatsService {

    private final UserMapper userMapper;
    private final MerchantMapper merchantMapper;
    private final ProductMapper productMapper;
    private final OrdersMapper ordersMapper;
    private final TraceRecordMapper traceRecordMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    /** 缓存 key */
    private static final String ADMIN_STATS_CACHE_KEY = "admin:stats";
    /** 缓存 TTL: 30分钟 */
    private static final long CACHE_TTL_MINUTES = 30;

    @Override
    public AdminStatsVO getAdminStats() {
        // 1. 尝试从缓存读取
        Object cached = redisTemplate.opsForValue().get(ADMIN_STATS_CACHE_KEY);
        if (cached != null) {
            log.debug("平台看板命中缓存");
            return (AdminStatsVO) cached;
        }

        // 2. 缓存未命中，查询各表统计数据
        AdminStatsVO vo = buildStats();

        // 3. 写入缓存
        redisTemplate.opsForValue().set(ADMIN_STATS_CACHE_KEY, vo,
                java.time.Duration.ofMinutes(CACHE_TTL_MINUTES));

        return vo;
    }

    private AdminStatsVO buildStats() {
        AdminStatsVO vo = new AdminStatsVO();

        // 用户总数
        vo.setUserCount(userMapper.selectCount(null));

        // 已通过商家数（approved）
        vo.setMerchantCount(merchantMapper.selectCount(
                new LambdaQueryWrapper<Merchant>().eq(Merchant::getStatus, "approved")));

        // 待审核商家数（pending）
        vo.setPendingMerchantCount(merchantMapper.selectCount(
                new LambdaQueryWrapper<Merchant>().eq(Merchant::getStatus, "pending")));

        // 商品总数
        vo.setProductCount(productMapper.selectCount(null));

        // 订单总数
        vo.setOrderCount(ordersMapper.selectCount(null));

        // 总销售额：已完成 + 已发货订单的金额之和
        Set<OrderStatus> completedStatuses = Set.of(OrderStatus.DONE, OrderStatus.SHIPPED);
        LambdaQueryWrapper<Orders> orderWrapper = new LambdaQueryWrapper<Orders>()
                .in(Orders::getStatus, completedStatuses);
        BigDecimal totalSales = ordersMapper.selectList(orderWrapper).stream()
                .map(Orders::getTotalAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setTotalSalesAmount(totalSales);

        // 溯源记录总数
        vo.setTraceCount(traceRecordMapper.selectCount(null));

        // 待审核溯源数
        vo.setPendingTraceCount(traceRecordMapper.selectCount(
                new LambdaQueryWrapper<TraceRecord>().eq(TraceRecord::getAuditStatus, "pending")));

        return vo;
    }
}
