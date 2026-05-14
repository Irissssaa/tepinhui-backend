package com.tepinhui.tepinhui_backend.controller;

import com.tepinhui.tepinhui_backend.common.Result;
import com.tepinhui.tepinhui_backend.dto.product.ProductCreateRequest;
import com.tepinhui.tepinhui_backend.dto.product.ProductQueryRequest;
import com.tepinhui.tepinhui_backend.dto.product.ProductUpdateRequest;
import com.tepinhui.tepinhui_backend.service.ProductService;
import com.tepinhui.tepinhui_backend.vo.product.ProductDetailVO;
import com.tepinhui.tepinhui_backend.vo.product.ProductPageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "商品-公开与商家侧接口", description = "公开商品查询与商家商品维护接口")
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(
        summary = "商品分页列表",
        description = "按页查询公开商品列表，支持关键字、特产ID等筛选条件"
    )
    public Result<ProductPageVO> pageProducts(
        @Valid @ParameterObject ProductQueryRequest request
    ) {
        ProductPageVO pageVO = productService.pageProducts(request);
        return Result.success(pageVO);
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "商品详情",
        description = "根据商品ID查询公开商品详情，仅返回已上架商品"
    )
    public Result<ProductDetailVO> getProductDetail(
        @Parameter(description = "商品ID", required = true)
        @PathVariable Long id
    ) {
        ProductDetailVO detailVO = productService.getProductDetail(id);
        return Result.success(detailVO);
    }

    @PostMapping
    @Operation(
        summary = "创建商品",
        description = "商家创建商品，创建后默认进入审核状态"
    )
    public Result<Long> createProduct(
        @Parameter(description = "商品创建请求体", required = true)
        @RequestBody @Valid ProductCreateRequest request
    ) {
        Long productId = productService.createProduct(request);
        return Result.success("商品创建成功", productId);
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "更新商品",
        description = "按商品ID更新商品基础信息"
    )
    public Result<Void> updateProduct(
        @Parameter(description = "商品ID", required = true)
        @PathVariable Long id,
        @Parameter(description = "商品更新请求体", required = true)
        @RequestBody @Valid ProductUpdateRequest request
    ) {
        productService.updateProduct(id, request);
        return Result.success("商品更新成功", null);
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "删除商品",
        description = "按商品ID删除或下架商品，作为商品 CRUD 的删除能力"
    )
    public Result<Void> deleteProduct(
        @Parameter(description = "商品ID", required = true)
        @PathVariable Long id
    ) {
        productService.deleteProduct(id);
        return Result.success("商品删除成功", null);
    }
}
