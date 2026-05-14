package com.tepinhui.tepinhui_backend.service.impl;

import com.tepinhui.tepinhui_backend.dto.order.OrderCreateRequest;
import com.tepinhui.tepinhui_backend.dto.order.OrderQueryRequest;
import com.tepinhui.tepinhui_backend.dto.order.OrderShipRequest;
import com.tepinhui.tepinhui_backend.exception.BusinessException;
import com.tepinhui.tepinhui_backend.service.OrderService;
import com.tepinhui.tepinhui_backend.vo.order.OrderDetailVO;
import com.tepinhui.tepinhui_backend.vo.order.OrderPageVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    @Override
    public OrderDetailVO createOrder(OrderCreateRequest request) {
        throw new BusinessException(501, "创建订单业务逻辑待实现");
    }

    @Override
    public OrderPageVO getCurrentUserOrders(OrderQueryRequest request) {
        OrderPageVO pageVO = new OrderPageVO();
        pageVO.setRecords(List.of());
        pageVO.setTotal(0L);
        pageVO.setPage(request.getPage());
        pageVO.setSize(request.getSize());
        return pageVO;
    }

    @Override
    public OrderDetailVO getOrderDetail(Long id) {
        throw new BusinessException(501, "查询订单详情业务逻辑待实现");
    }

    @Override
    public void cancelOrder(Long id) {
        throw new BusinessException(501, "取消订单业务逻辑待实现");
    }

    @Override
    public void confirmOrder(Long id) {
        throw new BusinessException(501, "确认收货业务逻辑待实现");
    }

    @Override
    public void shipOrder(Long id, OrderShipRequest request) {
        throw new BusinessException(501, "订单发货业务逻辑待实现");
    }
}
