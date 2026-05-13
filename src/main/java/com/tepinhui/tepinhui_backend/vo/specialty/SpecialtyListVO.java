package com.tepinhui.tepinhui_backend.vo.specialty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "SpecialtyListVO", description = "特产列表项响应")
public class SpecialtyListVO {

    @Schema(description = "特产ID")
    private Long id;

    @Schema(description = "特产名称")
    private String name;

    @Schema(description = "分类名称")
    private String category;

    @Schema(description = "节气标签")
    private String seasonTag;

    @Schema(description = "封面图URL")
    private String coverImg;

    @Schema(description = "是否首页落点推荐", example = "true")
    private Boolean isLanding;
}
