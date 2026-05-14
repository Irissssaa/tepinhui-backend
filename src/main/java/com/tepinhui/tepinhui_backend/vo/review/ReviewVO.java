package com.tepinhui.tepinhui_backend.vo.review;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(name = "ReviewVO", description = "订单评价响应")
public class ReviewVO {

    @Schema(description = "评价ID")
    private Long id;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "评分，范围1-5")
    private Integer rating;

    @Schema(description = "评价内容")
    private String content;

    @Schema(description = "评价图片URL列表")
    private List<String> images;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
