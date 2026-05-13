package com.tepinhui.tepinhui_backend.vo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(name = "OrderPageVO", description = "订单分页响应")
public class OrderPageVO {

    @Schema(description = "订单记录列表")
    private List<OrderDetailVO> records;

    @Schema(description = "总记录数")
    private Long total;

    @Schema(description = "当前页码")
    private Integer page;

    @Schema(description = "每页条数")
    private Integer size;
}
