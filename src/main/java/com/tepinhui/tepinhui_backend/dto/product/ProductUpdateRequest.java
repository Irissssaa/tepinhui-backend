package com.tepinhui.tepinhui_backend.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(name = "ProductUpdateRequest", description = "商品更新请求")
public class ProductUpdateRequest {

    @NotNull(message = "特产ID不能为空")
    @Schema(description = "特产ID")
    private Long specialtyId;

    @NotBlank(message = "商品名不能为空")
    @Schema(description = "商品名")
    private String name;

    @Schema(description = "商品描述")
    private String description;

    @NotNull(message = "商品价格不能为空")
    @DecimalMin(value = "0.01", inclusive = true, message = "商品价格必须大于0")
    @Schema(description = "商品价格")
    private BigDecimal price;

    @NotNull(message = "商品库存不能为空")
    @Min(value = 0, message = "商品库存不能小于0")
    @Schema(description = "商品库存")
    private Integer stock;

    @Schema(description = "商品图片JSON字符串")
    private String images;
}
