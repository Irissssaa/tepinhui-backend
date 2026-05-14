package com.tepinhui.tepinhui_backend.dto.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(name = "CartAddRequest", description = "加入购物车请求")
public class CartAddRequest {

    @NotNull(message = "商品ID不能为空")
    @Schema(description = "商品ID", example = "1001")
    private Long productId;

    @NotNull(message = "商品数量不能为空")
    @Min(value = 1, message = "商品数量必须大于等于1")
    @Schema(description = "商品数量", example = "2")
    private Integer quantity;
}
