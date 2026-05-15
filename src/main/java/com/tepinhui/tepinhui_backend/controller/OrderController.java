package com.tepinhui.tepinhui_backend.controller;

import com.tepinhui.tepinhui_backend.common.Result;
import com.tepinhui.tepinhui_backend.dto.order.OrderCalculateRequest;
import com.tepinhui.tepinhui_backend.dto.order.OrderCreateRequest;
import com.tepinhui.tepinhui_backend.dto.order.OrderPayRequest;
import com.tepinhui.tepinhui_backend.dto.order.OrderQueryRequest;
import com.tepinhui.tepinhui_backend.dto.order.OrderShipRequest;
import com.tepinhui.tepinhui_backend.dto.review.ReviewCreateRequest;
import com.tepinhui.tepinhui_backend.service.OrderService;
import com.tepinhui.tepinhui_backend.service.ReviewService;
import com.tepinhui.tepinhui_backend.vo.order.OrderCalculateVO;
import com.tepinhui.tepinhui_backend.vo.order.OrderDetailVO;
import com.tepinhui.tepinhui_backend.vo.order.OrderPageVO;
import com.tepinhui.tepinhui_backend.vo.order.OrderPayVO;
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
        summary = "创建订单",
        description = "创建订单并扣减库存，校验商品价格与库存可用性，返回订单详情（含商品项列表、金额明细）"
    )
    public Result<OrderDetailVO> createOrder(
        @Parameter(description = "创建订单请求", required = true)
        @RequestBody @Valid OrderCreateRequest request
    ) {
        return Result.success("订单创建成功", orderService.createOrder(request));
    }

    @GetMapping
    @Operation(
        summary = "订单分页列表",
        description = "分页查询当前登录用户订单，支持按订单状态筛选，返回分页数据（records、total、page、size）"
    )
    public Result<OrderPageVO> getOrders(@Valid @ModelAttribute OrderQueryRequest request) {
        return Result.success(orderService.getCurrentUserOrders(request));
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "订单详情",
        description = "根据订单ID查询订单详情，包含订单商品项列表；订单不存在返回404，无权访问返回403"
    )
    public Result<OrderDetailVO> getOrderDetail(
        @Parameter(description = "订单ID", required = true)
        @PathVariable Long id
    ) {
        return Result.success(orderService.getOrderDetail(id));
    }

    @PutMapping("/{id}/cancel")
    @Operation(
        summary = "取消订单",
        description = "取消指定订单并恢复库存，仅 pending 状态的订单可取消；订单不存在返回404，状态不可取消返回400"
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
        summary = "确认收货",
        description = "确认指定订单已收货，将订单状态从 shipped 更新为 done；仅 shipped 状态的订单可确认；订单不存在返回404，状态不可确认返回400"
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
        summary = "订单发货",
        description = "商家确认发货并填写物流单号，将订单状态从 paid 更新为 shipped；仅 paid 状态的订单可发货；订单不存在返回404，状态不可发货返回400"
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
        summary = "评价订单",
        description = "为已完成订单创建商品评价，校验评价资格（订单状态须为 done 且未评价）；订单不存在返回404，无评价资格返回400"
    )
    public Result<ReviewVO> createOrderReview(
        @Parameter(description = "订单ID", required = true)
        @PathVariable Long id,
        @Parameter(description = "创建评价请求", required = true)
        @RequestBody @Valid ReviewCreateRequest request
    ) {
        return Result.success("订单评价提交成功", reviewService.createOrderReview(id, request));
    }

    @PostMapping("/calculate")
    @Operation(
        summary = "订单算价",
        description = "根据商品项和收货地址计算订单金额，返回商品明细、小计、运费、优惠及应付总额"
    )
    public Result<OrderCalculateVO> calculateOrder(
        @Parameter(description = "订单算价请求", required = true)
        @RequestBody @Valid OrderCalculateRequest request
    ) {
        return Result.success(orderService.calculateOrder(request));
    }

    @PostMapping("/{id}/pay")
    @Operation(
        summary = "订单支付（模拟）",
        description = "为指定订单发起模拟支付，将订单状态从 pending 更新为 paid；订单不存在返回404，无权操作返回403，状态不可支付返回400"
    )
    public Result<OrderPayVO> payOrder(
        @Parameter(description = "订单ID", required = true)
        @PathVariable Long id,
        @Parameter(description = "订单支付请求", required = true)
        @RequestBody @Valid OrderPayRequest request
    ) {
        return Result.success(orderService.payOrder(id, request));
    }
}
