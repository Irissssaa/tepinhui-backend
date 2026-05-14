package com.tepinhui.tepinhui_backend.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(name = "OrderQueryRequest", description = "订单分页查询请求")
public class OrderQueryRequest {

    @Min(value = 1, message = "页码必须大于等于1")
    @Schema(description = "页码", example = "1", defaultValue = "1")
    private Integer page = 1;

    @Min(value = 1, message = "每页条数必须大于等于1")
    @Schema(description = "每页条数", example = "10", defaultValue = "10")
    private Integer size = 10;

    @Schema(description = "订单状态", example = "pending")
    private String status;
}
