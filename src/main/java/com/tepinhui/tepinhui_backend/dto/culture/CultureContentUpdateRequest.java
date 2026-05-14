package com.tepinhui.tepinhui_backend.dto.culture;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(name = "CultureContentUpdateRequest", description = "管理端维护特产文化内容请求")
public class CultureContentUpdateRequest {

    @Size(max = 2000, message = "特产文化简介不能超过2000个字符")
    @Schema(description = "特产文化简介", example = "西湖龙井以色翠、香郁、味甘、形美著称。")
    private String culturalInfo;

    @Valid
    @NotEmpty(message = "文化内容列表不能为空")
    @Schema(description = "文化内容列表")
    private List<CultureContentItemRequest> cultureContents;

    @Data
    @Schema(name = "CultureContentItemRequest", description = "文化内容项请求")
    public static class CultureContentItemRequest {

        @Schema(description = "文化内容ID，更新已有内容时传入", example = "3001")
        private Long id;

        @NotNull(message = "标题不能为空")
        @Size(min = 1, max = 100, message = "标题长度必须在1到100个字符之间")
        @Schema(description = "标题", example = "龙井茶的历史渊源")
        private String title;

        @NotNull(message = "内容不能为空")
        @Size(min = 1, max = 5000, message = "内容长度必须在1到5000个字符之间")
        @Schema(description = "正文内容", example = "龙井茶始于宋，闻于元，扬于明，盛于清。")
        private String content;

        @NotNull(message = "内容类型不能为空")
        @Size(min = 1, max = 50, message = "内容类型长度必须在1到50个字符之间")
        @Schema(description = "内容类型", example = "history")
        private String type;

        @Size(max = 255, message = "封面图URL不能超过255个字符")
        @Schema(description = "封面图URL", example = "https://oss.example.com/culture/longjing-history.jpg")
        private String coverImg;

        @Schema(description = "排序值", example = "1")
        private Integer sortOrder;

        @NotNull(message = "状态不能为空")
        @Size(min = 1, max = 20, message = "状态长度必须在1到20个字符之间")
        @Schema(description = "状态", example = "on")
        private String status;
    }
}
