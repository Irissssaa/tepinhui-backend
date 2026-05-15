package com.tepinhui.tepinhui_backend.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(name = "AdminLogAppliedFiltersVO", description = "管理员日志读取接口实际生效的过滤条件")
public class AdminLogAppliedFiltersVO {

    @Schema(description = "起始时间，未传则为空", example = "2026-05-15T09:30:00")
    private LocalDateTime startTime;

    @Schema(description = "结束时间，未传则为空", example = "2026-05-15T10:30:00")
    private LocalDateTime endTime;

    @Schema(description = "日志等级，未传则为空", example = "ERROR")
    private String level;

    @Schema(description = "消息体关键字，未传则为空", example = "JWT")
    private String keyword;

    @Schema(description = "实际生效的条数限制", example = "100")
    private Integer limit;
}
