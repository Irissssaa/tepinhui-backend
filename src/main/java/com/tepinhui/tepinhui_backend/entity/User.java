package com.tepinhui.tepinhui_backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.tepinhui.tepinhui_backend.common.Role;
import com.tepinhui.tepinhui_backend.common.UserStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String phone;

    private String email;

    private String nickname;

    private String avatarUrl;

    private String password;

    private Role role;

    private UserStatus status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
