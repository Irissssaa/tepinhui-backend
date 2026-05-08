package com.tepinhui.tepinhui_backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("specialty")
public class Specialty {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long originId;
    private String name;
    private String category;
    private String culturalInfo;
    private String seasonTag;
    private String coverImg;
    private Integer isLanding;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
