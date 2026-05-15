package com.tepinhui.tepinhui_backend.controller;

import com.tepinhui.tepinhui_backend.common.Result;
import com.tepinhui.tepinhui_backend.dto.user.UserProfileUpdateRequest;
import com.tepinhui.tepinhui_backend.service.UserProfileService;
import com.tepinhui.tepinhui_backend.vo.user.UserProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户资料-用户侧接口", description = "当前登录用户资料查询与更新接口")
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserProfileService userProfileService;

    @GetMapping("/profile")
    @Operation(
        summary = "获取当前用户资料",
        description = "查询当前登录用户的资料信息，包含id/username/nickname/email/phone/avatarUrl/role/status"
    )
    public Result<UserProfileVO> getProfile() {
        return Result.success(userProfileService.getCurrentUserProfile());
    }

    @PutMapping("/profile")
    @Operation(
        summary = "更新当前用户资料",
        description = "更新当前登录用户的昵称、头像、邮箱和手机号；非null字段才会更新"
    )
    public Result<UserProfileVO> updateProfile(
        @Parameter(description = "用户资料更新请求", required = true)
        @RequestBody @Valid UserProfileUpdateRequest request
    ) {
        return Result.success("用户资料更新成功", userProfileService.updateCurrentUserProfile(request));
    }
}
