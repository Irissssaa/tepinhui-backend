package com.tepinhui.tepinhui_backend.service.impl;

import com.tepinhui.tepinhui_backend.dto.cart.CartAddRequest;
import com.tepinhui.tepinhui_backend.dto.cart.CartUpdateRequest;
import com.tepinhui.tepinhui_backend.exception.BusinessException;
import com.tepinhui.tepinhui_backend.service.CartService;
import com.tepinhui.tepinhui_backend.vo.cart.CartVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartServiceImpl implements CartService {

    @Override
    public void addCartItem(CartAddRequest request) {
        throw new BusinessException(501, "加入购物车业务逻辑待实现");
    }

    @Override
    public CartVO getCurrentUserCart() {
        CartVO cartVO = new CartVO();
        cartVO.setItems(List.of());
        cartVO.setTotalQuantity(0);
        cartVO.setTotalAmount(BigDecimal.ZERO);
        return cartVO;
    }

    @Override
    public void updateCartItemQuantity(Long id, CartUpdateRequest request) {
        throw new BusinessException(501, "更新购物车数量业务逻辑待实现");
    }

    @Override
    public void deleteCartItem(Long id) {
        throw new BusinessException(501, "删除购物车项业务逻辑待实现");
    }

    @Override
    public void clearCurrentUserCart() {
        throw new BusinessException(501, "清空购物车业务逻辑待实现");
    }
}
