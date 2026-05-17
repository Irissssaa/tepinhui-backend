package com.tepinhui.tepinhui_backend.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tepinhui.tepinhui_backend.common.Result;
import com.tepinhui.tepinhui_backend.service.ReviewService;
import com.tepinhui.tepinhui_backend.vo.review.ReviewListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "评价管理", description = "商品评价查询与删除接口；评价创建仍由订单接口承载")
@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/api/v1/products/{productId}/reviews")
    @Operation(
        summary = "商品评价列表",
        description = "分页查询指定商品的评价列表，公开访问"
    )
    public Result<IPage<ReviewListVO>> getProductReviews(
        @Parameter(description = "商品ID", required = true)
        @PathVariable Long productId,
        @Parameter(description = "页码，从1开始")
        @RequestParam(defaultValue = "1") int page,
        @Parameter(description = "每页条数")
        @RequestParam(defaultValue = "10") int size
    ) {
        return Result.success(reviewService.getProductReviews(productId, page, size));
    }

    @GetMapping("/api/v1/user/reviews")
    @Operation(
        summary = "当前用户的评价列表",
        description = "分页查询当前登录用户发布的评价"
    )
    public Result<IPage<ReviewListVO>> getCurrentUserReviews(
        @Parameter(description = "页码，从1开始")
        @RequestParam(defaultValue = "1") int page,
        @Parameter(description = "每页条数")
        @RequestParam(defaultValue = "10") int size
    ) {
        return Result.success(reviewService.getCurrentUserReviews(page, size));
    }

    @DeleteMapping("/api/v1/reviews/{id}")
    @Operation(
        summary = "删除评价",
        description = "用户可删除自己的评价，管理员可删除任意评价"
    )
    public Result<Void> deleteReview(
        @Parameter(description = "评价ID", required = true)
        @PathVariable Long id
    ) {
        reviewService.deleteReview(id);
        return Result.success("评价删除成功", null);
    }
}
