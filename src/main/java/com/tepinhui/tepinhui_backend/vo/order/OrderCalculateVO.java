package com.tepinhui.tepinhui_backend.vo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "OrderCalculateVO", description = "订单算价响应")
public class OrderCalculateVO {

    @Schema(description = "各行明细")
    private List<OrderCalculateItemVO> items;

    @Schema(description = "商品小计", example = "256.00")
    private BigDecimal subtotal;

    @Schema(description = "运费（第一版固定为0）", example = "0.00")
    private BigDecimal shippingFee;

    @Schema(description = "优惠减免（第一版固定为0）", example = "0.00")
    private BigDecimal discount;

    @Schema(description = "应付总额", example = "256.00")
    private BigDecimal totalAmount;
}