package com.tepinhui.tepinhui_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tepinhui.tepinhui_backend.entity.Product;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}
