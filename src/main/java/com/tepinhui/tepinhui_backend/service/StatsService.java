package com.tepinhui.tepinhui_backend.service;

import com.tepinhui.tepinhui_backend.vo.stats.RegionSalesStatsVO;
import com.tepinhui.tepinhui_backend.vo.stats.RegionTraceStatsVO;
import com.tepinhui.tepinhui_backend.vo.stats.TraceStatsVO;

import java.util.List;

public interface StatsService {

    TraceStatsVO getTraceStats();

    List<RegionTraceStatsVO> listTraceStatsByRegion(String region);

    List<RegionSalesStatsVO> listSalesStatsByRegion(String region);
}
