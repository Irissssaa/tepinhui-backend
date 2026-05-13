package com.tepinhui.tepinhui_backend.controller.admin;

import com.tepinhui.tepinhui_backend.common.Result;
import com.tepinhui.tepinhui_backend.dto.product.ProductAuditRequest;
import com.tepinhui.tepinhui_backend.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "商品-管理端接口", description = "管理员审核商品上架申请接口")
@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

    @PutMapping("/{id}/audit")
    @Operation(
        summary = "审核商品（未实现）",
        description = "管理员审核商品上架申请，当前接口仅保留契约，业务逻辑待实现"
    )
    public Result<Void> auditProduct(
        @Parameter(description = "商品ID", required = true)
        @PathVariable Long id,
        @Parameter(description = "商品审核请求", required = true)
        @RequestBody @Valid ProductAuditRequest request
    ) {
        productService.auditProduct(id, request);
        return Result.success("商品审核完成", null);
    }
}
