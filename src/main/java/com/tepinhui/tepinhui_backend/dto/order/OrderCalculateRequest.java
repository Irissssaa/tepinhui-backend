package com.tepinhui.tepinhui_backend.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(name = "OrderCalculateRequest", description = "订单算价请求")
public class OrderCalculateRequest {

    @NotNull(message = "收货地址ID不能为空")
    @Schema(description = "收货地址ID，用于运费计算", example = "1001")
    private Long addressId;

    @Valid
    @NotEmpty(message = "商品项列表不能为空")
    @Schema(description = "商品项列表")
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
