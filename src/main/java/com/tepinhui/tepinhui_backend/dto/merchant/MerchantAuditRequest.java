package com.tepinhui.tepinhui_backend.dto.merchant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "MerchantAuditRequest", description = "商家入驻审核请求")
public class MerchantAuditRequest {

    @NotBlank(message = "审核状态不能为空")
    @Pattern(regexp = "^(approved|rejected)$", message = "审核状态只能是 approved 或 rejected")
    @Schema(description = "审核状态：approved=通过，rejected=驳回", example = "approved")
    private String status;

    @Size(max = 200, message = "审核备注不能超过200个字符")
    @Schema(description = "审核备注", example = "资质齐全，审核通过")
    private String auditRemark;
}
