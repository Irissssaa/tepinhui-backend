package com.tepinhui.tepinhui_backend.vo.category;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "CategoryVO", description = "分类响应")
public class CategoryVO {

    @Schema(description = "分类ID")
    private Long id;

    @Schema(description = "父级分类ID")
    private Long parentId;

    @Schema(description = "分类名称")
    private String name;

    @Schema(description = "分类图标URL")
    private String icon;

    @Schema(description = "排序值")
    private Integer sortOrder;

    @Schema(description = "分类状态", example = "on")
    private String status;
}
