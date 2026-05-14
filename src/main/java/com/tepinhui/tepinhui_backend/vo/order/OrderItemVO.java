package com.tepinhui.tepinhui_backend.vo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(name = "OrderItemVO", description = "订单商品项响应")
public class OrderItemVO {

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "购买数量")
    private Integer quantity;

    @Schema(description = "商品单价")
    private BigDecimal unitPrice;

    @Schema(description = "商品小计")
    private BigDecimal subtotal;

    @Schema(description = "商品快照JSON")
    private String snapshot;
}
