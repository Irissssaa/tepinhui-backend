package com.tepinhui.tepinhui_backend.vo.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(name = "CartVO", description = "购物车响应")
public class CartVO {

    @Schema(description = "购物车项列表")
    private List<CartItemVO> items;

    @Schema(description = "购物车商品总数量")
    private Integer totalQuantity;

    @Schema(description = "购物车总金额")
    private BigDecimal totalAmount;
}
