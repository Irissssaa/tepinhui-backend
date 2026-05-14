package com.tepinhui.tepinhui_backend.common;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Getter
public enum OrderStatus {

    PENDING("pending", "待支付"),
    PAID("paid", "已支付"),
    SHIPPED("shipped", "已发货"),
    DONE("done", "已完成"),
    CANCELLED("cancelled", "已取消");

    @EnumValue
    @JsonValue
    private final String value;
    private final String description;

    OrderStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
            PENDING, EnumSet.of(PAID, CANCELLED),
            PAID, EnumSet.of(SHIPPED, CANCELLED),
            SHIPPED, EnumSet.of(DONE),
            DONE, EnumSet.noneOf(OrderStatus.class),
            CANCELLED, EnumSet.noneOf(OrderStatus.class)
    );

    public boolean canTransitionTo(OrderStatus next) {
        return TRANSITIONS.getOrDefault(this, EnumSet.noneOf(OrderStatus.class)).contains(next);
    }

    @JsonCreator
    public static OrderStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (OrderStatus s : values()) {
            if (s.value.equalsIgnoreCase(value)) {
                return s;
            }
        }
        throw new IllegalArgumentException("无效的订单状态: " + value);
    }
}
