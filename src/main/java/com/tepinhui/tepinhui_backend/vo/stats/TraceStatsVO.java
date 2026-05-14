package com.tepinhui.tepinhui_backend.vo.stats;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "TraceStatsVO", description = "溯源统计总览响应")
public class TraceStatsVO {

    @Schema(description = "溯源总数", example = "0")
    private Long totalTraceCount;

    @Schema(description = "审核通过溯源数", example = "0")
    private Long passedTraceCount;

    @Schema(description = "待审核溯源数", example = "0")
    private Long pendingTraceCount;

    @Schema(description = "审核拒绝溯源数", example = "0")
    private Long rejectedTraceCount;
}
