package com.tepinhui.tepinhui_backend.service;

import com.tepinhui.tepinhui_backend.dto.product.ProductCreateRequest;
import com.tepinhui.tepinhui_backend.dto.product.ProductQueryRequest;
import com.tepinhui.tepinhui_backend.dto.product.ProductUpdateRequest;
import com.tepinhui.tepinhui_backend.vo.product.ProductDetailVO;
import com.tepinhui.tepinhui_backend.vo.product.ProductPageVO;

public interface ProductService {

    /**
     * 公开商品分页查询，仅返回已上架商品
     */
    ProductPageVO pageProducts(ProductQueryRequest request);

    /**
     * 公开商品详情查询，仅返回已上架商品
     */
    ProductDetailVO getProductDetail(Long id);

    /**
     * 商家创建商品，默认进入审核状态
     */
    Long createProduct(ProductCreateRequest request);

    /**
     * 商家更新自有商品，管理员可越权更新
     */
    void updateProduct(Long id, ProductUpdateRequest request);

    /**
     * 商家删除自有商品，管理员可越权删除
     */
    void deleteProduct(Long id);
}
