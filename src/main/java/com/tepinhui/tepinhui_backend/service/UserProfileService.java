package com.tepinhui.tepinhui_backend.service;

import com.tepinhui.tepinhui_backend.dto.user.PasswordChangeCodeRequest;
import com.tepinhui.tepinhui_backend.dto.user.PasswordChangeRequest;
import com.tepinhui.tepinhui_backend.dto.user.UserProfileUpdateRequest;
import com.tepinhui.tepinhui_backend.vo.user.UserProfileVO;
import org.springframework.web.multipart.MultipartFile;

public interface UserProfileService {

    UserProfileVO getCurrentUserProfile();

    UserProfileVO updateCurrentUserProfile(UserProfileUpdateRequest request);

    void sendPasswordChangeCode(PasswordChangeCodeRequest request);

    void changePassword(PasswordChangeRequest request);

    String uploadAvatar(MultipartFile file);
}
