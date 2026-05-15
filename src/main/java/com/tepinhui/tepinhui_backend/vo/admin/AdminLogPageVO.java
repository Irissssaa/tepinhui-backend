package com.tepinhui.tepinhui_backend.vo.admin;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(name = "AdminLogPageVO", description = "管理员日志读取响应")
public class AdminLogPageVO {

    @Schema(description = "本次实际生效的过滤条件")
    private AdminLogAppliedFiltersVO appliedFilters;

    @Schema(description = "本次读取的固定日志文件路径", example = "/home/tph/shared/logs/tepinhui-backend/application.log")
    private String logFilePath;

    @Schema(description = "本次返回的日志条数", example = "1")
    private Integer returnedCount;

    @ArraySchema(schema = @Schema(implementation = AdminLogRecordVO.class))
    private List<AdminLogRecordVO> records;
}
