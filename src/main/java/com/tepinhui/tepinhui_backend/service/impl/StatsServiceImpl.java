package com.tepinhui.tepinhui_backend.service.impl;

import com.tepinhui.tepinhui_backend.service.StatsService;
import com.tepinhui.tepinhui_backend.vo.stats.RegionSalesStatsVO;
import com.tepinhui.tepinhui_backend.vo.stats.RegionTraceStatsVO;
import com.tepinhui.tepinhui_backend.vo.stats.TraceStatsVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class StatsServiceImpl implements StatsService {

    // 预留缓存 key: stats:trace:region:{region}
    private static final String TRACE_REGION_CACHE_KEY = "stats:trace:region:{region}";

    // 预留缓存 key: stats:sales:region:{region}
    private static final String SALES_REGION_CACHE_KEY = "stats:sales:region:{region}";

    @Override
    public TraceStatsVO getTraceStats() {
        TraceStatsVO vo = new TraceStatsVO();
        vo.setTotalTraceCount(0L);
        vo.setPassedTraceCount(0L);
        vo.setPendingTraceCount(0L);
        vo.setRejectedTraceCount(0L);
        return vo;
    }

    @Override
    public List<RegionTraceStatsVO> listTraceStatsByRegion(String region) {
        return List.of();
    }

    @Override
    public List<RegionSalesStatsVO> listSalesStatsByRegion(String region) {
        return List.of();
    }
}
