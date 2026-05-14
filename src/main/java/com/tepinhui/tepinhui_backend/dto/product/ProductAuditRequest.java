package com.tepinhui.tepinhui_backend.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "ProductAuditRequest", description = "商品审核请求")
public class ProductAuditRequest {

    @NotBlank(message = "审核结果不能为空")
    @Pattern(regexp = "^(on|off)$", message = "审核结果只能是 on 或 off")
    @Schema(description = "审核结果：on=审核通过并上架，off=审核驳回或下架", example = "on")
    private String status;

    @Size(max = 200, message = "审核备注不能超过200个字符")
    @Schema(description = "审核备注", example = "商品信息完整，允许上架")
    private String auditRemark;
}
