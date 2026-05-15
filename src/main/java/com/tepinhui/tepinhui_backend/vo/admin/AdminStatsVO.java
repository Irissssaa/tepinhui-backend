package com.tepinhui.tepinhui_backend.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "平台看板数据")
public class AdminStatsVO {

    @Schema(description = "总用户数")
    private Long userCount;

    @Schema(description = "已通过商家数")
    private Long merchantCount;

    @Schema(description = "商品总数")
    private Long productCount;

    @Schema(description = "订单总数")
    private Long orderCount;

    @Schema(description = "总销售额（已完成+已发货订单金额之和）")
    private BigDecimal totalSalesAmount;

    @Schema(description = "溯源记录总数")
    private Long traceCount;

    @Schema(description = "待审核溯源数")
    private Long pendingTraceCount;

    @Schema(description = "待审核商家数")
    private Long pendingMerchantCount;
}
