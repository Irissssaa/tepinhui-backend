package com.tepinhui.tepinhui_backend.dto.specialty;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "SpecialtyUpdateRequest", description = "管理端更新特产请求")
public class SpecialtyUpdateRequest {

    @Size(max = 100, message = "特产名称不能超过100个字符")
    @Schema(description = "特产名称", example = "西湖龙井")
    private String name;

    @Size(max = 100, message = "分类名称不能超过100个字符")
    @Schema(description = "分类名称", example = "名茶")
    private String category;

    @Size(max = 100, message = "节气标签不能超过100个字符")
    @Schema(description = "节气标签", example = "春季头采")
    private String seasonTag;

    @Size(max = 255, message = "封面图URL不能超过255个字符")
    @Schema(description = "封面图URL", example = "https://oss.example.com/specialty/xihu-longjing-cover.jpg")
    private String coverImg;

    @Schema(description = "是否作为首页落点推荐", example = "false")
    private Boolean isLanding;

    @Schema(description = "产地ID", example = "1001")
    private Long originId;

    @Size(max = 2000, message = "特产文化简介不能超过2000个字符")
    @Schema(description = "特产文化简介", example = "西湖龙井以色翠、香郁、味甘、形美著称。")
    private String culturalInfo;
}
