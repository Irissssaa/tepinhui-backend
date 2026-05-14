package com.tepinhui.tepinhui_backend.dto.merchant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "MerchantApplyRequest", description = "商家入驻申请请求")
public class MerchantApplyRequest {

    @NotBlank(message = "店铺名称不能为空")
    @Size(max = 100, message = "店铺名称不能超过100个字符")
    @Schema(description = "店铺名称", example = "赣南脐橙旗舰店")
    private String shopName;

    @NotBlank(message = "营业执照号不能为空")
    @Size(max = 50, message = "营业执照号不能超过50个字符")
    @Schema(description = "营业执照号", example = "91360000MA0000000X")
    private String licenseNo;

    @Size(max = 255, message = "资质文件URL不能超过255个字符")
    @Schema(description = "资质文件URL", example = "https://oss.example.com/license/demo.pdf")
    private String qualification;
}
