package com.tepinhui.tepinhui_backend.controller.admin;

import com.tepinhui.tepinhui_backend.common.Result;
import com.tepinhui.tepinhui_backend.service.AdminStatsService;
import com.tepinhui.tepinhui_backend.vo.admin.AdminStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "平台看板-管理端接口", description = "管理员查看平台核心统计看板接口")
@RestController
@RequestMapping("/api/v1/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    @GetMapping
    @Operation(
        summary = "平台看板（未实现）",
        description = "（未实现：当前返回全 0 占位数据）管理员查看平台用户、商家、商品、订单和销售额概览"
    )
    public Result<AdminStatsVO> getAdminStats() {
        return Result.success(adminStatsService.getAdminStats());
    }
}
