package com.tepinhui.tepinhui_backend.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "OrderShipRequest", description = "订单发货请求")
public class OrderShipRequest {

    @NotBlank(message = "物流单号不能为空")
    @Size(max = 64, message = "物流单号不能超过64个字符")
    @Schema(description = "物流单号", example = "SF1234567890")
    private String logisticsNo;
}
