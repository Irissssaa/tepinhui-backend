package com.tepinhui.tepinhui_backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("culture_content")
public class CultureContent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long specialtyId;
    private String title;
    private String content;
    private String type;
    private String coverImg;
    private Integer sortOrder;
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
