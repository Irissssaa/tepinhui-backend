package com.tepinhui.tepinhui_backend.controller;

import com.tepinhui.tepinhui_backend.common.Result;
import com.tepinhui.tepinhui_backend.service.MapService;
import com.tepinhui.tepinhui_backend.vo.map.SeasonRecommendVO;
import com.tepinhui.tepinhui_backend.vo.map.SpecialtyMapVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "地图-公开接口", description = "公开地图与时令推荐查询接口")
@RestController
@RequestMapping("/api/v1/map")
@RequiredArgsConstructor
public class MapController {

    private final MapService mapService;

    @GetMapping("/specialties")
    @Operation(
        summary = "特产地图分布（未实现）",
        description = "（未实现：当前返回空列表占位）查询地图特产分布数据，供前端先对接地图点位结构"
    )
    public Result<List<SpecialtyMapVO>> listSpecialties() {
        return Result.success(mapService.listSpecialties());
    }

    @GetMapping("/season-recommend")
    @Operation(
        summary = "时令特产推荐（未实现）",
        description = "（未实现：当前返回空列表占位）查询时令特产推荐列表，供前端先对接推荐模块结构"
    )
    public Result<List<SeasonRecommendVO>> listSeasonRecommendations() {
        return Result.success(mapService.listSeasonRecommendations());
    }
}
