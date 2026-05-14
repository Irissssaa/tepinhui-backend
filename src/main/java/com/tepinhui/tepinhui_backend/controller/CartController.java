package com.tepinhui.tepinhui_backend.controller;

import com.tepinhui.tepinhui_backend.common.Result;
import com.tepinhui.tepinhui_backend.dto.cart.CartAddRequest;
import com.tepinhui.tepinhui_backend.dto.cart.CartUpdateRequest;
import com.tepinhui.tepinhui_backend.service.CartService;
import com.tepinhui.tepinhui_backend.vo.cart.CartVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "购物车-用户侧接口", description = "当前登录用户的购物车管理接口")
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping
    @Operation(
        summary = "加入购物车（未实现）",
        description = "将商品加入当前登录用户购物车；当前接口仅保留契约，业务逻辑待实现"
    )
    public Result<Void> addCartItem(
        @Parameter(description = "加入购物车请求", required = true)
        @RequestBody @Valid CartAddRequest request
    ) {
        cartService.addCartItem(request);
        return Result.success("加入购物车成功", null);
    }

    @GetMapping
    @Operation(
        summary = "购物车列表（未实现）",
        description = "（未实现：当前返回空购物车占位数据）查询当前登录用户的购物车列表"
    )
    public Result<CartVO> getCurrentUserCart() {
        return Result.success(cartService.getCurrentUserCart());
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "修改购物车数量（未实现）",
        description = "根据购物车项ID修改当前登录用户购物车中的商品数量；当前接口仅保留契约，业务逻辑待实现"
    )
    public Result<Void> updateCartItemQuantity(
        @Parameter(description = "购物车项ID", required = true)
        @PathVariable Long id,
        @Parameter(description = "更新购物车数量请求", required = true)
        @RequestBody @Valid CartUpdateRequest request
    ) {
        cartService.updateCartItemQuantity(id, request);
        return Result.success("购物车数量更新成功", null);
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "删除购物车项（未实现）",
        description = "根据购物车项ID删除当前登录用户购物车中的商品；当前接口仅保留契约，业务逻辑待实现"
    )
    public Result<Void> deleteCartItem(
        @Parameter(description = "购物车项ID", required = true)
        @PathVariable Long id
    ) {
        cartService.deleteCartItem(id);
        return Result.success("购物车项删除成功", null);
    }

    @DeleteMapping
    @Operation(
        summary = "清空购物车（未实现）",
        description = "清空当前登录用户购物车中的全部商品；当前接口仅保留契约，业务逻辑待实现"
    )
    public Result<Void> clearCurrentUserCart() {
        cartService.clearCurrentUserCart();
        return Result.success("购物车已清空", null);
    }
}
