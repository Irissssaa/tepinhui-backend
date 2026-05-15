package com.tepinhui.tepinhui_backend.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@Schema(name = "AdminLogQueryRequest", description = "管理员日志读取请求")
public class AdminLogQueryRequest {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "起始时间，ISO-8601 本地时间", example = "2026-05-15T09:30:00")
    private LocalDateTime startTime;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "结束时间，ISO-8601 本地时间", example = "2026-05-15T10:30:00")
    private LocalDateTime endTime;

    @Schema(description = "日志等级，仅允许 TRACE、DEBUG、INFO、WARN、ERROR", example = "ERROR")
    private String level;

    @Schema(description = "按消息体大小写敏感匹配关键字", example = "JWT")
    private String keyword;

    @Schema(description = "返回条数，未传时使用默认值", example = "100")
    private Integer limit;
}
