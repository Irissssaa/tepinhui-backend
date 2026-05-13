package com.tepinhui.tepinhui_backend.vo.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(name = "CartItemVO", description = "购物车项响应")
public class CartItemVO {

    @Schema(description = "购物车项ID")
    private Long id;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "商品单价")
    private BigDecimal price;

    @Schema(description = "商品数量")
    private Integer quantity;

    @Schema(description = "小计金额")
    private BigDecimal subtotal;

    @Schema(description = "商品图片URL")
    private String imageUrl;
}
