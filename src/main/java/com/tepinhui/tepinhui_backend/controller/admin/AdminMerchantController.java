package com.tepinhui.tepinhui_backend.controller.admin;

import com.tepinhui.tepinhui_backend.common.Result;
import com.tepinhui.tepinhui_backend.dto.merchant.MerchantAuditRequest;
import com.tepinhui.tepinhui_backend.service.MerchantService;
import com.tepinhui.tepinhui_backend.vo.merchant.MerchantDetailVO;
import com.tepinhui.tepinhui_backend.vo.merchant.MerchantPageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "商家审核管理", description = "管理员审核商家入驻申请和查询商家列表")
@RestController
@RequestMapping("/api/v1/admin/merchant")
@RequiredArgsConstructor
public class AdminMerchantController {

    private final MerchantService merchantService;

    @GetMapping("/pending")
    @Operation(summary = "待审核商家列表", description = "管理员分页查询待审核商家入驻申请")
    public Result<MerchantPageVO> pagePending(
        @Parameter(description = "页码，从1开始")
        @RequestParam(defaultValue = "1") Long page,
        @Parameter(description = "每页大小")
        @RequestParam(defaultValue = "10") Long size
    ) {
        return Result.success(merchantService.pagePendingMerchants(page, size));
    }

    @GetMapping("/list")
    @Operation(summary = "商家列表", description = "管理员分页查询全部商家，可按审核状态筛选")
    public Result<MerchantPageVO> pageMerchants(
        @Parameter(description = "页码，从1开始")
        @RequestParam(defaultValue = "1") Long page,
        @Parameter(description = "每页大小")
        @RequestParam(defaultValue = "10") Long size,
        @Parameter(description = "审核状态：pending/approved/rejected")
        @RequestParam(required = false) String status
    ) {
        return Result.success(merchantService.pageMerchants(page, size, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "商家详情", description = "管理员查看商家入驻资料和审核状态")
    public Result<MerchantDetailVO> getDetail(
        @Parameter(description = "商家ID", required = true)
        @PathVariable Long id
    ) {
        return Result.success(merchantService.getMerchantDetail(id));
    }

    @PutMapping("/{id}/audit")
    @Operation(
        summary = "审核商家",
        description = "管理员审核商家入驻申请；通过后后续实现应同步把关联用户角色更新为 MERCHANT"
    )
    public Result<Void> audit(
        @Parameter(description = "商家ID", required = true)
        @PathVariable Long id,
        @Parameter(description = "审核请求", required = true)
        @RequestBody @Valid MerchantAuditRequest request
    ) {
        merchantService.auditMerchant(id, request);
        return Result.success("商家审核完成", null);
    }
}
