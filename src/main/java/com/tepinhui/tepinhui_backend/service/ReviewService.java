package com.tepinhui.tepinhui_backend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tepinhui.tepinhui_backend.dto.review.ReviewCreateRequest;
import com.tepinhui.tepinhui_backend.vo.review.ReviewListVO;
import com.tepinhui.tepinhui_backend.vo.review.ReviewVO;

public interface ReviewService {

    ReviewVO createOrderReview(Long orderId, ReviewCreateRequest request);

    /**
     * 查询商品的评价列表（分页）
     * @param productId 商品ID
     * @param page 页码（从1开始）
     * @param size 每页条数
     * @return 分页评价列表
     */
    IPage<ReviewListVO> getProductReviews(Long productId, int page, int size);

    /**
     * 查询当前用户的评价列表（分页）
     * @param page 页码
     * @param size 每页条数
     * @return 分页评价列表
     */
    IPage<ReviewListVO> getCurrentUserReviews(int page, int size);

    /**
     * 删除评价（用户删自己的 / 管理员删任意）
     * @param reviewId 评价ID
     */
    void deleteReview(Long reviewId);
}
