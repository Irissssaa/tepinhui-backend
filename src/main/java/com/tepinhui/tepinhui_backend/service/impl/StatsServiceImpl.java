package com.tepinhui.tepinhui_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tepinhui.tepinhui_backend.entity.Orders;
import com.tepinhui.tepinhui_backend.entity.TraceRecord;
import com.tepinhui.tepinhui_backend.mapper.OrdersMapper;
import com.tepinhui.tepinhui_backend.mapper.TraceRecordMapper;
import com.tepinhui.tepinhui_backend.service.StatsService;
import com.tepinhui.tepinhui_backend.vo.stats.RegionSalesStatsVO;
import com.tepinhui.tepinhui_backend.vo.stats.RegionTraceStatsVO;
import com.tepinhui.tepinhui_backend.vo.stats.TraceStatsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final TraceRecordMapper traceRecordMapper;
    private final OrdersMapper ordersMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /** 缓存 key */
    private static final String TRACE_STATS_CACHE_KEY = "stats:trace";
    private static final String TRACE_REGION_CACHE_KEY = "stats:trace:region";
    private static final String SALES_REGION_CACHE_KEY = "stats:sales:region";
    /** 缓存 TTL */
    private static final Duration STATS_TTL = Duration.ofHours(1);

    @Override
    public TraceStatsVO getTraceStats() {
        // 1. 查缓存
        Object cached = redisTemplate.opsForValue().get(TRACE_STATS_CACHE_KEY);
        if (cached != null) {
            log.debug("溯源统计命中缓存");
            return (TraceStatsVO) cached;
        }

        LambdaQueryWrapper<TraceRecord> wrapper = new LambdaQueryWrapper<>();

        long total = traceRecordMapper.selectCount(wrapper.clone());
        long pending = traceRecordMapper.selectCount(wrapper.clone().eq(TraceRecord::getAuditStatus, "pending"));
        long passed = traceRecordMapper.selectCount(wrapper.clone().eq(TraceRecord::getAuditStatus, "pass"));
        long rejected = traceRecordMapper.selectCount(wrapper.clone().eq(TraceRecord::getAuditStatus, "reject"));

        TraceStatsVO vo = new TraceStatsVO();
        vo.setTotalTraceCount(total);
        vo.setPendingTraceCount(pending);
        vo.setPassedTraceCount(passed);
        vo.setRejectedTraceCount(rejected);

        redisTemplate.opsForValue().set(TRACE_STATS_CACHE_KEY, vo, STATS_TTL);
        log.debug("溯源统计已缓存 total={} pending={} passed={} rejected={}", total, pending, passed, rejected);

        return vo;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<RegionTraceStatsVO> listTraceStatsByRegion(String region) {
        String cacheKey = TRACE_REGION_CACHE_KEY + (StringUtils.hasText(region) ? ":" + region : "");
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("区域溯源统计命中缓存");
            return (List<RegionTraceStatsVO>) cached;
        }

        LambdaQueryWrapper<TraceRecord> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(region)) {
            wrapper.like(TraceRecord::getOriginAddress, region);
        }

        List<TraceRecord> records = traceRecordMapper.selectList(wrapper);

        // 按省份解析统计
        Map<String, Long> regionCount = new LinkedHashMap<>();
        for (TraceRecord r : records) {
            String province = parseProvince(r.getOriginAddress());
            if (province != null) {
                regionCount.merge(province, 1L, Long::sum);
            }
        }

        List<RegionTraceStatsVO> result = regionCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> {
                    RegionTraceStatsVO vo = new RegionTraceStatsVO();
                    vo.setRegion(e.getKey());
                    vo.setCount(e.getValue());
                    return vo;
                })
                .collect(Collectors.toList());

        redisTemplate.opsForValue().set(cacheKey, result, STATS_TTL);
        log.debug("区域溯源统计已缓存，共 {} 个地区", result.size());

        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<RegionSalesStatsVO> listSalesStatsByRegion(String region) {
        String cacheKey = SALES_REGION_CACHE_KEY + (StringUtils.hasText(region) ? ":" + region : "");
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("区域销售统计命中缓存");
            return (List<RegionSalesStatsVO>) cached;
        }

        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(region)) {
            wrapper.like(Orders::getAddress, region);
        }

        List<Orders> orders = ordersMapper.selectList(wrapper);

        // 按省份解析销售额
        Map<String, BigDecimal> regionSales = new LinkedHashMap<>();
        for (Orders o : orders) {
            String province = parseProvinceFromAddress(o.getAddress());
            if (province != null) {
                regionSales.merge(province, o.getTotalAmount(), BigDecimal::add);
            }
        }

        List<RegionSalesStatsVO> result = regionSales.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(e -> {
                    RegionSalesStatsVO vo = new RegionSalesStatsVO();
                    vo.setRegion(e.getKey());
                    vo.setSalesAmount(e.getValue());
                    return vo;
                })
                .collect(Collectors.toList());

        redisTemplate.opsForValue().set(cacheKey, result, STATS_TTL);
        log.debug("区域销售统计已缓存，共 {} 个地区", result.size());

        return result;
    }

    /**
     * 从地址字符串解析省份
     * 简单逻辑：取"省"或"自治区"之前的字符
     */
    private String parseProvince(String address) {
        if (!StringUtils.hasText(address)) {
            return null;
        }
        // 匹配 "XX省" 或 "XX自治区" 或 "XX市"（直辖市）
        String[] patterns = {"省", "自治区", "市"};
        for (String p : patterns) {
            int idx = address.indexOf(p);
            if (idx > 0) {
                return address.substring(0, idx);
            }
        }
        // 取前4个字符作为省份名
        return address.length() > 4 ? address.substring(0, 4) : address;
    }

    /**
     * 从订单地址 JSON 中解析省份
     * address 字段格式：{"province":"浙江省","city":"杭州市",...}
     */
    private String parseProvinceFromAddress(String addressJson) {
        if (!StringUtils.hasText(addressJson)) {
            return null;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> addr = objectMapper.readValue(addressJson, Map.class);
            Object province = addr.get("province");
            return (province != null) ? province.toString() : null;
        } catch (Exception e) {
            log.warn("解析订单地址失败: {}", addressJson, e);
            return null;
        }
    }
}
