package com.tepinhui.tepinhui_backend.vo.culture;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "CultureContentVO", description = "文化内容响应")
public class CultureContentVO {

    @Schema(description = "文化内容ID")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "正文内容")
    private String content;

    @Schema(description = "内容类型", example = "history")
    private String type;

    @Schema(description = "封面图URL")
    private String coverImg;

    @Schema(description = "排序值")
    private Integer sortOrder;

    @Schema(description = "状态", example = "on")
    private String status;
}
