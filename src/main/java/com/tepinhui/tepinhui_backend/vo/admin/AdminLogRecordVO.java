package com.tepinhui.tepinhui_backend.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(name = "AdminLogRecordVO", description = "管理员日志读取接口单条日志记录")
public class AdminLogRecordVO {

    @Schema(description = "日志时间", example = "2026-05-15T10:00:01.123")
    private LocalDateTime timestamp;

    @Schema(description = "日志等级", example = "ERROR")
    private String level;

    @Schema(description = "日志消息体", example = "JWT token expired")
    private String message;

    @Schema(description = "原始日志主行", example = "2026-05-15 10:00:01.123 | ERROR | [http-nio-8060-exec-1] | c.t.t.s.AuthService | JWT token expired")
    private String rawLine;

    @Schema(description = "异常堆栈跟踪（多行，换行符分隔；无异常时为 null）",
            example = "java.lang.NullPointerException: ...\n    at com.xxx.Service.method(Service.java:123)")
    private String stackTrace;
}
