package com.tepinhui.tepinhui_backend.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "OrderPayRequest", description = "订单支付请求")
public class OrderPayRequest {

    @Schema(description = "支付方式（如 mock、wechat、alipay）", example = "mock", defaultValue = "mock")
    private String payMethod;
}