package com.tepinhui.tepinhui_backend.vo.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(name = "ProductPageVO", description = "商品分页响应")
public class ProductPageVO {

    @Schema(description = "商品列表")
    private List<ProductListVO> records;

    @Schema(description = "总数")
    private Long total;

    @Schema(description = "当前页")
    private Long page;

    @Schema(description = "每页大小")
    private Long size;
}
