package com.tepinhui.tepinhui_backend.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(name = "OrderCreateRequest", description = "创建订单请求")
public class OrderCreateRequest {

    @NotNull(message = "收货地址ID不能为空")
    @Schema(description = "收货地址ID", example = "1001")
    private Long addressId;

    @Size(max = 255, message = "订单备注不能超过255个字符")
    @Schema(description = "订单备注", example = "请尽快发货")
    private String remark;

    @Valid
    @NotEmpty(message = "订单商品项不能为空")
    @Schema(description = "订单商品项列表")
    private List<OrderItemRequest> items;

    @Data
    @Schema(name = "OrderItemRequest", description = "订单商品项请求")
    public static class OrderItemRequest {

        @NotNull(message = "商品ID不能为空")
        @Schema(description = "商品ID", example = "2001")
        private Long productId;

        @NotNull(message = "购买数量不能为空")
        @Min(value = 1, message = "购买数量必须大于等于1")
        @Schema(description = "购买数量", example = "2")
        private Integer quantity;
    }
}
