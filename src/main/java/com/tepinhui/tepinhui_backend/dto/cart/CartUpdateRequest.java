package com.tepinhui.tepinhui_backend.dto.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(name = "CartUpdateRequest", description = "更新购物车数量请求")
public class CartUpdateRequest {

    @NotNull(message = "商品数量不能为空")
    @Min(value = 1, message = "商品数量必须大于等于1")
    @Schema(description = "商品数量", example = "3")
    private Integer quantity;
}
