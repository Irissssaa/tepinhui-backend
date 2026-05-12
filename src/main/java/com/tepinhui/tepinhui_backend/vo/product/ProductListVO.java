package com.tepinhui.tepinhui_backend.vo.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(name = "ProductListVO", description = "商品列表项")
public class ProductListVO {

    @Schema(description = "商品ID")
    private Long id;

    @Schema(description = "特产ID")
    private Long specialtyId;

    @Schema(description = "商品名")
    private String name;

    @Schema(description = "商品描述")
    private String description;

    @Schema(description = "商品价格")
    private BigDecimal price;

    @Schema(description = "商品库存")
    private Integer stock;

    @Schema(description = "商品图片JSON字符串")
    private String images;

    @Schema(description = "商品状态")
    private String status;
}
