package com.tepinhui.tepinhui_backend.service;

import com.tepinhui.tepinhui_backend.dto.culture.CultureContentUpdateRequest;

public interface CultureContentService {

    void updateSpecialtyCulture(Long specialtyId, CultureContentUpdateRequest request);
}
