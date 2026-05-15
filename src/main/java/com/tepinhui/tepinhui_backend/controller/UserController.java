package com.tepinhui.tepinhui_backend.controller;

import com.tepinhui.tepinhui_backend.common.Result;
import com.tepinhui.tepinhui_backend.dto.user.PasswordChangeCodeRequest;
import com.tepinhui.tepinhui_backend.dto.user.PasswordChangeRequest;
import com.tepinhui.tepinhui_backend.dto.user.UserProfileUpdateRequest;
import com.tepinhui.tepinhui_backend.service.UserProfileService;
import com.tepinhui.tepinhui_backend.vo.user.UserProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping("/password/code")
    @Operation(
        summary = "发送修改密码验证码",
        description = "向当前用户绑定的邮箱发送修改密码验证码，邮箱必须属于当前登录用户"
    )
    public Result<Void> sendPasswordChangeCode(
        @Parameter(description = "发送修改密码验证码请求", required = true)
        @RequestBody @Valid PasswordChangeCodeRequest request
    ) {
        userProfileService.sendPasswordChangeCode(request);
        return Result.success("验证码发送成功", null);
    }

    @PutMapping("/password")
    @Operation(
        summary = "修改密码",
        description = "通过邮箱验证码修改密码，验证码通过后使用BCrypt加密新密码并更新"
    )
    public Result<Void> changePassword(
        @Parameter(description = "修改密码请求", required = true)
        @RequestBody @Valid PasswordChangeRequest request
    ) {
        userProfileService.changePassword(request);
        return Result.success("密码修改成功", null);
    }

    @PostMapping("/avatar")
    @Operation(
        summary = "上传头像",
        description = "上传用户头像图片，支持JPEG/PNG/GIF格式，大小不超过2MB；上传后自动更新用户avatarUrl"
    )
    public Result<String> uploadAvatar(
        @Parameter(description = "头像文件", required = true)
        @RequestParam("file") MultipartFile file
    ) {
        String avatarUrl = userProfileService.uploadAvatar(file);
        return Result.success("头像上传成功", avatarUrl);
    }
}
