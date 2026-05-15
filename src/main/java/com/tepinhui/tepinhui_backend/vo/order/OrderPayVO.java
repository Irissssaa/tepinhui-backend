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
@Schema(name = "OrderPayVO", description = "订单支付响应")
public class OrderPayVO {

    @Schema(description = "订单ID", example = "3001")
    private Long orderId;

    @Schema(description = "订单号", example = "ORD202405150001")
    private String orderNo;

    @Schema(description = "支付金额", example = "256.00")
    private BigDecimal totalAmount;

    @Schema(description = "支付跳转URL（模拟支付时可为空）")
    private String payUrl;

    @Schema(description = "支付状态（pending/success）", example = "pending")
    private String status;
}