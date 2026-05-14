package com.tepinhui.tepinhui_backend.service;

import com.tepinhui.tepinhui_backend.dto.specialty.SpecialtyCreateRequest;
import com.tepinhui.tepinhui_backend.dto.specialty.SpecialtyUpdateRequest;
import com.tepinhui.tepinhui_backend.vo.specialty.SpecialtyDetailVO;
import com.tepinhui.tepinhui_backend.vo.specialty.SpecialtyListVO;

import java.util.List;

public interface SpecialtyService {

    List<SpecialtyListVO> listSpecialties();

    SpecialtyDetailVO getSpecialtyDetail(Long id);

    void createSpecialty(SpecialtyCreateRequest request);

    void updateSpecialty(Long id, SpecialtyUpdateRequest request);

    void updateSpecialtyStatus(Long id, String status);
}
