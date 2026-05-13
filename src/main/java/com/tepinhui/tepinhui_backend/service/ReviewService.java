package com.tepinhui.tepinhui_backend.service;

import com.tepinhui.tepinhui_backend.dto.review.ReviewCreateRequest;
import com.tepinhui.tepinhui_backend.vo.review.ReviewVO;

public interface ReviewService {

    ReviewVO createOrderReview(Long orderId, ReviewCreateRequest request);
}
