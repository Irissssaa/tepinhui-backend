package com.tepinhui.tepinhui_backend.service.impl;

import com.tepinhui.tepinhui_backend.dto.review.ReviewCreateRequest;
import com.tepinhui.tepinhui_backend.exception.BusinessException;
import com.tepinhui.tepinhui_backend.service.ReviewService;
import com.tepinhui.tepinhui_backend.vo.review.ReviewVO;
import org.springframework.stereotype.Service;

@Service
public class ReviewServiceImpl implements ReviewService {

    @Override
    public ReviewVO createOrderReview(Long orderId, ReviewCreateRequest request) {
        throw new BusinessException(501, "创建订单评价业务逻辑待实现");
    }
}
