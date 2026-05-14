package com.tepinhui.tepinhui_backend.service.impl;

import com.tepinhui.tepinhui_backend.dto.user.UserProfileUpdateRequest;
import com.tepinhui.tepinhui_backend.exception.BusinessException;
import com.tepinhui.tepinhui_backend.service.UserProfileService;
import com.tepinhui.tepinhui_backend.vo.user.UserProfileVO;
import org.springframework.stereotype.Service;

@Service
public class UserProfileServiceImpl implements UserProfileService {

    @Override
    public UserProfileVO getCurrentUserProfile() {
        throw new BusinessException(501, "用户资料业务逻辑待实现");
    }

    @Override
    public UserProfileVO updateCurrentUserProfile(UserProfileUpdateRequest request) {
        throw new BusinessException(501, "用户资料更新业务逻辑待实现");
    }
}
