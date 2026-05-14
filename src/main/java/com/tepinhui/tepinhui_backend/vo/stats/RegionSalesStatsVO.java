package com.tepinhui.tepinhui_backend.vo.stats;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(name = "RegionSalesStatsVO", description = "区域销售统计响应")
public class RegionSalesStatsVO {

    @Schema(description = "地区名称", example = "浙江省")
    private String region;

    @Schema(description = "销售额", example = "0.00")
    private BigDecimal salesAmount;
}
