package com.tepinhui.tepinhui_backend.vo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(name = "OrderDetailVO", description = "订单详情响应")
public class OrderDetailVO {

    @Schema(description = "订单ID")
    private Long id;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "订单状态", example = "pending")
    private String status;

    @Schema(description = "订单总金额")
    private BigDecimal totalAmount;

    @Schema(description = "收货地址快照JSON")
    private String address;

    @Schema(description = "物流单号")
    private String logisticsNo;

    @Schema(description = "订单备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "订单商品项列表")
    private List<OrderItemVO> items;
}
