package com.tepinhui.tepinhui_backend.controller.admin;

import com.tepinhui.tepinhui_backend.common.Result;
import com.tepinhui.tepinhui_backend.dto.culture.CultureContentUpdateRequest;
import com.tepinhui.tepinhui_backend.dto.specialty.SpecialtyCreateRequest;
import com.tepinhui.tepinhui_backend.dto.specialty.SpecialtyUpdateRequest;
import com.tepinhui.tepinhui_backend.service.CultureContentService;
import com.tepinhui.tepinhui_backend.service.SpecialtyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "特产-管理端接口", description = "管理员维护特产基础信息、状态和文化内容接口")
@Validated
@RestController
@RequestMapping("/api/v1/admin/specialties")
@RequiredArgsConstructor
public class AdminSpecialtyController {

    private final SpecialtyService specialtyService;
    private final CultureContentService cultureContentService;

    @PostMapping
    @Operation(
        summary = "新增特产（未实现）",
        description = "管理员新增特产基础信息；当前接口仅保留契约，业务逻辑待实现"
    )
    public Result<Void> createSpecialty(
        @Parameter(description = "新增特产请求", required = true)
        @RequestBody @Valid SpecialtyCreateRequest request
    ) {
        specialtyService.createSpecialty(request);
        return Result.success("特产新增成功", null);
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "更新特产（未实现）",
        description = "管理员更新特产基础信息；当前接口仅保留契约，业务逻辑待实现"
    )
    public Result<Void> updateSpecialty(
        @Parameter(description = "特产ID", required = true)
        @PathVariable Long id,
        @Parameter(description = "更新特产请求", required = true)
        @RequestBody @Valid SpecialtyUpdateRequest request
    ) {
        specialtyService.updateSpecialty(id, request);
        return Result.success("特产更新成功", null);
    }

    @PutMapping("/{id}/status")
    @Operation(
        summary = "更新特产状态（未实现）",
        description = "管理员更新特产上下架状态；当前接口仅保留契约，业务逻辑待实现"
    )
    public Result<Void> updateSpecialtyStatus(
        @Parameter(description = "特产ID", required = true)
        @PathVariable Long id,
        @Parameter(description = "特产状态，例如 on/off/review", required = true)
        @RequestParam @NotBlank(message = "特产状态不能为空") String status
    ) {
        specialtyService.updateSpecialtyStatus(id, status);
        return Result.success("特产状态更新成功", null);
    }

    @PutMapping("/{id}/culture")
    @Operation(
        summary = "维护特产文化内容（未实现）",
        description = "管理员维护特产文化简介和文化内容列表；当前接口仅保留契约，业务逻辑待实现"
    )
    public Result<Void> updateSpecialtyCulture(
        @Parameter(description = "特产ID", required = true)
        @PathVariable Long id,
        @Parameter(description = "特产文化内容维护请求", required = true)
        @RequestBody @Valid CultureContentUpdateRequest request
    ) {
        cultureContentService.updateSpecialtyCulture(id, request);
        return Result.success("特产文化内容更新成功", null);
    }
}
