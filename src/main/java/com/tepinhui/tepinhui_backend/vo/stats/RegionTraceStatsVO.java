package com.tepinhui.tepinhui_backend.vo.stats;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "RegionTraceStatsVO", description = "区域溯源统计响应")
public class RegionTraceStatsVO {

    @Schema(description = "地区名称", example = "浙江省")
    private String region;

    @Schema(description = "溯源数量", example = "0")
    private Long count;
}
