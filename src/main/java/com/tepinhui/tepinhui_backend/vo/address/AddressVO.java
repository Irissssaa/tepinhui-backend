package com.tepinhui.tepinhui_backend.vo.address;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(name = "AddressVO", description = "收货地址响应")
public class AddressVO {

    @Schema(description = "地址ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "收货人")
    private String consignee;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "省份")
    private String province;

    @Schema(description = "城市")
    private String city;

    @Schema(description = "区县")
    private String county;

    @Schema(description = "详细地址")
    private String detail;

    @Schema(description = "是否默认地址：0否，1是")
    private Integer isDefault;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
