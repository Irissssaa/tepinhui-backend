package com.tepinhui.tepinhui_backend.service;

import com.tepinhui.tepinhui_backend.dto.cart.CartAddRequest;
import com.tepinhui.tepinhui_backend.dto.cart.CartUpdateRequest;
import com.tepinhui.tepinhui_backend.vo.cart.CartVO;

public interface CartService {

    void addCartItem(CartAddRequest request);

    CartVO getCurrentUserCart();

    void updateCartItemQuantity(Long id, CartUpdateRequest request);

    void deleteCartItem(Long id);

    void clearCurrentUserCart();
}
