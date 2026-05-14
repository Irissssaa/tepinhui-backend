package com.tepinhui.tepinhui_backend.service.impl;

import com.tepinhui.tepinhui_backend.dto.culture.CultureContentUpdateRequest;
import com.tepinhui.tepinhui_backend.exception.BusinessException;
import com.tepinhui.tepinhui_backend.service.CultureContentService;
import org.springframework.stereotype.Service;

@Service
public class CultureContentServiceImpl implements CultureContentService {

    @Override
    public void updateSpecialtyCulture(Long specialtyId, CultureContentUpdateRequest request) {
        throw new BusinessException(501, "维护特产文化内容业务逻辑待实现");
    }
}
