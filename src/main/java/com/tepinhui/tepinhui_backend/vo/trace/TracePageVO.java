package com.tepinhui.tepinhui_backend.vo.trace;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
@Schema(name = "TracePageVO", description = "溯源记录分页响应")
public class TracePageVO {

    @Schema(description = "记录列表")
    private List<TraceListVO> records;

    @Schema(description = "总数")
    private Long total;

    @Schema(description = "当前页")
    private Long page;

    @Schema(description = "每页大小")
    private Long size;
}
