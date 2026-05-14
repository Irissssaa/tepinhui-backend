package com.tepinhui.tepinhui_backend.controller;

import com.tepinhui.tepinhui_backend.common.Result;
import com.tepinhui.tepinhui_backend.service.SpecialtyService;
import com.tepinhui.tepinhui_backend.vo.specialty.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "特产模块", description = "特产分布地图、详情、节气推荐")
public class SpecialtyController {

    private final SpecialtyService specialtyService;

    /**
     * 获取特产分布地图数据
     * GET /tph/api/v1/map/specialties
     */
    @GetMapping("/map/specialties")
    @Operation(
        summary = "特产分布地图",
        description = "获取全国特产分布数据，按省份分组，用于ECharts地图可视化"
    )
    public Result<List<RegionSpecialtyVO>> getSpecialtyMap() {
        List<RegionSpecialtyVO> map = specialtyService.getSpecialtyMap();
        return Result.success(map);
    }

    /**
     * 获取特产详情
     * GET /tph/api/v1/specialties/{id}
     */
    @GetMapping("/specialties/{id}")
    @Operation(
        summary = "特产详情",
        description = "获取特产详细信息，包括文化背景、产地坐标、季节标签等"
    )
    public Result<SpecialtyVO> getSpecialtyDetail(
        @Parameter(description = "特产ID", required = true)
        @PathVariable Long id
    ) {
        SpecialtyVO detail = specialtyService.getSpecialtyDetail(id);
        if (detail == null) {
            return Result.error(404, "特产不存在");
        }
        return Result.success(detail);
    }

    /**
     * 节气推荐
     * GET /tph/api/v1/map/season-recommend
     */
    @GetMapping("/map/season-recommend")
    @Operation(
        summary = "节气推荐",
        description = "根据当前季节返回推荐特产列表，匹配春/夏/秋/冬季标签"
    )
    public Result<SeasonRecommendVO> getSeasonRecommend() {
        SeasonRecommendVO recommend = specialtyService.getSeasonRecommend();
        return Result.success(recommend);
    }
}
