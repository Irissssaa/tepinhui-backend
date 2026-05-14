package com.tepinhui.tepinhui_backend.controller;

import com.tepinhui.tepinhui_backend.dto.trace.TraceInputDTO;
import com.tepinhui.tepinhui_backend.entity.TraceRecord;
import com.tepinhui.tepinhui_backend.service.TraceService;
import com.tepinhui.tepinhui_backend.vo.trace.TracePageVO;
import com.tepinhui.tepinhui_backend.vo.trace.TraceQueryVO;
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
@Tag(name = "溯源-公开与商家侧接口", description = "溯源码录入与公开溯源查询接口")
public class TraceController {

    private final TraceService traceService;

    /**
     * 商家录入溯源信息
     * POST /tph/api/v1/trace
     */
    @PostMapping
    @Operation(
        summary = "录入溯源",
        description = "商家录入溯源信息，系统自动生成溯源码，返回溯源码与二维码URL（审核通过后）"
    )
    public Result<TraceRecord> inputTrace(
        @Parameter(description = "溯源录入信息")
        @RequestBody @Valid TraceInputDTO dto
        // TODO: 从 Security Context 获取 merchantId
        // @RequestAttribute Long merchantId
    ) {
        Long merchantId = 1L; // TODO: 替换为实际 merchantId
        TraceRecord record = traceService.inputTrace(dto, merchantId);
        return Result.success(record);
    }

    /**
     * 消费者查询溯源全链路（扫码/手动输码）
     * GET /tph/api/v1/trace/{traceCode}
     */
    @GetMapping("/{traceCode}")
    @Operation(
        summary = "查询溯源全链路",
        description = "根据溯源码查询完整溯源链路信息（产品→产地→生产→加工→质检→流通），审核状态必须为pass"
    )
    public Result<TraceQueryVO> getTraceInfo(
        @Parameter(description = "溯源码，格式：TP-XX-XX-XXXX-XXXXXX", required = true)
        @PathVariable String traceCode
    ) {
        TraceQueryVO vo = traceService.getTraceInfo(traceCode);
        return Result.success(vo);
    }

    /**
     * 获取某商品全部溯源批次列表
     * GET /tph/api/v1/trace/product/{productId}
     */
    @GetMapping("/product/{productId}")
    @Operation(
        summary = "商品溯源批次列表",
        description = "获取某商品全部溯源批次列表（仅展示已审核通过的）"
    )
    public Result<TracePageVO> getTraceListByProduct(
        @Parameter(description = "商品ID", required = true)
        @PathVariable Long productId,
        @Parameter(description = "页码")
        @RequestParam(defaultValue = "1") Long page,
        @Parameter(description = "每页大小")
        @RequestParam(defaultValue = "10") Long size
    ) {
        TracePageVO vo = traceService.getTraceListByProduct(productId, page, size);
        return Result.success(vo);
    }
}
