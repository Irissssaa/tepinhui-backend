package com.tepinhui.tepinhui_backend.vo.review;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(name = "ReviewListVO", description = "评价列表项")
public class ReviewListVO {

    @Schema(description = "评价ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户昵称(脱敏)")
    private String username;

    @Schema(description = "用户头像")
    private String avatarUrl;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "评分，范围1-5")
    private Integer rating;

    @Schema(description = "评价内容")
    private String content;

    @Schema(description = "评价图片URL列表")
    private List<String> images;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
