package com.tepinhui.tepinhui_backend.controller;

import com.tepinhui.tepinhui_backend.common.Result;
import com.tepinhui.tepinhui_backend.dto.merchant.MerchantApplyRequest;
import com.tepinhui.tepinhui_backend.service.MerchantService;
import com.tepinhui.tepinhui_backend.vo.merchant.MerchantDetailVO;
import com.tepinhui.tepinhui_backend.vo.merchant.MerchantStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "商家-商家侧接口", description = "商家入驻申请、商家资料和经营数据接口")
@RestController
@RequestMapping("/api/v1/merchant")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    @PostMapping("/apply")
    @Operation(
        summary = "商家入驻申请（未实现）",
        description = "已登录用户提交商家入驻申请，后续审核通过后由后端授予 MERCHANT 角色；当前接口仅保留契约，业务逻辑待实现"
    )
    public Result<Long> apply(
        @Parameter(description = "商家入驻申请", required = true)
        @RequestBody @Valid MerchantApplyRequest request
    ) {
        Long merchantId = merchantService.apply(request);
        return Result.success("商家入驻申请已提交", merchantId);
    }

    @GetMapping("/profile")
    @Operation(
        summary = "当前商家资料（未实现）",
        description = "查询当前登录用户关联的商家资料；当前接口仅保留契约，业务逻辑待实现"
    )
    public Result<MerchantDetailVO> getProfile() {
        return Result.success(merchantService.getCurrentMerchantProfile());
    }

    @GetMapping("/stats")
    @Operation(
        summary = "商家经营数据（未实现）",
        description = "（未实现：当前返回 0 值统计占位数据）商家查看商品、订单、销售额和浏览量等经营数据"
    )
    public Result<MerchantStatsVO> getStats() {
        return Result.success(merchantService.getCurrentMerchantStats());
    }
}
