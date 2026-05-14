package com.tepinhui.tepinhui_backend.vo.merchant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(name = "MerchantDetailVO", description = "商家详情响应")
public class MerchantDetailVO {

    @Schema(description = "商家ID")
    private Long id;

    @Schema(description = "关联用户ID")
    private Long userId;

    @Schema(description = "店铺名称")
    private String shopName;

    @Schema(description = "营业执照号")
    private String licenseNo;

    @Schema(description = "资质文件URL")
    private String qualification;

    @Schema(description = "审核状态：pending/approved/rejected")
    private String status;

    @Schema(description = "审核备注")
    private String auditRemark;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
