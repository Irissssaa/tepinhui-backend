package com.tepinhui.tepinhui_backend.service.impl;

import com.tepinhui.tepinhui_backend.dto.specialty.SpecialtyCreateRequest;
import com.tepinhui.tepinhui_backend.dto.specialty.SpecialtyUpdateRequest;
import com.tepinhui.tepinhui_backend.exception.BusinessException;
import com.tepinhui.tepinhui_backend.service.SpecialtyService;
import com.tepinhui.tepinhui_backend.vo.specialty.SpecialtyDetailVO;
import com.tepinhui.tepinhui_backend.vo.specialty.SpecialtyListVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SpecialtyServiceImpl implements SpecialtyService {

    @Override
    public List<SpecialtyListVO> listSpecialties() {
        return List.of();
    }

    @Override
    public SpecialtyDetailVO getSpecialtyDetail(Long id) {
        throw new BusinessException(501, "查询特产详情业务逻辑待实现");
    }

    @Override
    public void createSpecialty(SpecialtyCreateRequest request) {
        throw new BusinessException(501, "新增特产业务逻辑待实现");
    }

    @Override
    public void updateSpecialty(Long id, SpecialtyUpdateRequest request) {
        throw new BusinessException(501, "更新特产业务逻辑待实现");
    }

    @Override
    public void updateSpecialtyStatus(Long id, String status) {
        throw new BusinessException(501, "更新特产状态业务逻辑待实现");
    }
}
