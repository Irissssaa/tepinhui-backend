package com.tepinhui.tepinhui_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tepinhui.tepinhui_backend.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 乐观锁扣减库存：仅当库存 >= quantity 时才扣减。
     *
     * @param productId 商品ID
     * @param quantity  扣减数量
     * @return 受影响行数（0 表示库存不足）
     */
    @Update("UPDATE product SET stock = stock - #{quantity}, updated_at = NOW() WHERE id = #{productId} AND stock >= #{quantity}")
    int deductStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    /**
     * 回滚库存：取消订单时将库存加回。
     *
     * @param productId 商品ID
     * @param quantity  回滚数量
     * @return 受影响行数
     */
    @Update("UPDATE product SET stock = stock + #{quantity}, updated_at = NOW() WHERE id = #{productId}")
    int rollbackStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}
