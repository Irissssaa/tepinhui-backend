package com.tepinhui.tepinhui_backend.common;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum UserStatus {

    DISABLED(0, "禁用"),
    ENABLED(1, "启用");

    @EnumValue
    private final int value;
    private final String description;

    UserStatus(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public static UserStatus fromValue(int value) {
        for (UserStatus s : values()) {
            if (s.value == value) {
                return s;
            }
        }
        throw new IllegalArgumentException("无效的状态值: " + value);
    }
}
