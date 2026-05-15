package com.tepinhui.tepinhui_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tepinhui.tepinhui_backend.exception.BusinessException;
import com.tepinhui.tepinhui_backend.dto.trace.TraceInputDTO;
import com.tepinhui.tepinhui_backend.entity.Merchant;
import com.tepinhui.tepinhui_backend.entity.TraceRecord;
import com.tepinhui.tepinhui_backend.entity.User;
import com.tepinhui.tepinhui_backend.mapper.MerchantMapper;
import com.tepinhui.tepinhui_backend.mapper.UserMapper;
import com.tepinhui.tepinhui_backend.service.TraceService;
import com.tepinhui.tepinhui_backend.vo.trace.TracePageVO;
import com.tepinhui.tepinhui_backend.vo.trace.TraceQueryVO;
import com.tepinhui.tepinhui_backend.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trace")
@RequiredArgsConstructor
@Tag(name = "溯源-公开与商家侧接口", description = "溯源码录入与公开溯源查询接口")
public class TraceController {

    private final TraceService traceService;
    private final MerchantMapper merchantMapper;
    private final UserMapper userMapper;

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
    ) {
        Long merchantId = getCurrentMerchantId();
        TraceRecord record = traceService.inputTrace(dto, merchantId);
        return Result.success(record);
    }

    /**
     * 从 SecurityContext 获取当前商家ID
     */
    private Long getCurrentMerchantId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(401, "未登录");
        }
        String username;
        Object principal = authentication.getPrincipal();
        if (principal instanceof String) {
            username = (String) principal;
        } else if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            throw new BusinessException(401, "登录状态无效");
        }
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(401, "登录状态无效");
        }
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .last("LIMIT 1")
        );
        if (user == null) {
            throw new BusinessException(404, "当前用户不存在");
        }
        Merchant merchant = merchantMapper.selectOne(
            new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getUserId, user.getId())
                .eq(Merchant::getStatus, "approved")
                .last("LIMIT 1")
        );
        if (merchant == null) {
            throw new BusinessException(403, "您还没有通过商家入驻审核");
        }
        return merchant.getId();
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
