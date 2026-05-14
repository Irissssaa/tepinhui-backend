package com.tepinhui.tepinhui_backend.vo.map;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "SeasonRecommendVO", description = "时令特产推荐响应")
public class SeasonRecommendVO {

    @Schema(description = "推荐ID")
    private Long id;

    @Schema(description = "特产名称", example = "西湖龙井")
    private String name;

    @Schema(description = "时令标签", example = "春季推荐")
    private String seasonTag;

    @Schema(description = "封面图URL", example = "https://example.com/cover.jpg")
    private String coverImg;

    @Schema(description = "推荐理由", example = "当前为占位推荐理由")
    private String reason;
}
