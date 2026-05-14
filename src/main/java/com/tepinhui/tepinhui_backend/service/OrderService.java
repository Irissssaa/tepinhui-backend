package com.tepinhui.tepinhui_backend.service;

import com.tepinhui.tepinhui_backend.dto.order.OrderCreateRequest;
import com.tepinhui.tepinhui_backend.dto.order.OrderQueryRequest;
import com.tepinhui.tepinhui_backend.dto.order.OrderShipRequest;
import com.tepinhui.tepinhui_backend.vo.order.OrderDetailVO;
import com.tepinhui.tepinhui_backend.vo.order.OrderPageVO;

public interface OrderService {

    OrderDetailVO createOrder(OrderCreateRequest request);

    OrderPageVO getCurrentUserOrders(OrderQueryRequest request);

    OrderDetailVO getOrderDetail(Long id);

    void cancelOrder(Long id);

    void confirmOrder(Long id);

    void shipOrder(Long id, OrderShipRequest request);
}
