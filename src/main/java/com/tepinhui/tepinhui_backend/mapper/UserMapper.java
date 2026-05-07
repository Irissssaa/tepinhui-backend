package com.tepinhui.tepinhui_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tepinhui.tepinhui_backend.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
