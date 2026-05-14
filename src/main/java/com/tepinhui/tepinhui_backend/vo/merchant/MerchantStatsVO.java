package com.tepinhui.tepinhui_backend.vo.merchant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(name = "MerchantStatsVO", description = "商家经营数据响应")
public class MerchantStatsVO {

    @Schema(description = "商品数量")
    private Long productCount;

    @Schema(description = "订单数量")
    private Long orderCount;

    @Schema(description = "销售额")
    private BigDecimal salesAmount;

    @Schema(description = "浏览量")
    private Long viewCount;
}
