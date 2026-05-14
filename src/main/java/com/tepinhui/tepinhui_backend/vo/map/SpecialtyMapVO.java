package com.tepinhui.tepinhui_backend.vo.map;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "SpecialtyMapVO", description = "地图特产分布响应")
public class SpecialtyMapVO {

    @Schema(description = "地区名称", example = "浙江省")
    private String region;

    @Schema(description = "特产名称", example = "龙井茶")
    private String specialtyName;

    @Schema(description = "特产数量", example = "0")
    private Integer count;

    @Schema(description = "经度", example = "120.15507")
    private Double longitude;

    @Schema(description = "纬度", example = "30.274084")
    private Double latitude;
}
