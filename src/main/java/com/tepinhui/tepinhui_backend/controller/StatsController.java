package com.tepinhui.tepinhui_backend.controller;

import com.tepinhui.tepinhui_backend.common.Result;
import com.tepinhui.tepinhui_backend.service.StatsService;
import com.tepinhui.tepinhui_backend.vo.stats.RegionSalesStatsVO;
import com.tepinhui.tepinhui_backend.vo.stats.RegionTraceStatsVO;
import com.tepinhui.tepinhui_backend.vo.stats.TraceStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "统计-公开接口", description = "公开统计查询接口")
@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/trace")
    @Operation(
        summary = "溯源统计总览",
        description = "查询溯源统计总览数据，含 Redis 1小时缓存"
    )
    public Result<TraceStatsVO> getTraceStats() {
        return Result.success(statsService.getTraceStats());
    }

    @GetMapping("/trace/by-region")
    @Operation(
        summary = "区域溯源统计",
        description = "按地区查询溯源统计数据，含 Redis 1小时缓存"
    )
    public Result<List<RegionTraceStatsVO>> listTraceStatsByRegion(
        @Parameter(description = "地区筛选条件", example = "浙江省")
        @RequestParam(required = false) String region
    ) {
        return Result.success(statsService.listTraceStatsByRegion(region));
    }

    @GetMapping("/sales/by-region")
    @Operation(
        summary = "区域销售统计",
        description = "按地区查询销售统计数据，含 Redis 1小时缓存"
    )
    public Result<List<RegionSalesStatsVO>> listSalesStatsByRegion(
        @Parameter(description = "地区筛选条件", example = "浙江省")
        @RequestParam(required = false) String region
    ) {
        return Result.success(statsService.listSalesStatsByRegion(region));
    }
}
