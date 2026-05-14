package com.tepinhui.tepinhui_backend.controller;

import com.tepinhui.tepinhui_backend.common.Result;
import com.tepinhui.tepinhui_backend.dto.order.OrderCreateRequest;
import com.tepinhui.tepinhui_backend.dto.order.OrderQueryRequest;
import com.tepinhui.tepinhui_backend.dto.order.OrderShipRequest;
import com.tepinhui.tepinhui_backend.dto.review.ReviewCreateRequest;
import com.tepinhui.tepinhui_backend.service.OrderService;
import com.tepinhui.tepinhui_backend.service.ReviewService;
import com.tepinhui.tepinhui_backend.vo.order.OrderDetailVO;
import com.tepinhui.tepinhui_backend.vo.order.OrderPageVO;
import com.tepinhui.tepinhui_backend.vo.review.ReviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "订单-用户侧接口", description = "订单管理、发货流转与订单评价接口")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final ReviewService reviewService;

    @PostMapping
    @Operation(
        summary = "创建订单（未实现）",
        description = "提交当前登录用户订单；当前接口仅保留契约，真实下单、库存扣减和支付逻辑待实现"
    )
    public Result<OrderDetailVO> createOrder(
        @Parameter(description = "创建订单请求", required = true)
        @RequestBody @Valid OrderCreateRequest request
    ) {
        return Result.success("订单创建成功", orderService.createOrder(request));
    }

    @GetMapping
    @Operation(
        summary = "订单分页列表（未实现）",
        description = "（未实现：当前返回空分页数据占位）分页查询当前登录用户订单，分页字段保持稳定"
    )
    public Result<OrderPageVO> getOrders(@Valid @ModelAttribute OrderQueryRequest request) {
        return Result.success(orderService.getCurrentUserOrders(request));
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "订单详情（未实现）",
        description = "根据订单ID查询订单详情；当前接口仅保留契约，业务逻辑待实现"
    )
    public Result<OrderDetailVO> getOrderDetail(
        @Parameter(description = "订单ID", required = true)
        @PathVariable Long id
    ) {
        return Result.success(orderService.getOrderDetail(id));
    }

    @PutMapping("/{id}/cancel")
    @Operation(
        summary = "取消订单（未实现）",
        description = "取消指定订单；当前接口仅保留契约，业务逻辑待实现"
    )
    public Result<Void> cancelOrder(
        @Parameter(description = "订单ID", required = true)
        @PathVariable Long id
    ) {
        orderService.cancelOrder(id);
        return Result.success("订单取消成功", null);
    }

    @PutMapping("/{id}/confirm")
    @Operation(
        summary = "确认收货（未实现）",
        description = "确认指定订单已收货；当前接口仅保留契约，业务逻辑待实现"
    )
    public Result<Void> confirmOrder(
        @Parameter(description = "订单ID", required = true)
        @PathVariable Long id
    ) {
        orderService.confirmOrder(id);
        return Result.success("订单确认收货成功", null);
    }

    @PutMapping("/{id}/ship")
    @Operation(
        summary = "订单发货（未实现）",
        description = "商家为指定订单录入物流单号并发货；当前接口仅保留契约，业务逻辑待实现"
    )
    public Result<Void> shipOrder(
        @Parameter(description = "订单ID", required = true)
        @PathVariable Long id,
        @Parameter(description = "订单发货请求", required = true)
        @RequestBody @Valid OrderShipRequest request
    ) {
        orderService.shipOrder(id, request);
        return Result.success("订单发货成功", null);
    }

    @PostMapping("/{id}/review")
    @Operation(
        summary = "评价订单（未实现）",
        description = "为指定订单创建评价；当前接口仅保留契约，评价资格校验和真实业务逻辑待实现"
    )
    public Result<ReviewVO> createOrderReview(
        @Parameter(description = "订单ID", required = true)
        @PathVariable Long id,
        @Parameter(description = "创建评价请求", required = true)
        @RequestBody @Valid ReviewCreateRequest request
    ) {
        return Result.success("订单评价提交成功", reviewService.createOrderReview(id, request));
    }
}
