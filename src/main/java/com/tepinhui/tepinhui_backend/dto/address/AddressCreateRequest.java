package com.tepinhui.tepinhui_backend.dto.address;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "AddressCreateRequest", description = "新增收货地址请求")
public class AddressCreateRequest {

    @NotBlank(message = "收货人不能为空")
    @Size(max = 50, message = "收货人不能超过50个字符")
    @Schema(description = "收货人", example = "张三")
    private String consignee;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @NotBlank(message = "省份不能为空")
    @Size(max = 50, message = "省份不能超过50个字符")
    @Schema(description = "省份", example = "浙江省")
    private String province;

    @NotBlank(message = "城市不能为空")
    @Size(max = 50, message = "城市不能超过50个字符")
    @Schema(description = "城市", example = "杭州市")
    private String city;

    @NotBlank(message = "区县不能为空")
    @Size(max = 50, message = "区县不能超过50个字符")
    @Schema(description = "区县", example = "西湖区")
    private String county;

    @NotBlank(message = "详细地址不能为空")
    @Size(max = 255, message = "详细地址不能超过255个字符")
    @Schema(description = "详细地址", example = "文三路 100 号 2 单元 501")
    private String detail;

    @Schema(description = "是否默认地址：0否，1是", example = "0")
    private Integer isDefault;
}
