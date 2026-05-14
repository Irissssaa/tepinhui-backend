package com.tepinhui.tepinhui_backend.controller;

import com.tepinhui.tepinhui_backend.common.Result;
import com.tepinhui.tepinhui_backend.service.SpecialtyService;
import com.tepinhui.tepinhui_backend.vo.specialty.SpecialtyDetailVO;
import com.tepinhui.tepinhui_backend.vo.specialty.SpecialtyListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "特产-公开接口", description = "公开特产列表与详情查询接口")
@RestController
@RequestMapping("/api/v1/specialties")
@RequiredArgsConstructor
public class SpecialtyController {

    private final SpecialtyService specialtyService;

    @GetMapping
    @Operation(
        summary = "特产列表",
        description = "查询特产列表，支持分页与筛选（省份、品类、节气标签）"
    )
    public Result<List<SpecialtyListVO>> listSpecialties() {
        return Result.success(specialtyService.listSpecialties());
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "特产详情",
        description = "根据特产ID查询详情，包含产地信息和文化内容"
    )
    public Result<SpecialtyDetailVO> getSpecialtyDetail(
        @Parameter(description = "特产ID", required = true)
        @PathVariable Long id
    ) {
        return Result.success(specialtyService.getSpecialtyDetail(id));
    }
}
