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
        summary = "加入购物车",
        description = "将商品加入当前登录用户购物车，已存在相同商品则累加数量；下架或不存在的商品会被拒绝，合并后数量超过库存也会被拒绝"
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
        summary = "购物车列表",
        description = "查询当前登录用户的购物车列表，已下架或被删除的商品不返回；返回项包含实时商品名、单价、首图与小计，并汇总总数量与总金额"
    )
    public Result<CartVO> getCurrentUserCart() {
        return Result.success(cartService.getCurrentUserCart());
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "修改购物车数量",
        description = "根据购物车项ID修改商品数量；仅本人可操作（越权返回403），数量需 ≥ 1 且不超过商品库存，关联商品下架时不允许修改"
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
        summary = "删除购物车项",
        description = "根据购物车项ID物理删除当前登录用户购物车中的对应商品；仅本人可操作（越权返回403），购物车项不存在返回404"
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
        summary = "清空购物车",
        description = "清空当前登录用户购物车中的全部商品（物理删除）；空购物车也返回成功"
    )
    public Result<Void> clearCurrentUserCart() {
        cartService.clearCurrentUserCart();
        return Result.success("购物车已清空", null);
    }
}
