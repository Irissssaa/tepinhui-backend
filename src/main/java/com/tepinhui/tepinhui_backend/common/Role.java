package com.tepinhui.tepinhui_backend.common;

import lombok.Getter;

@Getter
public enum Role {

    CONSUMER("消费者"),
    MERCHANT("商户"),
    ADMIN("管理员");

    private final String description;

    Role(String description) {
        this.description = description;
    }
}
