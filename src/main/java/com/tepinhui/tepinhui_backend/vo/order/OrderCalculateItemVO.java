package com.tepinhui.tepinhui_backend.vo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "OrderCalculateItemVO", description = "订单算价商品行明细")
public class OrderCalculateItemVO {

    @Schema(description = "商品ID", example = "2001")
    private Long productId;

    @Schema(description = "商品名称", example = "西湖龙井特级")
    private String productName;

    @Schema(description = "商品单价", example = "128.00")
    private BigDecimal price;

    @Schema(description = "购买数量", example = "2")
    private Integer quantity;

    @Schema(description = "商品小计", example = "256.00")
    private BigDecimal subtotal;
}
