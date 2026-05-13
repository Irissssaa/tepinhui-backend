package com.tepinhui.tepinhui_backend.dto.review;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(name = "ReviewCreateRequest", description = "创建订单评价请求")
public class ReviewCreateRequest {

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分不能低于1分")
    @Max(value = 5, message = "评分不能高于5分")
    @Schema(description = "评分，范围1-5", example = "5")
    private Integer rating;

    @Size(max = 1000, message = "评价内容不能超过1000个字符")
    @Schema(description = "评价内容", example = "商品包装完整，口感不错")
    private String content;

    @Schema(description = "评价图片URL列表")
    private List<String> images;
}
