package com.tepinhui.tepinhui_backend.service;

import com.tepinhui.tepinhui_backend.dto.user.UserProfileUpdateRequest;
import com.tepinhui.tepinhui_backend.vo.user.UserProfileVO;

public interface UserProfileService {

    UserProfileVO getCurrentUserProfile();

    UserProfileVO updateCurrentUserProfile(UserProfileUpdateRequest request);
}
