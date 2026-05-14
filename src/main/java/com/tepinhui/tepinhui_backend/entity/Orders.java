package com.tepinhui.tepinhui_backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.tepinhui.tepinhui_backend.common.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("orders")
public class Orders {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String orderNo;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private String address;
    private String logisticsNo;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
