package com.tepinhui.tepinhui_backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("origin")
public class Origin {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String provinceName;
    private String cityName;
    private String countyName;
    private String geoCode;
    private java.math.BigDecimal longitude;
    private java.math.BigDecimal latitude;
    private String address;
}
