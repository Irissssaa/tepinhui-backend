package com.tepinhui.tepinhui_backend.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(name = "AdminStatsVO", description = "平台看板响应")
public class AdminStatsVO {

    @Schema(description = "用户总数", example = "0")
    private Long userCount;

    @Schema(description = "商家总数", example = "0")
    private Long merchantCount;

    @Schema(description = "商品总数", example = "0")
    private Long productCount;

    @Schema(description = "订单总数", example = "0")
    private Long orderCount;

    @Schema(description = "销售总额", example = "0.00")
    private BigDecimal salesAmount;
}
