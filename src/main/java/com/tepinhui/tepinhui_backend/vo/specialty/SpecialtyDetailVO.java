package com.tepinhui.tepinhui_backend.vo.specialty;

import com.tepinhui.tepinhui_backend.vo.culture.CultureContentVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(name = "SpecialtyDetailVO", description = "特产详情响应")
public class SpecialtyDetailVO {

    @Schema(description = "特产ID")
    private Long id;

    @Schema(description = "特产名称")
    private String name;

    @Schema(description = "分类名称")
    private String category;

    @Schema(description = "特产文化简介")
    private String culturalInfo;

    @Schema(description = "节气标签")
    private String seasonTag;

    @Schema(description = "封面图URL")
    private String coverImg;

    @Schema(description = "是否首页落点推荐", example = "true")
    private Boolean isLanding;

    @Schema(description = "产地信息")
    private OriginInfoVO origin;

    @Schema(description = "文化内容列表")
    private List<CultureContentVO> cultureContents;
}
