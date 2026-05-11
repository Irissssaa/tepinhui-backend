package com.tepinhui.tepinhui_backend.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(name = "ProductQueryRequest", description = "商品分页查询请求")
public class ProductQueryRequest {

    @Min(value = 1, message = "页码必须大于0")
    @Schema(description = "页码", example = "1")
    private Long page = 1L;

    @Min(value = 1, message = "每页大小必须大于0")
    @Schema(description = "每页大小", example = "10")
    private Long size = 10L;

    @Schema(description = "商品关键词")
    private String keyword;

    @Schema(description = "特产ID")
    private Long specialtyId;

    @Schema(description = "商品状态")
    private String status;
}
