package com.tepinhui.tepinhui_backend.controller.admin;

import com.tepinhui.tepinhui_backend.dto.trace.TraceAuditDTO;
import com.tepinhui.tepinhui_backend.entity.TraceRecord;
import com.tepinhui.tepinhui_backend.service.TraceService;
import com.tepinhui.tepinhui_backend.vo.trace.TracePageVO;
import com.tepinhui.tepinhui_backend.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trace")
@RequiredArgsConstructor
@Tag(name = "溯源-管理端接口", description = "管理员溯源审核与列表查询接口")
public class AdminTraceController {

    private final TraceService traceService;

    /**
     * 分页查询待审核列表
     * GET /tph/api/v1/trace/admin/pending
     */
    @GetMapping("/admin/pending")
    @Operation(
        summary = "待审核列表（部分未实现）",
        description = "分页查询待审核的溯源记录列表。（部分未实现：列表中的商品名、商家名仍为硬编码示例值）"
    )
    public Result<TracePageVO> getPendingList(
        @Parameter(description = "页码")
        @RequestParam(defaultValue = "1") Long page,
        @Parameter(description = "每页大小")
        @RequestParam(defaultValue = "10") Long size
    ) {
        TracePageVO vo = traceService.getPendingList(page, size);
        return Result.success(vo);
    }

    /**
     * 查看审核详情
     * GET /tph/api/v1/trace/admin/{id}
     */
    @GetMapping("/admin/{id}")
    @Operation(
        summary = "审核详情",
        description = "查看某条溯源记录的完整录入信息"
    )
    public Result<TraceRecord> getTraceDetail(
        @Parameter(description = "溯源记录ID", required = true)
        @PathVariable Long id
    ) {
        TraceRecord record = traceService.getTraceDetail(id);
        return Result.success(record);
    }

    /**
     * 审核操作（通过/驳回）
     * PUT /tph/api/v1/trace/admin/audit
     */
    @PutMapping("/admin/audit")
    @Operation(
        summary = "审核溯源（部分未实现）",
        description = "管理员审核溯源信息，审核通过时自动生成二维码URL。（部分未实现：当前二维码 URL 仍返回示例 OSS 地址）"
    )
    public Result<TraceRecord> auditTrace(
        @Parameter(description = "审核信息")
        @RequestBody @Valid TraceAuditDTO dto
    ) {
        TraceRecord record = traceService.auditTrace(dto);
        return Result.success(record);
    }

    /**
     * 分页查询所有溯源记录（管理员）
     * GET /tph/api/v1/trace/admin/list
     */
    @GetMapping("/admin/list")
    @Operation(
        summary = "所有溯源记录（部分未实现）",
        description = "管理员分页查询所有溯源记录（含各种审核状态）。（部分未实现：列表中的商品名、商家名仍为硬编码示例值）"
    )
    public Result<TracePageVO> getAllList(
        @Parameter(description = "页码")
        @RequestParam(defaultValue = "1") Long page,
        @Parameter(description = "每页大小")
        @RequestParam(defaultValue = "10") Long size
    ) {
        TracePageVO vo = traceService.getAllList(page, size);
        return Result.success(vo);
    }
}
