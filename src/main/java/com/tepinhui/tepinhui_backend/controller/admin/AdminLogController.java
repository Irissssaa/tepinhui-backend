package com.tepinhui.tepinhui_backend.controller.admin;

import com.tepinhui.tepinhui_backend.common.Result;
import com.tepinhui.tepinhui_backend.dto.admin.AdminLogQueryRequest;
import com.tepinhui.tepinhui_backend.service.AdminLogService;
import com.tepinhui.tepinhui_backend.vo.admin.AdminLogPageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "日志-管理端接口", description = "管理员读取应用日志接口")
@RestController
@RequestMapping("/api/v1/admin/logs")
@RequiredArgsConstructor
public class AdminLogController {

    private final AdminLogService adminLogService;

    @GetMapping
    @Operation(
        summary = "读取应用日志",
        description = "管理员按时间区间、日志等级、关键字和返回条数读取固定应用日志文件；"
            + "时间参数使用 ISO-8601 本地时间格式，例如 2026-05-15T09:30:00"
    )
    public Result<AdminLogPageVO> getLogs(
        @Valid
        @ParameterObject
        @Parameter(
            description = "日志查询参数：startTime、endTime 使用 ISO-8601 本地时间；"
                + "level 仅支持 TRACE/DEBUG/INFO/WARN/ERROR；keyword 为消息体大小写敏感匹配；"
                + "limit 未传时使用服务默认值",
            required = false
        )
        AdminLogQueryRequest request
    ) {
        return Result.success(adminLogService.getLogs(request));
    }
}
